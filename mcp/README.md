# DevOps Dashboard MCP Server

Bu klasör, AI-Ops Dashboard'un **Model Context Protocol (MCP)** sunucusunu barındırır.

## Nedir?

MCP Server, dashboard'un yeteneklerini (sunucu izleme, AI analiz, görev kuyruğu) standart bir protokol üzerinden dış dünyaya açar. Claude Desktop, Cursor, VS Code veya herhangi bir MCP uyumlu istemci bu server'a bağlanıp dashboard'u kullanabilir.

## Dosya Yapısı

```
mcp/
├── server.py           # Ana MCP sunucusu (giriş noktası)
├── dashboard_client.py # Spring Boot + AI Service API istemcisi
├── config.py           # Ortam değişkenleri yönetimi
├── .env.example        # Örnek konfigürasyon
├── requirements.txt    # Python bağımlılıkları
└── README.md           # Bu dosya
```

## Sunulan Yetenekler

### 🔧 Tools (Araçlar)
| Tool | Açıklama |
|------|----------|
| `analyze_incident` | CrewAI çoklu-ajan sistemiyle olay analizi yapar |
| `get_server_status` | Tüm sunucuların veya belirli bir sunucunun durumunu getirir |
| `ask_devops_llm` | Yerel LLM'e doğrudan DevOps sorusu sorar |
| `get_task_queue` | Belirli bir sunucunun bekleyen görevlerini listeler |

### 📖 Resources (Kaynaklar)
| Resource | Açıklama |
|----------|----------|
| `devops://architecture` | Sistem mimarisi diyagramı |
| `devops://config` | Mevcut MCP konfigürasyonu |

### 💬 Prompts (Hazır İstemler)
| Prompt | Açıklama |
|--------|----------|
| `rca_prompt` | Kök Neden Analizi (RCA) şablonu |
| `health_check_prompt` | Tam sistem sağlık kontrolü şablonu |

## Kurulum

```bash
cd mcp
cp .env.example .env
# .env dosyasını düzenle

pip install -r requirements.txt
python server.py
```

## Claude Desktop Konfigürasyonu

`~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "devops-dashboard": {
      "command": "python3.11",
      "args": ["/Users/berk/Projects/devops-dashboard/mcp/server.py"]
    }
  }
}
```

## Mimari

```
Claude Desktop / Cursor / VS Code
         │  (MCP Protocol - stdio)
         ▼
   ┌─────────────┐
   │  MCP Server  │  ← Bu klasör
   │  (server.py) │
   └──┬───────┬───┘
      │       │
      ▼       ▼
Spring Boot   AI Service
(port 15000)  (port 8000)
```
