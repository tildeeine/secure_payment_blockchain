from django.db import models

# Create your models here.
# Each model is a table in the database, created automatically from the class definition
# Each field is a column in the table
class Node(models.Model):
    node_id = models.CharField(max_length=50, unique=True)
    status = models.CharField(max_length=20, default='stopped')
    last_seen = models.DateTimeField(auto_now=True)
    
    def __str__(self):
        return f"Node {self.node_id} ({self.status})"

class Client(models.Model):
    client_id = models.CharField(max_length=50, unique=True)
    status = models.CharField(max_length=20, default='stopped')
    last_seen = models.DateTimeField(auto_now=True)
    
    def __str__(self):
        return f"Client {self.client_id} ({self.status})"

class ProcessLog(models.Model):
    process_type = models.CharField(max_length=10)  # 'node' or 'client'
    process_id = models.CharField(max_length=50)
    timestamp = models.DateTimeField(auto_now_add=True)
    message = models.TextField()
    
    class Meta:
        ordering = ['-timestamp']