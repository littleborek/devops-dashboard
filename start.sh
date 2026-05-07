#!/bin/bash

# AI-Ops Dashboard Start Script
# ============================

# Renkli loglar
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 AI-Ops Dashboard başlatılıyor...${NC}"

# Proje dizinine geç
cd "$(dirname "$0")"

# Çıkışta tüm alt süreçleri temizle
cleanup() {
    echo -e "\n${YELLOW}🛑 Servisler durduruluyor...${NC}"
    # Docker'ı durdur (opsiyonel, veritabanı kalsın istenebilir ama temizlik için stop iyidir)
    docker-compose stop postgres > /dev/null 2>&1
    
    # Arka plan işlemlerini öldür
    PIDS=$(jobs -p)
    if [ -n "$PIDS" ]; then
        kill $PIDS
    fi
    exit
}
trap cleanup SIGINT SIGTERM

# 1. Docker/OrbStack Kontrolü
if ! docker info > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Docker çalışmıyor. OrbStack başlatılıyor...${NC}"
    open -a OrbStack
    while ! docker info > /dev/null 2>&1; do
        echo "   > Docker bekleniyor..."
        sleep 3
    done
    echo -e "${GREEN}✅ Docker hazır.${NC}"
fi

# 2. Veritabanını Başlat
echo -e "${BLUE}🐘 PostgreSQL başlatılıyor...${NC}"
docker-compose up -d postgres
if [ $? -ne 0 ]; then
    echo -e "❌ Veritabanı başlatılamadı!"
    exit 1
fi

# 3. AI Service Başlat (Arka planda)
echo -e "${BLUE}🤖 AI Service başlatılıyor...${NC}"
./venv311/bin/python ai-service/app.py > ai_service.log 2>&1 &
echo "   > Loglar: ai_service.log"

# 4. MCP Server Başlat (Arka planda)
echo -e "${BLUE}🔌 MCP Server başlatılıyor...${NC}"
# Not: MCP sunucusu stdio kullanıyorsa arka planda çalışması log üretebilir
./venv311/bin/python mcp/server.py > mcp_server.log 2>&1 &
echo "   > Loglar: mcp_server.log"

# 5. Spring Boot Backend Başlat (Ön planda)
echo -e "${GREEN}💻 Dashboard Backend başlatılıyor (Port 15000)...${NC}"
echo -e "${YELLOW}Kapatmak için CTRL+C tuşlarına basın.${NC}"
./mvnw spring-boot:run
