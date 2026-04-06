from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from crewai import Crew, Process
from agents import devops_expert
from tasks import create_tasks
import uvicorn

app = FastAPI()

class AnalyzeRequest(BaseModel):
    query: str
    context: str = "DevOps Dashboard Sistem Analizi"

@app.post("/api/v1/crew/analyze")
async def analyze(request: AnalyzeRequest):
    try:
        tasks = create_tasks(request.query, request.context)
        
        # Sadece bu görev ve bu ajanla çalıştır
        crew = Crew(
            agents=[devops_expert],
            tasks=tasks,
            verbose=True,
            process=Process.sequential
        )
        
        result = crew.kickoff()
        
        # Java tarafının beklentisine göre JSON objesi dönüyoruz
        return {"result": str(result), "status": "success"}
        
    except Exception as e:
        print(f"Error executing crew: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
