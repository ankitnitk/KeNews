import os
import json
import httpx
from feeds import CATEGORIES

PROMPT_TEMPLATE = """You are a news editor for KeNews, a Kenya-focused news app.
Given the article below, do two things:
1. Write a crisp 60-word English summary in active voice (translate from Swahili if needed)
2. Assign exactly one category from: {categories}

Respond ONLY with valid JSON, no markdown, no explanation:
{{"summary": "...", "category": "..."}}

Source: {source}
Title: {title}

Article:
{body}"""


async def summarize(title: str, body: str, source: str) -> dict | None:
    api_key = os.environ["GEMINI_API_KEY"]
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key={api_key}"
    prompt = PROMPT_TEMPLATE.format(
        categories=", ".join(CATEGORIES),
        source=source,
        title=title,
        body=body[:3000],
    )
    payload = {"contents": [{"parts": [{"text": prompt}]}]}
    try:
        async with httpx.AsyncClient(timeout=30) as client:
            r = await client.post(url, json=payload)
            r.raise_for_status()
            text = r.json()["candidates"][0]["content"]["parts"][0]["text"].strip()
            if text.startswith("```"):
                text = text.split("```")[1]
                if text.startswith("json"):
                    text = text[4:]
            return json.loads(text.strip())
    except Exception as e:
        print(f"[summarizer] error: {e}")
        return None
