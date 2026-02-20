#!/bin/bash

# --- CONFIGURATION ---
# Bu değerleri kurulum sırasında dashboard üzerinden otomatik alabiliriz.
MASTER_URL="http://192.168.1.10:8080/api/v1/agent/sync" # Dashboard IP'si
SERVER_NAME=$(hostname)
SERVER_IP=$(hostname -I | awk '{print $1}') # Yerel IP'yi alır. Dış IP için: curl -s https://api.ipify.org

# 1. CPU Kullanımı Hesaplama
CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print 100 - $1}')

# 2. RAM Kullanımı Hesaplama
RAM_TOTAL_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
RAM_AVAIL_KB=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
RAM_TOTAL_GB=$(echo "scale=2; $RAM_TOTAL_KB / 1024 / 1024" | bc)
RAM_USAGE_PERCENT=$(echo "scale=2; ($RAM_TOTAL_KB - $RAM_AVAIL_KB) * 100 / $RAM_TOTAL_KB" | bc)

# 3. Docker Konteynerlerini Toplama
CONTAINERS_JSON="[]"
if command -v docker >/dev/null 2>&1; then
    CONTAINERS_JSON=$(docker ps --format '{"containerId":"{{.ID}}", "name":"{{.Names}}", "image":"{{.Image}}", "state":"{{.State}}", "status":"{{.Status}}"}' | jq -s '.')
    # Eğer jq yüklü değilse basit bir string birleştirme yapılabilir, ancak jq en sağlıklısı.
fi

# 4. JSON Payload Oluşturma
PAYLOAD=$(cat <<EOF
{
  "serverIp": "$SERVER_IP",
  "serverName": "$SERVER_NAME",
  "cpuUsage": $CPU_USAGE,
  "ramUsage": $RAM_USAGE_PERCENT,
  "totalRam": "${RAM_TOTAL_GB} GB",
  "containers": $CONTAINERS_JSON
}
EOF
)

# 5. Gönderim
curl -X POST -H "Content-Type: application/json" -d "$PAYLOAD" "$MASTER_URL"
