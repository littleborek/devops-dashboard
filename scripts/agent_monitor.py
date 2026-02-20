import os
import socket
import json
import time
import subprocess
import urllib.request

# --- CONFIGURATION ---
MASTER_URL = "http://YOUR_MASTER_IP:8080/api/v1/agent/sync"
SERVER_NAME = socket.gethostname()
# If you leave SERVER_IP empty, the script will try to detect its own public IP
SERVER_IP = "" 

def get_public_ip():
    try:
        return urllib.request.urlopen('https://api.ipify.org').read().decode('utf8')
    except:
        return socket.gethostbyname(socket.gethostname())

def get_cpu_usage():
    try:
        # Get load average from the last minute
        load1, load5, load15 = os.getloadavg()
        cpus = os.cpu_count()
        return min(100.0, (load1 / cpus) * 100.0)
    except:
        return 0.0

def get_ram_usage():
    try:
        with open('/proc/meminfo', 'r') as f:
            lines = f.readlines()
        
        mem_total = 0
        mem_free = 0
        mem_available = 0
        
        for line in lines:
            if 'MemTotal' in line:
                mem_total = int(line.split()[1])
            elif 'MemFree' in line:
                mem_free = int(line.split()[1])
            elif 'MemAvailable' in line:
                mem_available = int(line.split()[1])
        
        if mem_available > 0:
            used = mem_total - mem_available
        else:
            used = mem_total - mem_free
            
        percent = (used / mem_total) * 100.0
        total_gb = f"{round(mem_total / 1024 / 1024, 2)} GB"
        
        return round(percent, 2), total_gb
    except:
        return 0.0, "0 GB"

def get_docker_containers():
    containers = []
    try:
        # Check if docker is running and get containers
        cmd = "docker ps --format '{{json .}}'"
        result = subprocess.check_output(cmd, shell=True).decode('utf-8')
        for line in result.split('\n'):
            if line.strip():
                c = json.loads(line)
                containers.append({
                    "containerId": c["ID"],
                    "name": c["Names"],
                    "image": c["Image"],
                    "state": c["State"],
                    "status": c["Status"]
                })
    except:
        pass # Docker not installed or not running
    return containers

def send_data():
    global SERVER_IP
    if not SERVER_IP:
        SERVER_IP = get_public_ip()
        
    cpu = get_cpu_usage()
    ram_percent, ram_total = get_ram_usage()
    containers = get_docker_containers()
    
    payload = {
        "serverIp": SERVER_IP,
        "serverName": SERVER_NAME,
        "cpuUsage": round(cpu, 2),
        "ramUsage": ram_percent,
        "totalRam": ram_total,
        "containers": containers
    }
    
    print(f"Sending data to {MASTER_URL}...")
    print(json.dumps(payload, indent=2))
    
    try:
        req = urllib.request.Request(MASTER_URL)
        req.add_header('Content-Type', 'application/json; charset=utf-8')
        jsondata = json.dumps(payload).encode('utf-8')
        
        with urllib.request.urlopen(req, jsondata) as f:
            print(f"Response: {f.read().decode('utf-8')}")
    except Exception as e:
        print(f"Error sending data: {e}")

if __name__ == "__main__":
    send_data()
