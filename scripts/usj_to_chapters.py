#!/usr/bin/env python3
"""
Convert the BSB's USJ download (one <BOOK>.usj file per book) into chapters.json,
the input to build_bible_db.py.

Usage:
    python3 scripts/usj_to_chapters.py path/to/bsb_usj [out.json]

Output matches the format the reader expects: per chapter, a list of verses whose
text ends in " " when the next verse continues the same paragraph, or "\n\n" when a
paragraph ends. A paragraph can also end mid-verse, so "\n\n" may appear inside a
verse's text too.

How USJ maps onto that:
  * Body paragraphs (prose `p`, poetry lines `q1`/`q2`, list items `li1`, ...) each
    become one paragraph, so every poetry line is its own paragraph.
  * Headings, cross-reference lines, book titles and blank-line markers are dropped,
    as are footnotes. The reader doesn't show these. So are the acrostic letters in
    Psalm 119 (`qa`, e.g. "ALEPH").
  * Psalm titles (`d` in Psalms, e.g. "A Psalm of David.") go in the chapter's
    "title" field, which the reader shows above verse 1. In USJ they sit after the
    verse 1 marker, so the marker still starts verse 1.
  * Two markers are used for ordinary verse text once each, and are kept as body
    text: `d` in Zechariah 12:1 and `mr` in Habakkuk 3:19.
  * Words of Jesus (`wj`) are kept as plain text.
"""

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

# Canonical book order. Also the full set of books we expect to find.
BOOKS = (
    "GEN EXO LEV NUM DEU JOS JDG RUT 1SA 2SA 1KI 2KI 1CH 2CH EZR NEH EST JOB PSA PRO "
    "ECC SNG ISA JER LAM EZK DAN HOS JOL AMO OBA JON MIC NAM HAB ZEP HAG ZEC MAL "
    "MAT MRK LUK JHN ACT ROM 1CO 2CO GAL EPH PHP COL 1TH 2TH 1TI 2TI TIT PHM HEB "
    "JAS 1PE 2PE 1JN 2JN 3JN JUD REV"
).split()

# Paragraph markers whose text is Bible text, shown as its own paragraph.
BODY_MARKERS = {"p", "m", "pmo", "pc", "pi1", "pi2", "q1", "q2", "q3", "qr", "qc", "li1", "li2", "mr"}
# Paragraph markers whose verse markers count but whose words are dropped.
MARKER_ONLY = {"qa"}
# Paragraph markers dropped entirely (headings, cross references, titles, spacing).
SKIPPED = {"s1", "s2", "s3", "r", "ms", "b", "h", "toc1", "toc2", "toc3", "mt1", "mt2"}

BREAK = "\n\n"


class Converter:
    def __init__(self, code: str):
        self.code = code
        self.chapters: dict[int, dict[int, list[str]]] = {}
        self.titles: dict[int, list[str]] = {}
        self.chapter: int | None = None
        self.verse: int | None = None

    def text(self, s: str) -> None:
        if self.chapter is not None and self.verse is not None:
            self.chapters[self.chapter][self.verse].append(s)
        elif s.strip():
            sys.exit(f"{self.code} {self.chapter}: text before the first verse: {s!r}")

    def start_verse(self, number: str) -> None:
        if not number.isdigit():
            sys.exit(f"{self.code} {self.chapter}: unsupported verse number {number!r}")
        if self.chapter is None:
            sys.exit(f"{self.code}: verse {number} before the first chapter")
        self.verse = int(number)
        verses = self.chapters[self.chapter]
        if self.verse in verses:
            sys.exit(f"{self.code} {self.chapter}:{self.verse} appears twice")
        verses[self.verse] = []

    def inline(self, node, keep_text: bool, sink: list[str] | None = None) -> None:
        """Walk a paragraph's content: strings, verse markers and character styles.
        Text goes to the current verse, or to [sink] if one is given."""
        if isinstance(node, str):
            if sink is not None:
                sink.append(node)
            elif keep_text:
                self.text(node)
            return
        kind = node.get("type")
        if kind == "verse":
            self.start_verse(node["number"])
        elif kind == "note":
            return  # footnote
        elif kind in ("char", "ref"):
            for child in node.get("content", []):
                self.inline(child, keep_text, sink)
        else:
            sys.exit(f"{self.code} {self.chapter}: unexpected inline node {node!r:.200}")

    def top_level(self, node) -> None:
        kind, marker = node.get("type"), node.get("marker")
        if kind == "book":
            return
        if kind == "chapter":
            self.chapter = int(node["number"])
            self.verse = None
            self.chapters[self.chapter] = {}
            return
        if kind != "para":
            sys.exit(f"{self.code}: unexpected top-level node {node!r:.200}")
        if marker in SKIPPED:
            return
        if marker == "d":
            self.descriptive_title(node)
            return
        if marker in MARKER_ONLY:
            for child in node.get("content", []):
                self.inline(child, keep_text=False)
            return
        if marker not in BODY_MARKERS:
            sys.exit(f"{self.code} {self.chapter}: unknown paragraph marker {marker!r}")
        for child in node.get("content", []):
            self.inline(child, keep_text=True)
        if self.verse is not None:
            self.text(BREAK)  # this paragraph ends here

    def descriptive_title(self, node) -> None:
        """`d`: a Psalm title, a book-division label, or (once) ordinary verse text."""
        content = node.get("content", [])
        has_verse = any(isinstance(c, dict) and c.get("type") == "verse" for c in content)
        if not has_verse:
            return  # "Psalms 1–41": goes with the "BOOK I" divider, not Bible text
        if self.code != "PSA":
            self.top_level({**node, "marker": "p"})  # Zechariah 12:1
            return
        if self.chapter in self.titles:
            sys.exit(f"{self.code} {self.chapter}: two Psalm titles")
        sink: list[str] = []
        for child in content:
            self.inline(child, keep_text=False, sink=sink)
        self.titles[self.chapter] = sink


