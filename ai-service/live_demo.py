import asyncio

from core.config import settings
from orchestrator.main_graph import app_workflow

async def run_test(query: str, description: str):
    print(f"\n{'='*60}")
    print(f"SCENARIO: {description}")
    print(f"QUERY: '{query}'")
    print(f"{'='*60}")
    
    initial_state = {
        "query": query,
        "context": "DevOps Dashboard Production Environment",
    }
    
    try:
        print("DEBUG: LangGraph is thinking...")
        final_state = await app_workflow.ainvoke(initial_state)
        
        print(f"\nCREWAI AGENTS: {final_state['agent_count']}")
        print("-" * 30)
        print(f"AI RESPONSE:\n{final_state['response']}")
        print("-" * 30)
        
    except Exception as e:
        print(f"\nAI request to {settings.api_base} failed")
        print(f"DETAILS: {str(e)}")

async def main():
    await run_test(
        "Hello! Can you tell me which services you are currently monitoring?",
        "Simple Information Request"
    )
    
    print("\n" + "#"*60 + "\n")
    
    await run_test(
        "There is a critical error in the production database logs showing 'Connection Pool Exhausted'. Analyze and suggest an immediate fix.",
        "Complex Technical Issue (Routes to CrewAI Team)"
    )

if __name__ == "__main__":
    asyncio.run(main())
