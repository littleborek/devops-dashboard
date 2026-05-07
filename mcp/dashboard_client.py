"""
Dashboard API Client.
Communicates with the Spring Boot backend (mTLS secured) to fetch
server data, metrics, task queue info, etc.
"""
import os
import ssl
import httpx
from config import config


def _get_ssl_context() -> ssl.SSLContext | None:
    """Creates an mTLS SSL context using client certificates if they exist."""
    if not all([config.CA_CERT_PATH, config.CLIENT_CERT_PATH, config.CLIENT_KEY_PATH]):
        return None
    
    # Check if files actually exist
    if not all(os.path.exists(p) for p in [config.CA_CERT_PATH, config.CLIENT_CERT_PATH, config.CLIENT_KEY_PATH]):
        return None

    try:
        ctx = ssl.create_default_context(cafile=config.CA_CERT_PATH)
        ctx.load_cert_chain(certfile=config.CLIENT_CERT_PATH, keyfile=config.CLIENT_KEY_PATH)
        return ctx
    except Exception:
        return None


async def get_all_servers() -> list[dict]:
    """Fetches all registered servers from the Spring Boot backend."""
    verify = _get_ssl_context() if config.DASHBOARD_API_BASE.startswith("https") else False
    async with httpx.AsyncClient(verify=verify) as client:
        resp = await client.get(f"{config.DASHBOARD_API_BASE}/servers")
        resp.raise_for_status()
        return resp.json()


async def get_server_by_id(server_id: int) -> dict:
    """Fetches a single server's details by ID."""
    verify = _get_ssl_context() if config.DASHBOARD_API_BASE.startswith("https") else False
    async with httpx.AsyncClient(verify=verify) as client:
        resp = await client.get(f"{config.DASHBOARD_API_BASE}/servers/{server_id}")
        resp.raise_for_status()
        return resp.json()


async def run_crew_analysis(query: str, context: str = "") -> dict:
    """
    Sends a query to the Python AI Service (FastAPI + CrewAI/LangGraph).
    This triggers the full LangGraph orchestrator → Router → Direct LLM or CrewAI.
    """
    async with httpx.AsyncClient(timeout=120.0) as client:
        payload = {"query": query, "context": context or "DevOps Dashboard System Analysis"}
        resp = await client.post(config.AI_SERVICE_URL, json=payload)
        resp.raise_for_status()
        return resp.json()


async def get_pending_tasks(server_id: int) -> list[dict]:
    """Fetches pending tasks for a specific server from the task queue."""
    verify = _get_ssl_context() if config.DASHBOARD_API_BASE.startswith("https") else False
    async with httpx.AsyncClient(verify=verify) as client:
        resp = await client.get(f"{config.DASHBOARD_API_BASE}/tasks/pending/{server_id}")
        resp.raise_for_status()
        return resp.json()


async def query_llm_direct(prompt: str) -> str:
    """Sends a direct prompt to the local LLM (LM Studio) without CrewAI."""
    async with httpx.AsyncClient(timeout=30.0) as client:
        payload = {
            "model": config.OPENAI_MODEL_NAME,
            "messages": [
                {"role": "system", "content": "You are a helpful DevOps assistant."},
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.7,
        }
        resp = await client.post(f"{config.OPENAI_API_BASE}/chat/completions", json=payload)
        resp.raise_for_status()
        return resp.json()["choices"][0]["message"]["content"]
