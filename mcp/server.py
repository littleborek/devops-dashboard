"""
DevOps Dashboard MCP Server
============================
Exposes the AI-Ops Dashboard capabilities as MCP tools and resources.
Any MCP-compatible client (Claude Desktop, Cursor, VS Code, etc.) can
connect to this server and use these tools.

Architecture:
  MCP Client  →  [this server]  →  Spring Boot API  (servers, tasks, metrics)
                                →  AI Service API    (CrewAI / LangGraph analysis)
                                →  Local LLM         (direct queries)
"""
import json
from mcp.server.fastmcp import FastMCP
import dashboard_client as api

# ── Initialize MCP Server ──────────────────────────────────────────────
mcp = FastMCP("DevOps Dashboard")


# ═══════════════════════════════════════════════════════════════════════
#  TOOLS — Actions that AI can invoke
# ═══════════════════════════════════════════════════════════════════════

@mcp.tool()
async def analyze_incident(query: str, context: str = "") -> str:
    """
    Analyzes a DevOps incident using the CrewAI multi-agent system.
    The query goes through a LangGraph orchestrator which routes it to either:
    - Direct LLM (for simple questions)
    - CrewAI team (for complex diagnostics with System Analyst + DevOps Engineer)

    Args:
        query: The DevOps question or incident description
               (e.g., "Why is CPU at 99% on production?")
        context: Optional additional context like error logs or metric snapshots
    """
    try:
        result = await api.run_crew_analysis(query, context)
        mode = result.get("mode", "unknown")
        response = result.get("result", "No response received.")
        return f"[Analysis Mode: {mode}]\n\n{response}"
    except Exception as e:
        return f"Error connecting to AI Service: {str(e)}\n\nMake sure the AI service is running (cd ai-service && python3 app.py)"


@mcp.tool()
async def get_server_status(server_id: int = 0) -> str:
    """
    Fetches the current status of all monitored servers, or a specific server.
    Returns CPU, RAM, disk usage, status (ONLINE/OFFLINE), containers, etc.

    Args:
        server_id: Optional. If provided (> 0), fetches only that server.
                   If 0 or omitted, fetches all servers.
    """
    try:
        if server_id > 0:
            server = await api.get_server_by_id(server_id)
            return json.dumps(server, indent=2, default=str)
        else:
            servers = await api.get_all_servers()
            if not servers:
                return "No servers are currently registered in the dashboard."

            lines = []
            for s in servers:
                status = s.get("status", "UNKNOWN")
                emoji = "🟢" if status == "ONLINE" else "🔴"
                lines.append(
                    f"{emoji} {s.get('name', 'N/A')} ({s.get('ipAddress', '?')}) "
                    f"| CPU: {s.get('cpuUsage', 0):.1f}% "
                    f"| RAM: {s.get('ramUsage', 0):.1f}% "
                    f"| Status: {status}"
                )
            return "=== Server Dashboard ===\n" + "\n".join(lines)
    except Exception as e:
        return f"Error fetching server data: {str(e)}\n\nMake sure the Spring Boot backend is running on port 15000."


@mcp.tool()
async def ask_devops_llm(question: str) -> str:
    """
    Asks the local LLM (LM Studio) a DevOps-related question directly,
    bypassing the CrewAI pipeline. Good for quick general knowledge questions.

    Args:
        question: A DevOps question (e.g., "How do I check open ports on Linux?")
    """
    try:
        return await api.query_llm_direct(question)
    except Exception as e:
        return f"Error connecting to LLM: {str(e)}\n\nMake sure LM Studio is running."


