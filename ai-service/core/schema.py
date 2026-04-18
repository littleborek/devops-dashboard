from typing import TypedDict, Optional
from pydantic import BaseModel

# LangGraph State
class DevOpsState(TypedDict):
    query: str
    context: str
    response: str
    mode: str
    next_step: str

# FastAPI Request Model
class AnalyzeRequest(BaseModel):
    query: str = ""
    context: str = "DevOps Dashboard System Analysis"

# FastAPI Response Model
class AnalyzeResponse(BaseModel):
    result: str
    status: str
    mode: str
