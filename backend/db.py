import aiosqlite
import json
from pathlib import Path

DB_PATH = Path(__file__).parent / "kenews.db"


async def init_db():
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("""
            CREATE TABLE IF NOT EXISTS articles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT UNIQUE NOT NULL,
                title TEXT NOT NULL,
                summary TEXT NOT NULL,
                category TEXT NOT NULL,
                source TEXT NOT NULL,
                image_url TEXT,
                published_at TEXT NOT NULL,
                fetched_at TEXT NOT NULL
            )
        """)
        await db.commit()


async def insert_article(article: dict) -> bool:
    """Returns True if inserted, False if already exists."""
    async with aiosqlite.connect(DB_PATH) as db:
        try:
            await db.execute(
                """
                INSERT INTO articles
                    (url, title, summary, category, source, image_url, published_at, fetched_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    article["url"],
                    article["title"],
                    article["summary"],
                    article["category"],
                    article["source"],
                    article.get("image_url"),
                    article["published_at"],
                    article["fetched_at"],
                ),
            )
            await db.commit()
            return True
        except aiosqlite.IntegrityError:
            return False


async def get_articles(
    category: str | None = None,
    limit: int = 30,
    offset: int = 0,
) -> list[dict]:
    async with aiosqlite.connect(DB_PATH) as db:
        db.row_factory = aiosqlite.Row
        if category and category.lower() != "all":
            cursor = await db.execute(
                """
                SELECT * FROM articles
                WHERE category = ?
                ORDER BY published_at DESC
                LIMIT ? OFFSET ?
                """,
                (category, limit, offset),
            )
        else:
            cursor = await db.execute(
                """
                SELECT * FROM articles
                ORDER BY published_at DESC
                LIMIT ? OFFSET ?
                """,
                (limit, offset),
            )
        rows = await cursor.fetchall()
        return [dict(row) for row in rows]


async def article_exists(url: str) -> bool:
    async with aiosqlite.connect(DB_PATH) as db:
        cursor = await db.execute("SELECT 1 FROM articles WHERE url = ?", (url,))
        return await cursor.fetchone() is not None
