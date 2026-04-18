import asyncio
import json
import os
from orchestrator.main_graph import app_workflow

async def run_test(query: str, description: str):
    print(f"\n{'='*60}")
    print(f"SCENARIO: {description}")
    print(f"QUERY: '{query}'")
    print(f"{'='*60}")
    
    initial_state = {
        "query": query,
        "context": "DevOps Dashboard Production Environment",
        "response": "",
        "mode": "",
        "next_step": ""
    }
    
    try:
        # This calls your local LLM at http://100.119.208.36:1234/v1
        print("DEBUG: LangGraph is thinking...")
        final_state = await app_workflow.ainvoke(initial_state)
        
        print(f"\nROUTE TAKEN: {final_state['mode'].upper()}")
        print("-" * 30)
        print(f"AI RESPONSE:\n{final_state['response']}")
        print("-" * 30)
        
    except Exception as e:
        print(f"\nCONNECTION ERROR: Could not reach LLM at http://100.119.208.36:1234/v1")
        print(f"DETAILS: {str(e)}")
        print("\nTIP: Make sure LM Studio is running and 'Cross-Origin Resource Sharing (CORS)' is enabled if applicable.")

async def main():
    # Scenario 1: A simple question that should go to Direct-LLM
    await run_test(
        "Hello! Can you tell me which services you are currently monitoring?",
        "Simple Information Request (Routes to Direct-LLM)"
    )
    
    print("\n" + "#"*60 + "\n")
    
    # Scenario 2: A complex technical issue that should trigger CrewAI
    await run_test(
        "There is a critical error in the production database logs showing 'Connection Pool Exhausted'. Analyze and suggest an immediate fix.",
        "Complex Technical Issue (Routes to CrewAI Team)"
    )

if __name__ == "__main__":
    # Ensure environment variables are set correctly for the test
    os.environ["OPENAI_API_BASE"] = "http://100.119.208.36:1234/v1"
    os.environ["OPENAI_MODEL_NAME"] = "mistralai-mistral-nemo-instruct-2407-12b-mpoa-v1"
    
    asyncio.run(main())
