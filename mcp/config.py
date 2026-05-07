"""
Configuration for the DevOps Dashboard MCP Server.
Reads from environment variables or .env file.
"""
import os
from dotenv import load_dotenv

load_dotenv()


class MCPConfig:
    # Spring Boot Backend API
    DASHBOARD_API_BASE: str = os.environ.get("DASHBOARD_API_BASE", "https://localhost:15000/api/v1")

    # Python AI Service (FastAPI + CrewAI)
    AI_SERVICE_URL: str = os.environ.get("AI_SERVICE_URL", "http://localhost:8000/api/v1/crew/analyze")

    # LLM Configuration (for direct queries)
    OPENAI_API_BASE: str = os.environ.get("OPENAI_API_BASE", "http://100.119.208.36:1234/v1")
    OPENAI_API_KEY: str = os.environ.get("OPENAI_API_KEY", "lm-studio")
    OPENAI_MODEL_NAME: str = os.environ.get("OPENAI_MODEL_NAME", "mistralai-mistral-nemo-instruct-2407-12b-mpoa-v1")

    # mTLS Certificate Paths (for secure communication with Spring Boot backend)
    CLIENT_CERT_PATH: str = os.environ.get("CLIENT_CERT_PATH", "../certs/client.crt")
    CLIENT_KEY_PATH: str = os.environ.get("CLIENT_KEY_PATH", "../certs/client.key")
    CA_CERT_PATH: str = os.environ.get("CA_CERT_PATH", "../certs/rootCA.crt")


config = MCPConfig()
