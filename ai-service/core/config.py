import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()
os.environ.setdefault("CREWAI_DISABLE_TELEMETRY", "true")

class Settings:
    # LLM Configuration
    OPENAI_API_BASE = os.environ.get("OPENAI_API_BASE", "http://localhost:1234/v1")
    OPENAI_MODEL_NAME = os.environ.get("OPENAI_MODEL_NAME", "openai/mistralai/mistral-nemo-instruct-2407")
    OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "lm-studio")

    @property
    def api_base(self) -> str:
        return self.OPENAI_API_BASE.rstrip("/").removesuffix("/chat/completions")

    @property
    def model_name(self) -> str:
        return self.OPENAI_MODEL_NAME.removeprefix("openai/")

    # LangSmith Configuration
    LANGCHAIN_API_KEY = os.environ.get("LANGCHAIN_API_KEY")
    LANGCHAIN_TRACING_V2 = "true" if LANGCHAIN_API_KEY else "false"
    LANGCHAIN_ENDPOINT = "https://api.smith.langchain.com"
    LANGCHAIN_PROJECT = os.environ.get("LANGCHAIN_PROJECT", "devops-dashboard-ai")

settings = Settings()
