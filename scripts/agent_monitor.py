import os
import socket
import json
import time
import subprocess
import urllib.request
import urllib.error
import ssl
import base64
import threading
from datetime import datetime

# --- CONFIGURATION ---
MASTER_IP = os.environ.get("MASTER_IP", "localhost")
MASTER_PORT = 15000
BASE_URL = f"https://{MASTER_IP}:{MASTER_PORT}/api/v1"

CERTS_DIR = os.environ.get("CERTS_DIR", "../certs")
ROOT_CA = os.path.join(CERTS_DIR, "rootCA.crt")
CLIENT_CRT = os.path.join(CERTS_DIR, "client.crt")
CLIENT_KEY = os.path.join(CERTS_DIR, "client.key")
PUB_KEY = os.path.join(CERTS_DIR, "signing_public.pem")

SERVER_NAME = socket.gethostname()
SERVER_IP = os.environ.get("SERVER_IP", "")  # Let it be auto-detected or provided, though we can skip auto-detect if hardcoded


# Create strict SSL Context for mTLS
def get_ssl_context():
    context = ssl.create_default_context(cafile=ROOT_CA)
    context.load_cert_chain(certfile=CLIENT_CRT, keyfile=CLIENT_KEY)
    # context.check_hostname = False # Optional, if you're using IPs instead of CNs this might be necessary
    return context

# Store SSL context so we don't recreate it every time
ssl_context = get_ssl_context()

def get_public_ip():
    try:
        req = urllib.request.Request('https://api.ipify.org')
        with urllib.request.urlopen(req, timeout=5) as res:
            return res.read().decode('utf8')
    except:
        return "127.0.0.1" # Fallback

if not SERVER_IP:
    SERVER_IP = get_public_ip()

def api_request(method, endpoint, payload=None):
    url = f"{BASE_URL}{endpoint}"
    # print(f"[{method}] {url}")
    try:
        req = urllib.request.Request(url, method=method)
        req.add_header('Content-Type', 'application/json; charset=utf-8')
        
        jsondata = None
        if payload is not None:
            jsondata = json.dumps(payload).encode('utf-8')
            
        with urllib.request.urlopen(req, data=jsondata, context=ssl_context, timeout=10) as f:
            resp = f.read().decode('utf-8')
            return json.loads(resp) if resp else None
    except urllib.error.URLError as e:
        print(f"API Error ({url}): {e}")
        return None
    except Exception as e:
        print(f"General Error API ({url}): {e}")
        return None

def verify_signature(command: str, signature_b64: str) -> bool:
    try:
        with open("cmd.tmp", "wb") as f:
            f.write(command.encode('utf-8')) # Use exact bytes of command text
        
        sig_bytes = base64.b64decode(signature_b64)
        with open("sig.tmp", "wb") as f:
            f.write(sig_bytes)
        
        # We assume openssl is installed on the agent
        result = subprocess.run(
            ["openssl", "dgst", "-sha256", "-verify", PUB_KEY, "-signature", "sig.tmp", "cmd.tmp"],
            capture_output=True, text=True
        )
        
        os.remove("cmd.tmp")
        os.remove("sig.tmp")
        
        if "Verified OK" in result.stdout:
            return True
        else:
            print("RSA Verification failed for command:", command)
            print("Openssl output:", result.stderr, result.stdout)
            return False
    except Exception as e:
        print("Exception during RSA verification:", e)
        return False

# ----- LOGGING STREAM -----
running_streams = {}

def stream_logs(container_id, server_id):
    if container_id in running_streams:
        print(f"Stream for {container_id} is already running.")
        return
        
    running_streams[container_id] = True
    print(f"Starting log stream for container: {container_id}")
    
    try:
        process = subprocess.Popen(
            ["docker", "logs", "-f", "--tail", "50", container_id],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True
        )
        
        while running_streams.get(container_id):
            line = process.stdout.readline()
            if not line:
                break
                
            line = line.strip()
            if line:
                # Send log line to backend
                log_msg = {
                    "serverId": str(server_id),
                    "content": line,
                    "level": "INFO",
                    "timestamp": datetime.now().isoformat()
                }
                api_request("POST", "/agent/logs/publish", log_msg)
                
    except Exception as e:
        print(f"Error streaming logs for {container_id}: {e}")
    finally:
        print(f"Log stream stopped for {container_id}")
        if container_id in running_streams:
            del running_streams[container_id]
        if 'process' in locals():
            process.kill()

