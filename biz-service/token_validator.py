"""
JWT Token Validator for Biz Service

Supports two validation modes:
1. Local JWT validation (default)
2. Redis-based validation (optional, for real-time revocation)
"""
import jwt
import hashlib
from functools import wraps
from flask import request, jsonify

import config

# Optional Redis support
try:
    import redis
    import json
    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False


class TokenValidator:
    """JWT Token Validator with optional Redis support"""

    def __init__(self):
        self.secret_key = config.JWT_SECRET_KEY
        self.algorithm = config.JWT_ALGORITHM
        self.issuer = config.JWT_ISSUER
        self.audience = config.JWT_AUDIENCE
        self.redis_client = None

        # Initialize Redis if enabled
        if config.USE_REDIS and REDIS_AVAILABLE:
            try:
                self.redis_client = redis.Redis(
                    host=config.REDIS_HOST,
                    port=config.REDIS_PORT,
                    db=config.REDIS_DB,
                    decode_responses=True
                )
                self.redis_client.ping()
                print(f"[TokenValidator] Redis connected: {config.REDIS_HOST}:{config.REDIS_PORT}")
            except redis.ConnectionError:
                print("[TokenValidator] Redis connection failed, falling back to local validation")
                self.redis_client = None

    def _generate_token_key(self, token):
        """Generate Redis key from token hash"""
        token_hash = hashlib.sha256(token.encode()).hexdigest()[:32]
        return f"token:{token_hash}"

    def validate(self, token):
        """
        Validate JWT token

        Returns:
            dict: Token payload if valid
            None: If token is invalid
        """
        # Step 1: Validate JWT signature and claims
        try:
            payload = jwt.decode(
                token,
                self.secret_key,
                algorithms=[self.algorithm],
                audience=self.audience,
                issuer=self.issuer
            )
        except jwt.ExpiredSignatureError:
            return None, "token_expired"
        except jwt.InvalidAudienceError:
            return None, "invalid_audience"
        except jwt.InvalidIssuerError:
            return None, "invalid_issuer"
        except jwt.InvalidTokenError as e:
            return None, f"invalid_token: {str(e)}"

        # Step 2: Check Redis for revocation (if enabled)
        if self.redis_client:
            token_key = self._generate_token_key(token)
            token_data = self.redis_client.get(token_key)

            if not token_data:
                return None, "token_not_found_in_redis"

            data = json.loads(token_data)
            if not data.get("active", False):
                return None, "token_revoked"

        return payload, None

    def get_token_info(self, token):
        """Get token information without full validation (for debugging)"""
        try:
            # Decode without verification
            payload = jwt.decode(
                token,
                options={"verify_signature": False}
            )
            return payload
        except jwt.InvalidTokenError:
            return None


# Global validator instance
validator = TokenValidator()


def extract_bearer_token():
    """Extract Bearer token from Authorization header"""
    auth_header = request.headers.get('Authorization', '')
    if auth_header.startswith('Bearer '):
        return auth_header[7:]
    return None


def require_token(f):
    """
    Decorator to require valid JWT token

    Usage:
        @app.route('/api/resource')
        @require_token
        def protected_resource():
            # Access token info via request.token_info
            agent_id = request.token_info.get('sub')
            ...
    """
    @wraps(f)
    def decorated(*args, **kwargs):
        token = extract_bearer_token()

        if not token:
            return jsonify({
                "error": "missing_token",
                "error_description": "Authorization header with Bearer token required"
            }), 401

        payload, error = validator.validate(token)

        if error:
            return jsonify({
                "error": "invalid_token",
                "error_description": error
            }), 401

        # Attach token info to request
        request.token_info = payload

        return f(*args, **kwargs)

    return decorated


def require_scope(*required_scopes):
    """
    Decorator to require specific scope(s)

    Usage:
        @app.route('/api/commands')
        @require_token
        @require_scope('agent:commands')
        def get_commands():
            ...
    """
    def decorator(f):
        @wraps(f)
        def decorated(*args, **kwargs):
            token_scope = request.token_info.get('scope', '')

            for scope in required_scopes:
                if scope not in token_scope:
                    return jsonify({
                        "error": "insufficient_scope",
                        "error_description": f"Token does not have required scope: {scope}"
                    }), 403

            return f(*args, **kwargs)
        return decorated
    return decorator
