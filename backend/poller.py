import asyncio
import feedparser
import httpx
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime

from feeds import KENYA_FEEDS
from db import insert_article, article_exists
from summarizer import summarize


def _extract_image(entry) -> str | None:
    # Try media:content
    if hasattr(entry, "media_content") and entry.media_content:
        return entry.media_content[0].get("url")
    # Try enclosures
    if hasattr(entry, "enclosures") and entry.enclosures:
        for enc in entry.enclosures:
            if enc.get("type", "").startswith("image"):
                return enc.get("href") or enc.get("url")
    # Try og:image via summary html
    if "<img" in getattr(entry, "summary", ""):
        import re
        m = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', entry.summary)
        if m:
            return m.group(1)
    return None


def _parse_date(entry) -> str:
    try:
        if hasattr(entry, "published"):
            dt = parsedate_to_datetime(entry.published)
            return dt.astimezone(timezone.utc).isoformat()
    except Exception:
        pass
    return datetime.now(timezone.utc).isoformat()


async def _fetch_article_text(url: str) -> str:
    """Best-effort fetch of article body text."""
    try:
        async with httpx.AsyncClient(timeout=10, follow_redirects=True) as client:
            r = await client.get(url, headers={"User-Agent": "KeNews/1.0"})
            if r.status_code == 200:
                from html.parser import HTMLParser

                class TextExtractor(HTMLParser):
                    def __init__(self):
                        super().__init__()
                        self.chunks = []
                        self._skip = False

                    def handle_starttag(self, tag, attrs):
                        if tag in ("script", "style", "nav", "header", "footer"):
                            self._skip = True

                    def handle_endtag(self, tag):
                        if tag in ("script", "style", "nav", "header", "footer"):
                            self._skip = False

                    def handle_data(self, data):
                        if not self._skip and data.strip():
                            self.chunks.append(data.strip())

                p = TextExtractor()
                p.feed(r.text)
                return " ".join(p.chunks)
    except Exception:
        pass
    return ""


async def poll_feed(feed: dict):
    print(f"[poller] polling {feed['name']}")
    parsed = feedparser.parse(feed["url"])
    tasks = []
    for entry in parsed.entries[:int(os.environ.get("MAX_ARTICLES_PER_FEED", 10))]:
        url = entry.get("link", "")
        if not url or await article_exists(url):
            continue
        tasks.append(_process_entry(entry, feed))
    if tasks:
        await asyncio.gather(*tasks)


async def _process_entry(entry, feed: dict):
    url = entry.get("link", "")
    title = entry.get("title", "").strip()
    summary_hint = entry.get("summary", "")

    body = await _fetch_article_text(url)
    text = body or summary_hint or title

    result = await summarize(title, text, feed["name"])
    if not result:
        return

    image_url = _extract_image(entry)
    now = datetime.now(timezone.utc).isoformat()

    await insert_article({
        "url": url,
        "title": title,
        "summary": result["summary"],
        "category": result["category"],
        "source": feed["name"],
        "image_url": image_url,
        "published_at": _parse_date(entry),
        "fetched_at": now,
    })
    print(f"[poller] saved: {title[:60]}")


async def poll_all():
    import os
    tasks = [poll_feed(f) for f in KENYA_FEEDS]
    await asyncio.gather(*tasks)


import os
