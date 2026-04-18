from httpx import AsyncClient

from langgraph.graph import StateGraph, END
from core.schema import DevOpsState
from core.config import settings
from agents.devops_crew import run_crew_analysis

def is_complex_query(query: str) -> bool:
    complex_keywords = ["error", "fail", "slow", "offline", "cpu", "ram", "troubleshoot", "fix", "diagnostic", "problem"]
    query_lower = query.lower()
    return any(kw in query_lower for kw in complex_keywords) or len(query.split()) > 4

async def router_node(state: DevOpsState):
    if is_complex_query(state['query']):
        return {"next_step": "crew"}
    return {"next_step": "direct"}

async def direct_llm_node(state: DevOpsState):
    async with AsyncClient(timeout=30.0) as client:

        payload = {
            "model": settings.OPENAI_MODEL_NAME,
            "messages": [
                {"role": "system", "content": f"You are a helpful DevOps assistant. Context: {state['context']}. Give a DIRECT, concise answer."},
                {"role": "user", "content": state['query']}
            ],
            "temperature": 0.7
        }
        try:
            response = await client.post(f"{settings.OPENAI_API_BASE}/chat/completions", json=payload)
            response.raise_for_status()
            data = response.json()
            return {"response": data["choices"][0]["message"]["content"], "mode": "direct-llm"}
        except Exception as e:
            return {"response": f"Error: {str(e)}", "mode": "error"}

async def crewai_node(state: DevOpsState):
    try:
        result = run_crew_analysis(state['query'], state['context'])
        return {"response": result, "mode": "full-analysis"}
    except Exception as e:
        return {"response": f"CrewAI Error: {str(e)}", "mode": "error"}

# Build Graph
workflow = StateGraph(DevOpsState)
workflow.add_node("router", router_node)
workflow.add_node("direct_llm", direct_llm_node)
workflow.add_node("crewai", crewai_node)

workflow.set_entry_point("router")
workflow.add_conditional_edges(
    "router",
    lambda x: x["next_step"],
    {"direct": "direct_llm", "crew": "crewai"}
)
workflow.add_edge("direct_llm", END)
workflow.add_edge("crewai", END)

app_workflow = workflow.compile()
