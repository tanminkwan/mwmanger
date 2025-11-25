#!/usr/bin/env python3
"""
Mock server for testing mTLS-based token renewal
Supports both refresh token and mTLS authentication methods
"""

from flask import Flask, request, jsonify
import jwt
import datetime
import ssl
import re
from functools import wraps

app = Flask(__name__)

# Configuration
SECRET_KEY = "test-secret-key-for-mock-server"
TOKEN_EXPIRY_MINUTES = 30

# Mock database for agents and tokens
AGENTS_DB = {
    "agent-test001": {
        "agent_id": "agent-test001",
        "refresh_token": "refresh-token-test001",
        "status": "active"
    },
    "agent-test002": {
        "agent_id": "agent-test002",
        "refresh_token": "refresh-token-test002",
        "status": "active"
    }
}

def generate_access_token(agent_id):
    """Generate JWT access token"""
    payload = {
        "agent_id": agent_id,
        "exp": datetime.datetime.utcnow() + datetime.timedelta(minutes=TOKEN_EXPIRY_MINUTES),
        "iat": datetime.datetime.utcnow()
    }
    return jwt.encode(payload, SECRET_KEY, algorithm="HS256")

def verify_access_token(token):
    """Verify JWT access token"""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256"])
        return payload
    except jwt.ExpiredSignatureError:
        return None
    except jwt.InvalidTokenError:
        return None

def extract_bearer_token(auth_header):
    """Extract token from Bearer authorization header"""
    if auth_header and auth_header.startswith("Bearer "):
        return auth_header[7:]
    return None

def extract_agent_id_from_cn(cert_dn):
    """Extract agent ID from certificate CN"""
    # DN format: "CN=agent-test001, OU=Agents, O=Company, C=KR"
    match = re.search(r'CN=([^,]+)', cert_dn)
    if match:
        return match.group(1)
    return None

