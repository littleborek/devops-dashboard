from httpx import AsyncClient
from langgraph.graph import StateGraph, END
from core.schema import DevOpsState
from core.config import settings
from agents.devops_crew import run_crew_analysis

def get_heuristic_complex(query: str) -> bool:
    """Fallback heuristic if LLM routing fails."""
    complex_keywords = ["error", "fail", "slow", "offline", "cpu", "ram", "troubleshoot", "fix", "diagnostic", "problem"]
    query_lower = query.lower()
    return any(kw in query_lower for kw in complex_keywords) or len(query.split()) > 4

async def classify_intent(query: str, context: str) -> str:
    """Uses LLM to classify if a query is SIMPLE or COMPLEX."""
    async with AsyncClient(timeout=10.0) as client:
        prompt = f"""
        Analyze the following DevOps related query and classify it.
        
        Context: {context}
        Query: {query}
        
        Classification Rules:
        - SIMPLE: Basic greetings, general information requests, or simple status checks that don't need deep analysis.
        - COMPLEX: Error troubleshooting, performance diagnostics, log analysis, or requests for technical solutions.
        
        Return ONLY one word: 'SIMPLE' or 'COMPLEX'.
        """
        
        payload = {
            "model": settings.OPENAI_MODEL_NAME,
            "messages": [
                {"role": "system", "content": "You are a routing assistant. Be concise and return only one word."},
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.0 # Strict classification
        }
        
        try:
            response = await client.post(f"{settings.OPENAI_API_BASE}/chat/completions", json=payload)
            response.raise_for_status()
            result = response.json()["choices"][0]["message"]["content"].strip().upper()
            print(f"DEBUG: LLM Router classified as: {result}")
            return "crew" if "COMPLEX" in result else "direct"
        except Exception as e:
            print(f"DEBUG: LLM Router failed ({e}), falling back to heuristic.")
            return "crew" if get_heuristic_complex(query) else "direct"

async def router_node(state: DevOpsState):
    print(f"--- ROUTING QUERY: {state['query']} ---")
    next_step = await classify_intent(state['query'], state['context'])
    return {"next_step": next_step}

async def direct_llm_node(state: DevOpsState):
    print("--- DIRECT LLM NODE ---")
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
    print("--- CREWAI NODE ---")
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