@mcp.tool()
async def get_task_queue(server_id: int) -> str:
    """
    Fetches pending remote execution tasks for a specific server.
    These are RSA-signed commands waiting to be picked up by the agent.

    Args:
        server_id: The server ID to check pending tasks for.
    """
    try:
        tasks = await api.get_pending_tasks(server_id)
        if not tasks:
            return f"No pending tasks for server {server_id}."

        lines = []
        for t in tasks:
            lines.append(
                f"Task #{t.get('id')} | Command: {t.get('command', 'N/A')} | "
                f"Status: {t.get('status', 'PENDING')}"
            )
        return f"=== Pending Tasks (Server {server_id}) ===\n" + "\n".join(lines)
    except Exception as e:
        return f"Error fetching tasks: {str(e)}"


# ═══════════════════════════════════════════════════════════════════════
#  RESOURCES — Read-only data that AI can inspect
# ═══════════════════════════════════════════════════════════════════════

@mcp.resource("devops://architecture")
def get_architecture() -> str:
    """Returns the system architecture overview of the AI-Ops Dashboard."""
    return """
=== AI-Ops Dashboard Architecture ===

┌─────────────────────────────────────────────────────┐
│  Frontend (Angular)                                  │
│  - Notification Hub, Chat Interface, Server Views   │
└──────────────────┬──────────────────────────────────┘
                   │ HTTP / WebSocket
┌──────────────────▼──────────────────────────────────┐
│  Backend (Spring Boot - Java 17)                     │
│  - REST API, mTLS Auth, RSA Command Signing         │
│  - PostgreSQL, Prometheus Metrics Export             │
│  Port: 15000                                         │
└──────┬───────────────────────────┬──────────────────┘
       │                           │
┌──────▼──────┐             ┌──────▼──────────────────┐
│  AI Service │             │  Remote Agents           │
│  (Python)   │             │  (Bash/Python)           │
│  - LangGraph│             │  - mTLS + RSA Verify     │
│  - CrewAI   │             │  - Docker Log Stream     │
│  Port: 8000 │             │  - System Metrics        │
└─────────────┘             └──────────────────────────┘

Agents:
  1. System Data Analyst - Extracts metrics & identifies anomalies
  2. Response Engineer   - Provides solutions & executable commands

Routing:
  SIMPLE queries  → Direct LLM (fast, single-pass)
  COMPLEX queries → CrewAI (multi-agent analysis)
"""


@mcp.resource("devops://config")
def get_current_config() -> str:
    """Returns the current MCP server configuration (credentials masked)."""
    from config import config
    return (
        f"Dashboard API: {config.DASHBOARD_API_BASE}\n"
        f"AI Service:    {config.AI_SERVICE_URL}\n"
        f"LLM Endpoint:  {config.OPENAI_API_BASE}\n"
        f"LLM Model:     {config.OPENAI_MODEL_NAME}\n"
        f"mTLS:          {'Enabled' if config.CLIENT_CERT_PATH else 'Disabled'}"
    )


# ═══════════════════════════════════════════════════════════════════════
#  PROMPTS — Pre-built prompt templates
# ═══════════════════════════════════════════════════════════════════════

@mcp.prompt()
def rca_prompt(server_name: str, issue: str) -> str:
    """Generates a Root Cause Analysis prompt for a specific server issue."""
    return (
        f"You are an expert Site Reliability Engineer (SRE).\n"
        f"Perform a Root Cause Analysis for the following issue:\n\n"
        f"Server: {server_name}\n"
        f"Issue: {issue}\n\n"
        f"Please provide:\n"
        f"1. Most likely root cause\n"
        f"2. Immediate remediation steps\n"
        f"3. Long-term prevention recommendations"
    )


@mcp.prompt()
def health_check_prompt() -> str:
    """Generates a prompt to perform a full system health check."""
    return (
        "Use the get_server_status tool to fetch all server statuses, "
        "then analyze the results and report:\n"
        "1. Any servers that are OFFLINE or have high CPU/RAM usage (>80%)\n"
        "2. Overall system health summary\n"
        "3. Recommended actions if any issues are found"
    )


# ═══════════════════════════════════════════════════════════════════════
#  Entry Point
# ═══════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    mcp.run()
