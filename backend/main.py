import os
from contextlib import asynccontextmanager
from dotenv import load_dotenv

load_dotenv()

from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from apscheduler.schedulers.asyncio import AsyncIOScheduler

from db import init_db, get_articles
from poller import poll_all
from feeds import CATEGORIES

scheduler = AsyncIOScheduler()


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    # Initial poll on startup
    await poll_all()
    # Schedule recurring polls
    interval = int(os.environ.get("POLL_INTERVAL_MINUTES", 30))
    scheduler.add_job(poll_all, "interval", minutes=interval)
    scheduler.start()
    yield
    scheduler.shutdown()


app = FastAPI(title="KeNews API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET"],
    allow_headers=["*"],
)


@app.get("/articles")
async def list_articles(
    category: str | None = Query(default=None),
    limit: int = Query(default=30, le=100),
    offset: int = Query(default=0),
):
    articles = await get_articles(category=category, limit=limit, offset=offset)
    return {"articles": articles, "count": len(articles)}


@app.get("/categories")
async def list_categories():
    return {"categories": ["All"] + CATEGORIES}


@app.get("/health")
async def health():
    return {"status": "ok"}
