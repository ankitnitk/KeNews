import anthropic
import os
from feeds import CATEGORIES

_client: anthropic.AsyncAnthropic | None = None


def get_client() -> anthropic.AsyncAnthropic:
    global _client
    if _client is None:
        _client = anthropic.AsyncAnthropic(api_key=os.environ["ANTHROPIC_API_KEY"])
    return _client


SYSTEM_PROMPT = f"""You are a news editor for KeNews, a Kenya-focused news app.
Given a news article, you will:
1. Detect if it's in Swahili or English (translate mentally if Swahili)
2. Write a crisp 60-word English summary in active voice, present tense where possible
3. Assign exactly one category from: {", ".join(CATEGORIES)}

Respond ONLY with valid JSON in this exact format:
{{
  "summary": "60-word summary here",
  "category": "CategoryName"
}}"""


async def summarize(title: str, body: str, source: str) -> dict | None:
    """Returns dict with 'summary' and 'category', or None on failure."""
    content = f"Source: {source}\nTitle: {title}\n\nArticle:\n{body[:3000]}"
    try:
        message = await get_client().messages.create(
            model="claude-haiku-4-5-20251001",
            max_tokens=300,
            system=SYSTEM_PROMPT,
            messages=[{"role": "user", "content": content}],
        )
        import json
        text = message.content[0].text.strip()
        # Strip markdown code fences if present
        if text.startswith("```"):
            text = text.split("```")[1]
            if text.startswith("json"):
                text = text[4:]
        return json.loads(text.strip())
    except Exception as e:
        print(f"[summarizer] error: {e}")
        return None
