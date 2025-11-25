#!/usr/bin/env python3
"""
OAuth2-compliant Mock Authorization Server
Supports both refresh_token and client_credentials grant types with mTLS
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
ISSUER = "https://auth.mwagent.example.com"
AUDIENCE = "https://api.mwagent.example.com"

# Mock database for agents and tokens
AGENTS_DB = {
    "agent-test001": {
        "agent_id": "agent-test001",
        "refresh_token": "refresh-token-test001",
        "status": "active",
        "scope": "agent:commands agent:results"
    },
    "agent-test002": {
        "agent_id": "agent-test002",
        "refresh_token": "refresh-token-test002",
        "status": "active",
        "scope": "agent:commands agent:results"
    }
}

def generate_access_token(agent_id, scope="agent:commands", method="refresh_token"):
    """Generate OAuth2-compliant JWT access token"""
    payload = {
        # OAuth2 standard claims
        "sub": agent_id,                    # Subject (agent ID)
        "iss": ISSUER,                      # Issuer
        "aud": AUDIENCE,                    # Audience
        "exp": datetime.datetime.utcnow() + datetime.timedelta(minutes=TOKEN_EXPIRY_MINUTES),
        "iat": datetime.datetime.utcnow(),  # Issued at
        "scope": scope,                     # OAuth2 scope

        # Custom claims
        "client_auth_method": method,       # Authentication method used
        "token_type": "access_token"
    }
    return jwt.encode(payload, SECRET_KEY, algorithm="HS256")

def verify_access_token(token):
    """Verify JWT access token"""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256"], audience=AUDIENCE)
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
            return jsonify({
                "error": "invalid_token",
                "error_description": "No token provided"
            }), 401

        payload = verify_access_token(token)
        if not payload:
            return jsonify({
                "error": "invalid_token",
                "error_description": "Token is invalid or expired"
            }), 401

        request.agent_id = payload['sub']
        request.token_scope = payload.get('scope', '')
        return f(*args, **kwargs)

    return decorated_function


# ==================== OAuth2 Token Endpoint (RFC 6749) ====================

@app.route('/oauth2/token', methods=['POST'])
def oauth2_token():
    """
    OAuth2 Token Endpoint (RFC 6749 Section 3.2)

    Supported grant types:
    - refresh_token: Refresh access token using refresh token
    - client_credentials: Issue access token using mTLS client certificate
    """

    # Get grant_type from form data
    grant_type = request.form.get('grant_type')

    if not grant_type:
        return jsonify({
            "error": "invalid_request",
            "error_description": "grant_type parameter is required"
        }), 400

    # Handle different grant types
    if grant_type == "refresh_token":
        return handle_refresh_token_grant()

    elif grant_type == "client_credentials":
        return handle_client_credentials_grant()

    else:
        return jsonify({
            "error": "unsupported_grant_type",
            "error_description": f"Grant type '{grant_type}' is not supported"
        }), 400


def handle_refresh_token_grant():
    """Handle OAuth2 Refresh Token Grant (RFC 6749 Section 6)"""

    refresh_token = request.form.get('refresh_token')

    if not refresh_token:
        return jsonify({
            "error": "invalid_request",
            "error_description": "refresh_token parameter is required"
        }), 400

    # Find agent by refresh token
    agent = None
    for agent_data in AGENTS_DB.values():
        if agent_data['refresh_token'] == refresh_token:
            agent = agent_data
            break

    if not agent:
        return jsonify({
            "error": "invalid_grant",
            "error_description": "Invalid refresh token"
        }), 401

    # Generate new access token
    access_token = generate_access_token(
        agent['agent_id'],
        scope=agent['scope'],
        method="refresh_token"
    )

    print(f"[OAuth2] refresh_token grant: Access token issued for {agent['agent_id']}")

    # OAuth2 standard token response
    return jsonify({
        "access_token": access_token,
        "token_type": "Bearer",
        "expires_in": TOKEN_EXPIRY_MINUTES * 60,
        "scope": agent['scope']
    }), 200


def handle_client_credentials_grant():
    """Handle OAuth2 Client Credentials Grant with mTLS (RFC 8705)"""

    # Extract client certificate from mTLS connection
    client_cert_dn = request.environ.get('SSL_CLIENT_S_DN')

    if not client_cert_dn:
        return jsonify({
            "error": "invalid_client",
            "error_description": "Client certificate required for mTLS authentication"
        }), 401

    # Extract agent ID from certificate CN
    agent_id = extract_agent_id_from_cn(client_cert_dn)

    if not agent_id:
        return jsonify({
            "error": "invalid_client",
            "error_description": "Cannot extract client ID from certificate CN"
        }), 401

    # Verify agent exists and is active
    agent = AGENTS_DB.get(agent_id)
    if not agent:
        return jsonify({
            "error": "invalid_client",
            "error_description": f"Client '{agent_id}' not registered"
        }), 401

    if agent['status'] != 'active':
        return jsonify({
            "error": "invalid_client",
            "error_description": f"Client '{agent_id}' is inactive"
        }), 401

    # Generate access token
    access_token = generate_access_token(
        agent_id,
        scope=agent['scope'],
        method="client_credentials_mtls"
    )

    print(f"[OAuth2] client_credentials grant (mTLS): Access token issued for {agent_id}")

    # OAuth2 standard token response
    return jsonify({
        "access_token": access_token,
        "token_type": "Bearer",
        "expires_in": TOKEN_EXPIRY_MINUTES * 60,
        "scope": agent['scope']
    }), 200


# ==================== Legacy Endpoints (Backward Compatibility) ====================

@app.route('/api/v1/security/refresh', methods=['POST'])
def legacy_refresh_token():
    """
    DEPRECATED: Legacy refresh token endpoint
    Use /oauth2/token with grant_type=refresh_token instead
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
    access_token = generate_access_token(agent['agent_id'], agent['scope'], "refresh_token_legacy")

    print(f"[LEGACY] refresh_token: Access token renewed for agent: {agent['agent_id']}")

    return jsonify({
        "access_token": access_token,
        "expires_in": TOKEN_EXPIRY_MINUTES * 60,
        "token_type": "Bearer"
    }), 200


