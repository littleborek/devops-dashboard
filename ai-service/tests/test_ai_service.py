import pytest
from httpx import AsyncClient, ASGITransport
from app import app
from unittest.mock import patch, MagicMock

@pytest.mark.asyncio
async def test_analyze_simple_query_mocked():
    """
    Verifies that a simple query is routed and handled correctly (Mocked).
    """
    mock_client_instance = MagicMock()
    mock_client_instance.post = MagicMock()
    
    # Mock return value for the post call
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "choices": [{"message": {"content": "Mocked Direct Response"}}]
    }
    mock_response.raise_for_status = MagicMock()
    
    # Configure the mock post to return our mock response
    # Since it's an async call in the code, it needs to return an awaitable
    async def amock_post(*args, **kwargs):
        return mock_response
    
    mock_client_instance.post.side_effect = amock_post
    mock_client_instance.__aenter__.return_value = mock_client_instance

    # We patch the AsyncClient CLASS inside orchestrator.main_graph
    with patch("orchestrator.main_graph.AsyncClient", return_value=mock_client_instance):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            response = await ac.post("/api/v1/crew/analyze", json={
                "query": "Hi",
                "context": "Test"
            })
    
    assert response.status_code == 200, f"Response: {response.text}"
    data = response.json()
    assert "mode" in data, f"Key 'mode' missing from response: {data}"
    assert data["mode"] == "direct-llm"
    assert "Mocked Direct Response" in data["result"]

@pytest.mark.asyncio
async def test_analyze_complex_query_mocked():
    """
    Verifies that a complex query triggers CrewAI and returns its result (Mocked).
    """
    with patch("orchestrator.main_graph.run_crew_analysis", return_value="Mocked CrewAI Analysis"):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            response = await ac.post("/api/v1/crew/analyze", json={
                "query": "The server is down with error 500 and cpu is high.",
                "context": "Server: Prod"
            })
    
    assert response.status_code == 200, f"Response: {response.text}"
    data = response.json()
    assert data["mode"] == "full-analysis"
    assert "Mocked CrewAI Analysis" in data["result"]

@pytest.mark.asyncio
async def test_error_handling_mocked():
    """
    Verifies the system's resilience when the LLM service is down (Mocked).
    """
    mock_client_instance = MagicMock()
    async def amock_fail(*args, **kwargs):
        raise Exception("Connection Refused")
    
    mock_client_instance.post.side_effect = amock_fail
    mock_client_instance.__aenter__.return_value = mock_client_instance

    with patch("orchestrator.main_graph.AsyncClient", return_value=mock_client_instance):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            response = await ac.post("/api/v1/crew/analyze", json={
                "query": "Hello",
                "context": "Test"
            })
    
    assert response.status_code == 200
    data = response.json()
    assert data["mode"] == "error"
    assert "Connection Refused" in data["result"]
