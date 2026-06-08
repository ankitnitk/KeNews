import os
import json
from google import genai
from feeds import CATEGORIES

_client = None


def get_client():
    global _client
    if _client is None:
        _client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
    return _client


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
    prompt = PROMPT_TEMPLATE.format(
        categories=", ".join(CATEGORIES),
        source=source,
        title=title,
        body=body[:3000],
    )
    try:
        response = get_client().models.generate_content(
            model="gemini-1.5-flash",
            contents=prompt,
        )
        text = response.text.strip()
        if text.startswith("```"):
            text = text.split("```")[1]
            if text.startswith("json"):
                text = text[4:]
        return json.loads(text.strip())
    except Exception as e:
        print(f"[summarizer] error: {e}")
        return None
