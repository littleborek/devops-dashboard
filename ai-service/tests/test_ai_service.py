import asyncio
import json
from unittest.mock import AsyncMock

import httpx
import pytest
from openai import OpenAI

from app import app
from crewai.llms.providers.openai import completion
from core.config import settings
from orchestrator import main_graph


async def analyze(query):
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        return await client.post("/api/v1/crew/analyze", json={"query": query, "context": "CPU: 95%"})


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("query", "label", "agent_count"),
    [
        ("Selam", None, 1),
        ("Kritik kesinti var. Veritabani online olmasi iletisim sorununu dislar mi?", None, 3),
        ("Kritik kesinti nedir?", "SIMPLE", 1),
        ("Sunucu durumu nedir?", "SIMPLE", 1),
        ("CPU neden yuksek?", "DIAGNOSTIC", 2),
        ("Production kesintisinin kok nedenini bul.", "INCIDENT", 3),
        ("Serviste hata var.", "invalid label", 2),
    ],
)
async def test_real_crew_with_mock_provider(monkeypatch, tmp_path, query, label, agent_count):
    monkeypatch.setenv("CREWAI_STORAGE_DIR", str(tmp_path))
    monkeypatch.setattr(settings, "OPENAI_API_BASE", "http://model.test/v1/")
    monkeypatch.setattr(settings, "OPENAI_API_KEY", "test-token")
    monkeypatch.setattr(settings, "OPENAI_MODEL_NAME", "openai/local/test-model")
    model_calls = []
    route_calls = []

    def mock_completion(request):
        assert str(request.url) == "http://model.test/v1/chat/completions"
        assert request.headers["Authorization"] == "Bearer test-token"
        payload = json.loads(request.content)
        assert payload["model"] == "local/test-model"
        messages = " ".join(message["content"] for message in payload["messages"])
        if "Classify the user's DevOps query" in messages:
            route_calls.append(messages)
            content = label
        else:
            model_calls.append(messages)
            content = f"Final Answer: Model response {len(model_calls)}"
        return httpx.Response(200, json={
            "id": "test-completion",
            "object": "chat.completion",
            "created": 0,
            "model": payload["model"],
            "choices": [{"index": 0, "message": {"role": "assistant", "content": content},
                         "finish_reason": "stop"}],
        })

    transport = httpx.MockTransport(mock_completion)
    monkeypatch.setattr(main_graph, "AsyncClient", lambda **kw: httpx.AsyncClient(transport=transport, **kw))
    monkeypatch.setattr(completion, "OpenAI", lambda **kw: OpenAI(http_client=httpx.Client(transport=transport), **kw))

    response = await analyze(query)
    assert response.status_code == 200, response.text
    assert response.json() == {
        "status": "success",
        "mode": "direct-llm" if agent_count == 1 else "full-analysis",
        "result": f"Model response {agent_count}",
    }
    assert len(route_calls) == (label is not None)
    assert len(model_calls) == agent_count
    assert "DevOps Engineer" in model_calls[-1]
    if agent_count >= 2:
        assert "Telemetry Analyst" in model_calls[0]
        assert "Model response 1" in model_calls[-1]
    if agent_count == 3:
        assert "Incident Reviewer" in model_calls[1]
        assert "Model response 2" in model_calls[-1]


@pytest.mark.asyncio
@pytest.mark.parametrize(("error", "status"), [(RuntimeError, 502), (TimeoutError, 504)])
async def test_provider_failure_is_not_reported_as_success(monkeypatch, error, status):
    monkeypatch.setattr(main_graph, "run_crew_analysis", AsyncMock(side_effect=error("private endpoint")))
    response = await analyze("Hello")
    assert response.status_code == status
    assert "private endpoint" not in response.text


@pytest.mark.asyncio
async def test_blank_query_is_rejected():
    assert (await analyze("   ")).status_code == 422


@pytest.mark.asyncio
async def test_routing_timeout_uses_keyword_fallback(monkeypatch):
    def timeout(request):
        raise httpx.ReadTimeout("timeout", request=request)

    monkeypatch.setattr(main_graph, "AsyncClient", lambda **kw: httpx.AsyncClient(
        transport=httpx.MockTransport(timeout), **kw
    ))
    crew = AsyncMock(return_value="response")
    monkeypatch.setattr(main_graph, "run_crew_analysis", crew)
    query = "Production kesintisi var."
    response = await asyncio.wait_for(analyze(query), timeout=5)
    assert response.status_code == 200
    crew.assert_awaited_once_with(query, "CPU: 95%", 3)