# ----- TASKS POLLING -----
def poll_tasks(server_id):
    tasks = api_request("GET", f"/tasks/pending/{server_id}")
    if not tasks:
        return
        
    for task in tasks:
        task_id = task.get("id")
        command = task.get("command")
        signature = task.get("signature")
        
        print(f"\n--- Received Task {task_id} ---")
        print(f"Command: {command}")
        
        if not signature:
            print("Task rejected: No signature provided.")
            api_request("POST", f"/tasks/result/{task_id}", {"status": "FAILED", "result": "No RSA signature provided"})
            continue
            
        if not verify_signature(command, signature):
            print("Task rejected: Invalid signature.")
            api_request("POST", f"/tasks/result/{task_id}", {"status": "FAILED", "result": "Invalid RSA signature"})
            continue
            
        print("Signature verified successfully! Executing...")
        
        # Handle special commands like STREAM_LOG <container_id>
        if command.startswith("STREAM_LOG "):
            container_id = command.split(" ", 1)[1].strip()
            threading.Thread(target=stream_logs, args=(container_id, server_id), daemon=True).start()
            api_request("POST", f"/tasks/result/{task_id}", {"status": "EXECUTED", "result": f"Started log stream for {container_id}"})
            continue

        if command.startswith("STOP_LOG "):
            container_id = command.split(" ", 1)[1].strip()
            if container_id in running_streams:
                del running_streams[container_id]
            api_request("POST", f"/tasks/result/{task_id}", {"status": "EXECUTED", "result": f"Stopped log stream for {container_id}"})
            continue
            
        # Execute normal bash command
        try:
            result = subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT, text=True)
            api_request("POST", f"/tasks/result/{task_id}", {"status": "EXECUTED", "result": result})
            print(f"Task {task_id} executed successfully.")
        except subprocess.CalledProcessError as e:
            api_request("POST", f"/tasks/result/{task_id}", {"status": "FAILED", "result": e.output})
            print(f"Task {task_id} failed: {e.output}")


# ----- MAIN LOOP -----
def get_cpu_usage():
    try:
        load1, load5, load15 = os.getloadavg()
        cpus = os.cpu_count() or 1
        return min(100.0, (load1 / cpus) * 100.0)
    except:
        return 0.0

def get_ram_usage():
    try:
        with open('/proc/meminfo', 'r') as f:
            lines = f.readlines()
        mem_total = mem_free = mem_available = 0
        for line in lines:
            if 'MemTotal' in line: mem_total = int(line.split()[1])
            elif 'MemFree' in line: mem_free = int(line.split()[1])
            elif 'MemAvailable' in line: mem_available = int(line.split()[1])
        used = (mem_total - mem_available) if mem_available > 0 else (mem_total - mem_free)
        return round((used / mem_total) * 100.0, 2), f"{round(mem_total / 1024 / 1024, 2)} GB"
    except:
        return 0.0, "0 GB"

def get_docker_containers():
    containers = []
    try:
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
        pass
    return containers

def send_sync_data():
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
    
    # Send sync data and get the registered server ID
    resp = api_request("POST", "/agent/sync", payload)
    if resp and "id" in resp:
        return resp["id"]
    return None

if __name__ == "__main__":
    print(f"Starting Secure Agent (mTLS+RSA) for {SERVER_NAME} ({SERVER_IP})")
    
    # Wait for initial sync to get our server ID
    server_id = None
    while not server_id:
        print("Attempting to sync with master...")
        server_id = send_sync_data()
        if not server_id:
            time.sleep(5)
            
    print(f"Successfully synced! Assigned Server ID: {server_id}")
    
    # Main polling and event loop
    loop_count = 0
    while True:
        try:
            # Poll tasks every 5 seconds
            poll_tasks(server_id)
            
            # Re-sync metrics every 15 seconds (every 3rd loop)
            if loop_count % 3 == 0:
                send_sync_data()
                
            loop_count += 1
            time.sleep(5)
            
        except KeyboardInterrupt:
            print("Shutting down agent...")
            for c_id in list(running_streams.keys()):
                running_streams[c_id] = False
            break
        except Exception as e:
            print(f"Error in main loop: {e}")
            time.sleep(5)
