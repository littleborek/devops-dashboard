import asyncio
import logging
import os
from fastapi import FastAPI, HTTPException
from core.schema import AnalyzeRequest, AnalyzeResponse
from core.config import settings
from orchestrator.main_graph import app_workflow
import uvicorn

app = FastAPI(title="DevOps AI Service")
logger = logging.getLogger(__name__)

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
        async with asyncio.timeout(285):
            final_state = await app_workflow.ainvoke({"query": request.query, "context": request.context})
        
        return {
            "result": final_state["response"],
            "status": "success",
            "mode": final_state["mode"]
        }
    except TimeoutError as e:
        logger.warning("AI workflow timed out")
        raise HTTPException(status_code=504, detail="The AI request timed out.") from e
    except Exception as e:
        logger.error("AI workflow failed (%s)", type(e).__name__)
        raise HTTPException(status_code=502, detail="The AI provider could not complete the request.") from e

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
