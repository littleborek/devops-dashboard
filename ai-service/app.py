import os
from fastapi import FastAPI, HTTPException
from core.schema import AnalyzeRequest, AnalyzeResponse
from core.config import settings
from orchestrator.main_graph import app_workflow
import uvicorn

app = FastAPI(title="DevOps AI Service")

# LangSmith Activation
if settings.LANGCHAIN_API_KEY:
    os.environ["LANGCHAIN_TRACING_V2"] = "true"
    os.environ["LANGCHAIN_ENDPOINT"] = settings.LANGCHAIN_ENDPOINT
    os.environ["LANGCHAIN_API_KEY"] = settings.LANGCHAIN_API_KEY
    os.environ["LANGCHAIN_PROJECT"] = settings.LANGCHAIN_PROJECT
    print(f"DEBUG: LangSmith tracing enabled for project: {settings.LANGCHAIN_PROJECT}")

@app.post("/api/v1/crew/analyze", response_model=AnalyzeResponse)
async def analyze(request: AnalyzeRequest):
    try:
        initial_state = {
            "query": request.query,
            "context": request.context,
            "response": "",
            "mode": "",
            "next_step": ""
        }
        
        final_state = await app_workflow.ainvoke(initial_state)
        
        return {
            "result": final_state["response"],
            "status": "success",
            "mode": final_state["mode"]
        }
    except Exception as e:
        print(f"Workflow Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