def require_auth(f):
    """Decorator to require valid access token"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        token = extract_bearer_token(auth_header)

        if not token:
            return jsonify({"error": "No token provided"}), 401

        payload = verify_access_token(token)
        if not payload:
            return jsonify({"error": "Invalid or expired token"}), 401

        request.agent_id = payload['agent_id']
        return f(*args, **kwargs)

    return decorated_function


# ==================== Existing Endpoints (Refresh Token Method) ====================

@app.route('/api/v1/security/refresh', methods=['POST'])
def refresh_token():
    """
    Existing method: Refresh access token using refresh token
    """
    auth_header = request.headers.get('Authorization')
    refresh_token = extract_bearer_token(auth_header)

    if not refresh_token:
        return jsonify({"error": "No refresh token provided"}), 401

    # Find agent by refresh token
    agent = None
    for agent_data in AGENTS_DB.values():
        if agent_data['refresh_token'] == refresh_token:
            agent = agent_data
            break

    if not agent:
        return jsonify({"error": "Invalid refresh token"}), 401

    # Generate new access token
    access_token = generate_access_token(agent['agent_id'])

    print(f"[REFRESH TOKEN] Access token renewed for agent: {agent['agent_id']}")

    return jsonify({
        "access_token": access_token,
        "expires_in": TOKEN_EXPIRY_MINUTES * 60,
        "token_type": "Bearer"
    }), 200

@app.route('/api/v1/agent/getRefreshToken/<agent_id>', methods=['GET'])
@require_auth
def get_refresh_token(agent_id):
    """
    Existing method: Get refresh token for agent
    """
    if agent_id not in AGENTS_DB:
        return jsonify({"error": "Agent not found"}), 404

    agent = AGENTS_DB[agent_id]

    print(f"[GET REFRESH TOKEN] Refresh token retrieved for agent: {agent_id}")

    return jsonify({
        "refresh_token": agent['refresh_token'],
        "agent_id": agent_id
    }), 200


# ==================== New Endpoints (mTLS Method) ====================

@app.route('/api/v1/security/token/renew', methods=['POST'])
def renew_token_mtls():
    """
    New method: Renew access token using mTLS (client certificate)
    """
    # Extract client certificate from request
    # In production, this would be: request.environ.get('SSL_CLIENT_CERT')
    # For Flask testing, we check if the connection is using client cert

    client_cert_dn = request.environ.get('SSL_CLIENT_S_DN')

    if not client_cert_dn:
        return jsonify({
            "error": "Client certificate required",
            "message": "mTLS authentication failed - no client certificate"
        }), 401

    # Extract agent ID from certificate CN
    agent_id = extract_agent_id_from_cn(client_cert_dn)

    if not agent_id:
        return jsonify({
            "error": "Invalid certificate",
            "message": "Cannot extract agent ID from certificate CN"
        }), 403

    # Verify agent exists and is active
    agent = AGENTS_DB.get(agent_id)
    if not agent:
        return jsonify({
            "error": "Agent not found",
            "message": f"Agent {agent_id} not registered"
        }), 403

    if agent['status'] != 'active':
        return jsonify({
            "error": "Agent inactive",
            "message": f"Agent {agent_id} is not active"
        }), 403

    # Generate new access token
    access_token = generate_access_token(agent_id)

    print(f"[mTLS] Access token renewed for agent: {agent_id} (from certificate CN)")

    return jsonify({
        "access_token": access_token,
        "expires_in": TOKEN_EXPIRY_MINUTES * 60,
        "token_type": "Bearer",
        "method": "mTLS"
    }), 200


# ==================== Test Endpoints ====================

@app.route('/api/v1/agent/test', methods=['GET'])
@require_auth
def test_endpoint():
    """Test endpoint to verify access token"""
    return jsonify({
        "message": "Access token is valid",
        "agent_id": request.agent_id
    }), 200

@app.route('/api/v1/commands/<agent_id>', methods=['GET'])
@require_auth
def get_commands(agent_id):
    """Mock command polling endpoint"""
    return jsonify({
        "return_code": 1,
        "data": [],
        "agent_id": agent_id
    }), 200

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({"status": "ok"}), 200


# ==================== Server Configuration ====================

def create_ssl_context():
    """Create SSL context for mTLS"""
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)

    # Server certificate and key
    context.load_cert_chain(
        certfile='./certs/server.crt',
        keyfile='./certs/server.key'
    )

    # Trust store for client certificates
    context.load_verify_locations(cafile='./certs/ca.crt')

    # Require client certificate (mTLS)
    context.verify_mode = ssl.CERT_OPTIONAL  # OPTIONAL to support both methods

    return context


if __name__ == '__main__':
    import sys

    print("=" * 60)
    print("Mock Server for mTLS Token Renewal Testing")
    print("=" * 60)
    print("\nSupported authentication methods:")
    print("1. Refresh Token (existing)")
    print("   - POST /api/v1/security/refresh")
    print("   - GET  /api/v1/agent/getRefreshToken/<agent_id>")
    print("\n2. mTLS (new)")
    print("   - POST /api/v1/security/token/renew")
    print("\nRegistered agents:")
    for agent_id, agent in AGENTS_DB.items():
        print(f"   - {agent_id}: refresh_token={agent['refresh_token']}")
    print("=" * 60)

    # Check if SSL certificates exist
    use_ssl = len(sys.argv) > 1 and sys.argv[1] == '--ssl'

    if use_ssl:
        try:
            ssl_context = create_ssl_context()
            print("\n[INFO] Starting server with mTLS support on https://0.0.0.0:8443")
            print("[INFO] Client certificates will be verified\n")
            app.run(host='0.0.0.0', port=8443, ssl_context=ssl_context, debug=True)
        except FileNotFoundError as e:
            print(f"\n[ERROR] SSL certificates not found: {e}")
            print("[INFO] Run './generate-certs.sh' to create certificates")
            print("[INFO] Or start without SSL: python mock_server.py\n")
            sys.exit(1)
    else:
        print("\n[INFO] Starting server WITHOUT SSL on http://0.0.0.0:8080")
        print("[INFO] Only refresh token method will work")
        print("[INFO] To enable mTLS, run: python mock_server.py --ssl\n")
        app.run(host='0.0.0.0', port=8080, debug=True)