# The USJ export ("Autogenerated BSB by bsb2usfm") has typographic quirks that aren't
# part of the translation. Each rule notes an example it fixes.
CLEANUPS = [
    (re.compile(r"\[’’\]"), ""),                    # EXO 20:26 'on it.’[’’]': editorial marker
    (re.compile(r"\[([^\]]*)\]"), r"\1"),           # EXO 38:28 '[of silver]': show supplied words plainly
    (re.compile(r" \. \. \."), ""),                 # NUM 26:18 'numbered . . . 40,500': table filler
    (re.compile(r"\s*—\s*"), "—"),                  # GEN 1:5 'morning — the': BSB closes em-dashes
    (re.compile(r"\s+([,;:?!)])"), r"\1"),          # 1SA 8:19 'Nevertheless ,'
    (re.compile(r"([(“‘])\s+"), r"\1"),             # space after an opening bracket or quote
    (re.compile(r"([a-z][.!?])([A-Z])"), r"\1 \2"), # JDG 16:14 'web.Then'
]


def clean_segment(seg: str) -> str:
    for pattern, replacement in CLEANUPS:
        seg = pattern.sub(replacement, seg)
    return re.sub(r"\s+", " ", seg).strip()


def clean_verse(pieces: list[str]) -> str:
    """Join a verse's pieces into the reader's format (see module docstring)."""
    raw = "".join(pieces)
    segments = [clean_segment(seg) for seg in raw.split(BREAK)]
    ends_paragraph = segments[-1] == ""
    body = BREAK.join(seg for seg in segments if seg)
    if not body:
        return ""
    return body + (BREAK if ends_paragraph else " ")


def convert_book(path: Path) -> tuple[str, dict[int, dict[int, str]], dict[int, str]]:
    usj = json.loads(path.read_text(encoding="utf-8"))
    content = usj["content"]
    code = content[0]["code"]
    name = next(
        "".join(c for c in n["content"] if isinstance(c, str))
        for n in content
        if isinstance(n, dict) and n.get("marker") == "h"
    )
    conv = Converter(code)
    for node in content:
        conv.top_level(node)
    chapters = {
        ch: {v: clean_verse(pieces) for v, pieces in verses.items()}
        for ch, verses in conv.chapters.items()
    }
    for ch, verses in chapters.items():
        # Some verses are intentionally absent: modern translations move a few
        # (e.g. Matthew 17:21) to footnotes. So require ascending order, not 1..n.
        numbers = list(verses)
        if numbers[0] != 1 or numbers != sorted(numbers):
            sys.exit(f"{code} {ch}: verses out of order: {numbers}")
        empty = [v for v, text in verses.items() if not text]
        if empty:
            sys.exit(f"{code} {ch}: empty verses {empty}")
    titles = {ch: clean_segment("".join(pieces)) for ch, pieces in conv.titles.items()}
    return name, chapters, titles


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    in_dir = Path(sys.argv[1])
    out_path = Path(sys.argv[2]) if len(sys.argv) > 2 else REPO_ROOT / "chapters.json"

    expected: set[str] = set(BOOKS)
    found: set[str] = {p.stem for p in in_dir.glob("*.usj")}
    if found != expected:
        sys.exit(f"Expected the 66 books. Missing: {sorted(expected - found)}, "
                 f"unexpected: {sorted(found - expected)}")

    out = []
    for code in BOOKS:
        name, chapters, titles = convert_book(in_dir / f"{code}.usj")
        for ch, verses in chapters.items():
            out.append({
                "translationId": "BSB",
                "bookName": name,
                "chapterId": f"{code}.{ch}",
                "reference": f"{name} {ch}",
                **({"title": titles[ch]} if ch in titles else {}),
                "verses": [
                    {"verseId": f"{code}.{ch}.{v}", "text": text} for v, text in verses.items()
                ],
            })

    out_path.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    total = sum(len(c["verses"]) for c in out)
    print(f"Wrote {len(out)} chapters, {total} verses, {sum('title' in c for c in out)} titles -> {out_path}")


if __name__ == "__main__":
    main()
