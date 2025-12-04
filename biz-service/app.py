"""
Biz Service - Sample Business Service API

This service demonstrates how external services can validate
JWT tokens issued by the Auth Server.

Endpoints:
    GET  /api/commands     - Get pending commands for agent
    POST /api/results      - Submit command execution results
    GET  /api/config       - Get agent configuration
    GET  /health           - Health check (no auth required)
    GET  /api/token-info   - Debug: Show token information
"""
from flask import Flask, jsonify, request
from datetime import datetime

import config
from token_validator import require_token, require_scope

app = Flask(__name__)


# =============================================================================
# Health Check (No Authentication)
# =============================================================================

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy",
        "service": "biz-service",
        "timestamp": datetime.utcnow().isoformat()
    })


# =============================================================================
# Protected API Endpoints
# =============================================================================

@app.route('/api/commands', methods=['GET'])
@require_token
@require_scope('agent:commands')
def get_commands():
    """
    Get pending commands for the agent

    Required scope: agent:commands
    """
    agent_id = request.token_info.get('sub')
    hostname = request.token_info.get('hostname')
    username = request.token_info.get('username')

    # Sample commands (in real implementation, fetch from database)
    commands = [
        {
            "command_id": "cmd-001",
            "type": "execute",
            "payload": {
                "script": "echo 'Hello from Biz Service'",
                "timeout": 30
            },
            "created_at": datetime.utcnow().isoformat()
        },
        {
            "command_id": "cmd-002",
            "type": "collect_metrics",
            "payload": {
                "metrics": ["cpu", "memory", "disk"]
            },
            "created_at": datetime.utcnow().isoformat()
        }
    ]

    return jsonify({
        "agent_id": agent_id,
        "hostname": hostname,
        "username": username,
        "commands": commands,
        "count": len(commands)
    })


@app.route('/api/results', methods=['POST'])
@require_token
@require_scope('agent:results')
def submit_results():
    """
    Submit command execution results

    Required scope: agent:results
    """
    agent_id = request.token_info.get('sub')
    hostname = request.token_info.get('hostname')

    # Get request body
    data = request.get_json()

    if not data:
        return jsonify({
            "error": "invalid_request",
            "error_description": "Request body is required"
        }), 400

    command_id = data.get('command_id')
    result = data.get('result')
    status = data.get('status', 'completed')

    if not command_id:
        return jsonify({
            "error": "invalid_request",
            "error_description": "command_id is required"
        }), 400

    # In real implementation, store result in database
    print(f"[Result] Agent: {agent_id}, Command: {command_id}, Status: {status}")

    return jsonify({
        "status": "accepted",
        "agent_id": agent_id,
        "command_id": command_id,
        "received_at": datetime.utcnow().isoformat()
    })


@app.route('/api/config', methods=['GET'])
@require_token
@require_scope('agent:commands')
def get_config():
    """
    Get agent configuration

    Required scope: agent:commands
    """
    agent_id = request.token_info.get('sub')
    hostname = request.token_info.get('hostname')

    # Sample configuration
    agent_config = {
        "polling_interval": 60,
        "heartbeat_interval": 30,
        "log_level": "INFO",
        "features": {
            "metrics_collection": True,
            "log_forwarding": True,
            "remote_execution": True
        }
    }

    return jsonify({
        "agent_id": agent_id,
        "hostname": hostname,
        "config": agent_config
    })


# =============================================================================
# Debug Endpoints
# =============================================================================

@app.route('/api/token-info', methods=['GET'])
@require_token
def get_token_info():
    """
    Debug endpoint: Show token information

    Returns all claims from the JWT token
    """
    return jsonify({
        "token_info": request.token_info
    })


@app.route('/api/whoami', methods=['GET'])
@require_token
def whoami():
    """
    Debug endpoint: Show agent identity from token
    """
    return jsonify({
        "agent_id": request.token_info.get('sub'),
        "hostname": request.token_info.get('hostname'),
        "username": request.token_info.get('username'),
        "usertype": request.token_info.get('usertype'),
        "scope": request.token_info.get('scope'),
        "client_ip": request.token_info.get('client_ip'),
        "token_type": request.token_info.get('token_type'),
        "auth_method": request.token_info.get('client_auth_method')
    })


# =============================================================================
# Error Handlers
# =============================================================================

@app.errorhandler(404)
def not_found(e):
    return jsonify({
        "error": "not_found",
        "error_description": "The requested resource was not found"
    }), 404


@app.errorhandler(500)
def internal_error(e):
    return jsonify({
        "error": "internal_error",
        "error_description": "An internal server error occurred"
    }), 500


# =============================================================================
# Main
# =============================================================================

if __name__ == '__main__':
    print("=" * 60)
    print("Biz Service - Sample Business Service API")
    print("=" * 60)
    print(f"Host: {config.HOST}")
    print(f"Port: {config.PORT}")
    print(f"Debug: {config.DEBUG}")
    print(f"Redis Enabled: {config.USE_REDIS}")
    print("=" * 60)
    print("Endpoints:")
    print("  GET  /health          - Health check (no auth)")
    print("  GET  /api/commands    - Get pending commands")
    print("  POST /api/results     - Submit execution results")
    print("  GET  /api/config      - Get agent configuration")
    print("  GET  /api/token-info  - Debug: Show token info")
    print("  GET  /api/whoami      - Debug: Show agent identity")
    print("=" * 60)

    app.run(
        host=config.HOST,
        port=config.PORT,
        debug=config.DEBUG
    )
