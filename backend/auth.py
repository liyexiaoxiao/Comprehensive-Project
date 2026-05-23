import os
from functools import wraps

import jwt
from flask import g, jsonify, request


DEFAULT_DEV_SECRET = (
    "this-is-a-secret-key-for-development-only-when-using-production-environment-"
    "secret-should-be-injected-via-environment-variable"
)


def _jwt_secret():
    return os.getenv("JWT_SECRET", DEFAULT_DEV_SECRET)


def _bearer_token():
    auth_header = request.headers.get("Authorization", "")
    if auth_header.startswith("Bearer "):
        return auth_header[7:].strip()
    return None


def _is_public_path(public_paths):
    if request.method == "OPTIONS":
        return True

    path = request.path
    for method, prefix in public_paths:
        if method != "*" and method != request.method:
            continue
        if path == prefix or path.startswith(prefix.rstrip("/") + "/"):
            return True
    return False


def verify_jwt_request():
    token = _bearer_token()
    if not token:
        return None, (jsonify({"error": "Missing bearer token"}), 401)

    try:
        claims = jwt.decode(token, _jwt_secret(), algorithms=["HS256"])
    except jwt.ExpiredSignatureError:
        return None, (jsonify({"error": "Token expired"}), 401)
    except jwt.InvalidTokenError:
        return None, (jsonify({"error": "Invalid token"}), 401)

    header_user_id = request.headers.get("X-User-Id")
    token_user_id = claims.get("userId")
    if header_user_id and token_user_id is not None and str(header_user_id) != str(token_user_id):
        return None, (jsonify({"error": "User identity mismatch"}), 403)

    return claims, None


def install_jwt_auth(app, public_paths=None):
    public_paths = public_paths or []

    @app.before_request
    def _authenticate_request():
        if _is_public_path(public_paths):
            return None

        claims, error_response = verify_jwt_request()
        if error_response:
            return error_response

        g.jwt_claims = claims
        return None


def current_user_id():
    claims = getattr(g, "jwt_claims", {}) or {}
    return claims.get("userId")


def require_jwt(view_func):
    @wraps(view_func)
    def wrapper(*args, **kwargs):
        claims, error_response = verify_jwt_request()
        if error_response:
            return error_response
        g.jwt_claims = claims
        return view_func(*args, **kwargs)

    return wrapper
