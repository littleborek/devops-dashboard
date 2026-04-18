import os
import json
import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv

# Load environment variables from .env if present
load_dotenv()

# langgraph workflow
from workflow import app_workflow
import uvicorn

app = FastAPI()

# LangSmith Configuration
if os.environ.get("LANGCHAIN_API_KEY"):
    os.environ["LANGCHAIN_TRACING_V2"] = "true"
    os.environ["LANGCHAIN_ENDPOINT"] = "https://api.smith.langchain.com"
    os.environ["LANGCHAIN_PROJECT"] = os.environ.get("LANGCHAIN_PROJECT", "devops-dashboard-ai")
    print("DEBUG: LangSmith tracing enabled.")
else:
    print("DEBUG: LangSmith API Key not found. Tracing disabled.")

class AnalyzeRequest(BaseModel):
    query: str = ""
    context: str = "DevOps Dashboard System Analysis"

@app.post("/api/v1/crew/analyze")
async def analyze(request: AnalyzeRequest):
    try:
        # Execute LangGraph Workflow
        initial_state = {
            "query": request.query,
            "context": request.context,
            "response": "",
            "mode": "",
            "next_step": ""
        }
        
        print(f"DEBUG: Starting LangGraph workflow for: '{request.query}'")
        final_state = await app_workflow.ainvoke(initial_state)
        
        return {
            "result": final_state["response"],
            "status": "success",
            "mode": final_state["mode"]
        }
        
    except Exception as e:
        print(f"Error executing workflow: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
