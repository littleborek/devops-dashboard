from typing import Annotated, NotRequired, TypedDict

from pydantic import BaseModel, StringConstraints

# LangGraph State
class DevOpsState(TypedDict):
    query: str
    context: str
    response: NotRequired[str]
    mode: NotRequired[str]
    agent_count: NotRequired[int]

# FastAPI Request Model
class AnalyzeRequest(BaseModel):
    query: Annotated[str, StringConstraints(strip_whitespace=True, min_length=1)]
    context: str = "DevOps Dashboard System Analysis"

# FastAPI Response Model
class AnalyzeResponse(BaseModel):
    result: str
    status: str
    mode: str
