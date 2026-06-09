import os
import asyncpg

_pool = None


async def get_pool():
    global _pool
    if _pool is None:
        _pool = await asyncpg.create_pool(os.environ["DATABASE_URL"], ssl="require")
    return _pool


async def init_db():
    pool = await get_pool()
    async with pool.acquire() as conn:
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS articles (
                id SERIAL PRIMARY KEY,
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


async def insert_article(article: dict) -> bool:
    pool = await get_pool()
    async with pool.acquire() as conn:
        try:
            await conn.execute(
                """
                INSERT INTO articles
                    (url, title, summary, category, source, image_url, published_at, fetched_at)
                VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
                ON CONFLICT (url) DO NOTHING
                """,
                article["url"], article["title"], article["summary"],
                article["category"], article["source"], article.get("image_url"),
                article["published_at"], article["fetched_at"],
            )
            return True
        except Exception:
            return False


async def get_articles(
    category: str | None = None,
    limit: int = 30,
    offset: int = 0,
) -> list[dict]:
    pool = await get_pool()
    async with pool.acquire() as conn:
        if category and category.lower() != "all":
            rows = await conn.fetch(
                "SELECT * FROM articles WHERE category=$1 ORDER BY published_at DESC LIMIT $2 OFFSET $3",
                category, limit, offset,
            )
        else:
            rows = await conn.fetch(
                "SELECT * FROM articles ORDER BY published_at DESC LIMIT $1 OFFSET $2",
                limit, offset,
            )
        return [dict(r) for r in rows]


async def article_exists(url: str) -> bool:
    pool = await get_pool()
    async with pool.acquire() as conn:
        row = await conn.fetchrow("SELECT 1 FROM articles WHERE url=$1", url)
        return row is not None