@app.route('/api/v1/security/token/renew', methods=['POST'])
def legacy_renew_token_mtls():
    """
    DEPRECATED: Legacy mTLS token renewal endpoint
    Use /oauth2/token with grant_type=client_credentials instead
    """
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
    access_token = generate_access_token(agent_id, agent['scope'], "mtls_legacy")

    print(f"[LEGACY] mTLS: Access token renewed for agent: {agent_id}")

    return jsonify({
        "access_token": access_token,
        "expires_in": TOKEN_EXPIRY_MINUTES * 60,
        "token_type": "Bearer",
        "method": "mTLS"
    }), 200


@app.route('/api/v1/agent/getRefreshToken/<agent_id>', methods=['GET'])
@require_auth
def get_refresh_token(agent_id):
    """Get refresh token for agent (for initial setup)"""
    if agent_id not in AGENTS_DB:
        return jsonify({"error": "Agent not found"}), 404

    agent = AGENTS_DB[agent_id]

    print(f"[API] Refresh token retrieved for agent: {agent_id}")

    return jsonify({
        "refresh_token": agent['refresh_token'],
        "agent_id": agent_id
    }), 200


# ==================== Resource Server Endpoints ====================

@app.route('/api/v1/agent/test', methods=['GET'])
@require_auth
def test_endpoint():
    """Test endpoint to verify access token"""
    return jsonify({
        "message": "Access token is valid",
        "agent_id": request.agent_id,
        "scope": request.token_scope
    }), 200


@app.route('/api/v1/commands/<agent_id>', methods=['GET'])
@require_auth
def get_commands(agent_id):
    """Mock command polling endpoint"""

    # Check if agent_id matches token subject
    if request.agent_id != agent_id:
        return jsonify({
            "error": "forbidden",
            "error_description": "Token subject does not match requested agent ID"
        }), 403

    # Check scope
    if "agent:commands" not in request.token_scope:
        return jsonify({
            "error": "insufficient_scope",
            "error_description": "Token does not have 'agent:commands' scope"
        }), 403

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

    print("=" * 70)
    print("OAuth2-Compliant Mock Authorization Server")
    print("=" * 70)
    print("\n📋 OAuth2 Token Endpoint (RFC 6749):")
    print("   POST /oauth2/token")
    print("     - grant_type=refresh_token (Refresh Token Grant)")
    print("     - grant_type=client_credentials (Client Credentials + mTLS)")
    print("\n🔒 Supported Grant Types:")
    print("   1. refresh_token: Use refresh token to get new access token")
    print("   2. client_credentials: Use mTLS client certificate")
    print("\n⚠️  Legacy Endpoints (Backward Compatibility):")
    print("   - POST /api/v1/security/refresh (deprecated)")
    print("   - POST /api/v1/security/token/renew (deprecated)")
    print("\n👥 Registered Agents:")
    for agent_id, agent in AGENTS_DB.items():
        print(f"   - {agent_id}")
        print(f"     refresh_token: {agent['refresh_token']}")
        print(f"     scope: {agent['scope']}")
    print("=" * 70)

    # Check if SSL certificates exist
    use_ssl = len(sys.argv) > 1 and sys.argv[1] == '--ssl'

    if use_ssl:
        try:
            ssl_context = create_ssl_context()
            print("\n[INFO] Starting OAuth2 server with mTLS on https://0.0.0.0:8443")
            print("[INFO] Client certificates will be verified\n")
            app.run(host='0.0.0.0', port=8443, ssl_context=ssl_context, debug=True)
        except FileNotFoundError as e:
            print(f"\n[ERROR] SSL certificates not found: {e}")
            print("[INFO] Run './generate-certs.sh' to create certificates")
            print("[INFO] Or start without SSL: python mock_server.py\n")
            sys.exit(1)
    else:
        print("\n[INFO] Starting OAuth2 server WITHOUT SSL on http://0.0.0.0:8080")
        print("[INFO] mTLS grant will not work without SSL")
        print("[INFO] To enable mTLS, run: python mock_server.py --ssl\n")
        app.run(host='0.0.0.0', port=8080, debug=True)
