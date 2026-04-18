import os
from typing import TypedDict, Annotated, Literal
from langgraph.graph import StateGraph, END
from crewai import Crew, Process
from agents import system_analyst, devops_engineer
from tasks import create_specialized_tasks
import httpx

# Models
class DevOpsState(TypedDict):
    query: str
    context: str
    response: str
    mode: str  # 'direct' or 'crew'
    next_step: str

# Config
LLM_BASE_URL = os.environ.get("OPENAI_API_BASE", "http://100.119.208.36:1234/v1")
MODEL_NAME = os.environ.get("OPENAI_MODEL_NAME", "mistral-nemo")

def is_complex_query(query: str) -> bool:
    """Heuristic to decide if we need the full CrewAI team."""
    complex_keywords = ["error", "fail", "slow", "offline", "cpu", "ram", "troubleshoot", "fix", "diagnostic", "problem"]
    query_lower = query.lower()
    if any(kw in query_lower for kw in complex_keywords):
        return True
    if len(query.split()) > 4:
        return True
    return False

async def router_node(state: DevOpsState):
    """Decides whether to go to direct LLM or CrewAI."""
    print(f"--- ROUTING QUERY: {state['query']} ---")
    if is_complex_query(state['query']):
        return {"next_step": "crew"}
    else:
        return {"next_step": "direct"}

async def direct_llm_node(state: DevOpsState):
    """Bypasses agents for simple queries."""
    print("--- DIRECT LLM NODE ---")
    async with httpx.AsyncClient(timeout=30.0) as client:
        payload = {
            "model": MODEL_NAME,
            "messages": [
                {"role": "system", "content": f"You are a helpful DevOps assistant. Context: {state['context']}. Give a DIRECT, concise answer."},
                {"role": "user", "content": state['query']}
            ],
            "temperature": 0.7
        }
        try:
            response = await client.post(f"{LLM_BASE_URL}/chat/completions", json=payload)
            response.raise_for_status()
            data = response.json()
            return {"response": data["choices"][0]["message"]["content"], "mode": "direct-llm"}
        except Exception as e:
            return {"response": f"Error in direct LLM: {str(e)}", "mode": "error"}

async def crewai_node(state: DevOpsState):
    """Executes the CrewAI flow for complex queries."""
    print("--- CREWAI NODE ---")
    try:
        tasks = create_specialized_tasks(state['query'], state['context'])
        crew = Crew(
            agents=[system_analyst, devops_engineer],
            tasks=tasks,
            verbose=True,
            process=Process.sequential
        )
        result = crew.kickoff()
        return {"response": str(result), "mode": "full-analysis"}
    except Exception as e:
        return {"response": f"Error in CrewAI: {str(e)}", "mode": "error"}

# Build the Graph
workflow = StateGraph(DevOpsState)

# Add Nodes
workflow.add_node("router", router_node)
workflow.add_node("direct_llm", direct_llm_node)
workflow.add_node("crewai", crewai_node)

# Set Entry Point
workflow.set_entry_point("router")

# Define Conditional Edges
workflow.add_conditional_edges(
    "router",
    lambda x: x["next_step"],
    {
        "direct": "direct_llm",
        "crew": "crewai"
    }
)

# Connect to End
workflow.add_edge("direct_llm", END)
workflow.add_edge("crewai", END)

# Compile
app_workflow = workflow.compile()
