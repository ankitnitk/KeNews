# KeNews Backend

FastAPI service that polls Kenya news RSS feeds, summarizes with Claude, and serves a REST API.

## Setup

```bash
cd backend
python -m venv venv
venv\Scripts\activate        # Windows
pip install -r requirements.txt

copy .env.example .env
# Edit .env and add your ANTHROPIC_API_KEY

python -m uvicorn main:app --reload --port 8000
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /articles | List articles (query: category, limit, offset) |
| GET | /categories | List available categories |
| GET | /health | Health check |

## Deploy (free tier)

### Railway
```bash
railway login
railway init
railway up
```

### Fly.io
```bash
fly launch
fly deploy
```
