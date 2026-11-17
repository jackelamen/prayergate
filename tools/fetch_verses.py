#!/usr/bin/env python3
"""
Downloads the Bible verses the prayers quote and writes tools/verses.json.

Run from the project root (needs internet):   python3 tools/fetch_verses.py

English: World English Bible (public domain), from
         github.com/TehShrike/world-english-bible
Korean:  개역한글 (Korean Revised Version, 1961), the "KorRV" module from
         github.com/scrollmapper/bible_databases

tools/make_prayers.py reads tools/verses.json, so the app build itself never
needs the internet. Run this again only after adding or changing a reference
in make_prayers.py. The words of Scripture are copied exactly; only the
spacing is tidied.
"""
import json
import os
import re
import sys
import urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from make_prayers import PRAYERS, parse_reference  # noqa: E402

WEB_URL = "https://raw.githubusercontent.com/TehShrike/world-english-bible/master/json/{}.json"
KOR_URL = "https://raw.githubusercontent.com/scrollmapper/bible_databases/master/formats/json/KorRV.json"

# Our book name -> (WEB file name, book name in KorRV.json)
BOOKS = {
    "Genesis": ("genesis", "Genesis"), "1 Samuel": ("1samuel", "I Samuel"),
    "Psalm": ("psalms", "Psalms"), "Proverbs": ("proverbs", "Proverbs"),
    "Jeremiah": ("jeremiah", "Jeremiah"),
    "Matthew": ("matthew", "Matthew"), "Mark": ("mark", "Mark"), "Luke": ("luke", "Luke"),
    "John": ("john", "John"), "Romans": ("romans", "Romans"),
    "1 Corinthians": ("1corinthians", "I Corinthians"), "2 Corinthians": ("2corinthians", "II Corinthians"),
    "Galatians": ("galatians", "Galatians"), "Ephesians": ("ephesians", "Ephesians"),
    "Philippians": ("philippians", "Philippians"), "Colossians": ("colossians", "Colossians"),
    "1 Thessalonians": ("1thessalonians", "I Thessalonians"),
    "1 Timothy": ("1timothy", "I Timothy"), "2 Timothy": ("2timothy", "II Timothy"),
    "Hebrews": ("hebrews", "Hebrews"), "James": ("james", "James"),
    "1 Peter": ("1peter", "I Peter"), "1 John": ("1john", "I John"),
}


def get(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.loads(r.read().decode("utf-8-sig"))


def tidy(text):
    return re.sub(r"\s+", " ", text).strip()


def main():
    refs = sorted({ref for items in PRAYERS.values() for (_, _, ref) in items})
    korean = {b["name"]: b for b in get(KOR_URL)["books"]}
    web_cache = {}
    out = {}
    for ref in refs:
        book, chapter, first, last = parse_reference(ref)
        web_file, kor_name = BOOKS[book]
        if web_file not in web_cache:
            web_cache[web_file] = get(WEB_URL.format(web_file))
        en = tidy(" ".join(
            item["value"] for item in web_cache[web_file]
            if item.get("chapterNumber") == chapter and first <= item.get("verseNumber", 0) <= last and "value" in item
        ))
        verses = korean[kor_name]["chapters"][chapter - 1]["verses"]
        ko = tidy(" ".join(v["text"] for v in verses if first <= v["verse"] <= last))
        if not en or not ko:
            sys.exit(f"Could not find {ref}")
        out[ref] = {"en": en, "ko": ko}
    path = os.path.join(os.path.dirname(__file__), "verses.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1, sort_keys=True)
        f.write("\n")
    print(f"Wrote {len(out)} passages to tools/verses.json")


if __name__ == "__main__":
    main()
