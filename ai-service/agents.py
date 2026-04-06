import os

# Ortam değişkenleri
os.environ["OPENAI_API_KEY"] = "lm-studio"
os.environ["OPENAI_API_BASE"] = "http://100.119.208.36:1234/v1"
os.environ["OPENAI_BASE_URL"] = "http://100.119.208.36:1234/v1"
os.environ["OPENAI_MODEL_NAME"] = "llama-3.2-3b-instruct"

from crewai import Agent

# Sadece Tek Bir Akıllı Ajan (Basitlik ve hız için)
devops_expert = Agent(
    role='Senior DevOps Expert',
    goal='Provide concise and actionable DevOps advice in Turkish.',
    backstory='You are a helpful and direct DevOps expert. You avoid repetition and give short, useful responses.',
    verbose=True,
    allow_delegation=False
)

# Diğerlerini silebiliriz veya bu isimde kalabilirler ama app.py'de tek ajan kullanacağız.
inspector = devops_expert
architect = devops_expert
manager = devops_expert
