import os
from crewai import Agent, Task, Crew, Process
from langchain_openai import ChatOpenAI
from core.config import settings

# Shared LLM Configuration via Environment Variables
os.environ["OPENAI_API_BASE"] = settings.OPENAI_API_BASE
os.environ["OPENAI_API_KEY"] = settings.OPENAI_API_KEY
os.environ["OPENAI_MODEL_NAME"] = settings.OPENAI_MODEL_NAME
os.environ["OPENAI_TIMEOUT"] = "300" 


# 1. Agents
llm_model = settings.OPENAI_MODEL_NAME
# Ensure it has a provider prefix for LiteLLM
if "/" not in llm_model:
    llm_model = f"openai/{llm_model}"

system_analyst = Agent(
    role='System Data Analyst',
    goal='Extract specific metrics or identify technical issues requested by the user.',
    backstory="""You are a data-driven specialist. You look at telemetry signals 
    and extract only what is relevant to the user query. You avoid fluff and 
    focus on accuracy.""",
    verbose=True,
    allow_delegation=False,
    llm=llm_model
)

devops_engineer = Agent(
    role='Response Engineer',
    goal='Answer user questions directly and provide solutions for technical problems.',
    backstory="""You are an expert communicator and problem solver. 
    Your first priority is to answer the user's question directly.""",
    verbose=True,
    allow_delegation=False,
    llm=llm_model
)

# 2. Tasks Generator
def create_tasks(query, context):
    diagnostic_task = Task(
        description=f"Analyze context: '{context}' for query: '{query}'. Extract metrics or identify issues.",
        expected_output="Relevant data or diagnosis required to answer the user query.",
        agent=system_analyst
    )

    resolution_task = Task(
        description=f"Based on the data, provide the final response to: '{query}'. Answer directly and concisely.",
        expected_output="A direct answer or a focused DevOps report based on user intent.",
        agent=devops_engineer,
        context=[diagnostic_task]
    )

    return [diagnostic_task, resolution_task]

# 3. Execution Wrapper
def run_crew_analysis(query: str, context: str) -> str:
    tasks = create_tasks(query, context)
    crew = Crew(
        agents=[system_analyst, devops_engineer],
        tasks=tasks,
        verbose=True,
        process=Process.sequential
    )
    return str(crew.kickoff())
