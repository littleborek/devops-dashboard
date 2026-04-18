import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

class Settings:
    # LLM Configuration
    OPENAI_API_BASE = os.environ.get("OPENAI_API_BASE", "http://100.119.208.36:1234/v1")
    OPENAI_MODEL_NAME = os.environ.get("OPENAI_MODEL_NAME", "mistralai-mistral-nemo-instruct-2407-12b-mpoa-v1")
    OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "lm-studio")

    
    # LangSmith Configuration
    LANGCHAIN_API_KEY = os.environ.get("LANGCHAIN_API_KEY")
    LANGCHAIN_TRACING_V2 = "true" if LANGCHAIN_API_KEY else "false"
    LANGCHAIN_ENDPOINT = "https://api.smith.langchain.com"
    LANGCHAIN_PROJECT = os.environ.get("LANGCHAIN_PROJECT", "devops-dashboard-ai")

settings = Settings()
