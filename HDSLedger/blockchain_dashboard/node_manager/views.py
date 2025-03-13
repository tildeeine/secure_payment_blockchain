import json
import os
import subprocess
import threading
from django.shortcuts import render, redirect
from django.http import JsonResponse
from django.views import View
from .models import Node, Client, ProcessLog

# A view is a Python function that takes a Web request and returns a Web response. Can be HTML, but also CML, JSON, 404 error, etc.

# Configuration files path
SERVER_CONFIG_PATH = "../Service/src/main/resources/regular_config.json"
CLIENT_CONFIG_PATH = "../Client/src/main/resources/client_config.json"

# Store process objects
node_processes = {}
client_processes = {}

setup = False

def start_node(node_id, config_file="regular_config.json"):
    """Start a blockchain node with the given ID"""
    if setup == False:
        setup_system()
    if node_id in node_processes:
        return False  # Already running
        
    cmd = f"cd ../Service && mvn exec:java -Dexec.args='{node_id} {config_file}'"
    process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    node_processes[node_id] = process
    
    # Update database
    node, created = Node.objects.get_or_create(node_id=node_id)
    node.status = 'running'
    node.save()
    
    # Start thread to collect output
    def collect_output():
        while process.poll() is None:
            line = process.stdout.readline()
            if line:
                ProcessLog.objects.create(
                    process_type='node',
                    process_id=node_id,
                    message=line.strip()
                )
    
    threading.Thread(target=collect_output, daemon=True).start()
    return True

def start_client(client_id):
    """Start a client with the given ID"""
    if setup == False:
        setup_system()
    if client_id in client_processes:
        return False  # Already running
        
    cmd = f"cd ../Client && mvn exec:java -Dexec.args='{client_id}'"
    process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    client_processes[client_id] = process
    
    # Update database
    client, created = Client.objects.get_or_create(client_id=client_id)
    client.status = 'running'
    client.save()
    
    # Start thread to collect output
    def collect_output():
        while process.poll() is None:
            line = process.stdout.readline()
            if line:
                ProcessLog.objects.create(
                    process_type='client',
                    process_id=client_id,
                    message=line.strip()
                )
    
    threading.Thread(target=collect_output, daemon=True).start()
    return True

def setup_system():
    """Compile the Java projects"""
    global setup
    print("Setting up system...")
    # Use subprocess instead of os.system for better control and output capture
    try:
        # Navigate to the correct directory first
        subprocess.run("cd .. && mvn clean compile", shell=True, check=True)
        subprocess.run("cd .. && mvn clean install", shell=True, check=True)
        setup = True
        print("System setup completed successfully")
    except subprocess.CalledProcessError as e:
        print(f"Error during system setup: {e}")
        return False
    return True

def stop_node(node_id):
    """Stop a node with the given ID"""
    if node_id in node_processes:
        node_processes[node_id].terminate()
        del node_processes[node_id]
        
        node = Node.objects.get(node_id=node_id)
        node.status = 'stopped'
        node.save()
        return True
    return False

def stop_client(client_id):
    """Stop a client with the given ID"""
    if client_id in client_processes:
        client_processes[client_id].terminate()
        del client_processes[client_id]
        
        client = Client.objects.get(client_id=client_id)
        client.status = 'stopped'
        client.save()
        return True
    return False

class DashboardView(View):
    def get(self, request):
        nodes = Node.objects.all()
        clients = Client.objects.all()
        
        # Load config files to get available nodes/clients
        try:
            with open(SERVER_CONFIG_PATH) as f:
                server_config = json.load(f)
            
            with open(CLIENT_CONFIG_PATH) as f:
                client_config = json.load(f)
                
            # Create any nodes/clients that exist in config but not in DB
            for node in server_config:
                Node.objects.get_or_create(node_id=node['id'])
                
            for client in client_config:
                Client.objects.get_or_create(client_id=client['id'])
        except:
            # Handle missing config files
            pass
        
        return render(request, 'node_manager/dashboard.html', {
            'nodes': nodes,
            'clients': clients
        })

class StartNodeView(View):
    def get(self, request, node_id):
        start_node(node_id)
        return redirect('dashboard')

class StopNodeView(View):
    def get(self, request, node_id):
        stop_node(node_id)
        return redirect('dashboard')

class StartClientView(View):
    def get(self, request, client_id):
        start_client(client_id)
        return redirect('dashboard')

class StopClientView(View):
    def get(self, request, client_id):
        stop_client(client_id)
        return redirect('dashboard')

class LogsView(View):
    def get(self, request, process_type, process_id):
        logs = ProcessLog.objects.filter(
            process_type=process_type,
            process_id=process_id
        ).order_by('timestamp')[:100]  # Last 100 logs
        
        return JsonResponse({
            'logs': [log.message for log in logs]
        })

class StartAllView(View):
    def get(self, request):
        # Start all nodes
        with open(SERVER_CONFIG_PATH) as f:
            server_config = json.load(f)
            for node in server_config:
                start_node(node['id'])
        
        # Start all clients
        with open(CLIENT_CONFIG_PATH) as f:
            client_config = json.load(f)
            for client in client_config:
                start_client(client['id'])
                
        return redirect('dashboard')

class StopAllView(View):
    def get(self, request):
        # Stop all nodes
        for node_id in list(node_processes.keys()):
            stop_node(node_id)
            
        # Stop all clients
        for client_id in list(client_processes.keys()):
            stop_client(client_id)
            
        return redirect('dashboard')