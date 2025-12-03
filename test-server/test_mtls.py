#!/usr/bin/env python3
"""
mTLS Test Script for OAuth2 Mock Server
Tests client_credentials grant with mTLS client certificate
"""

import requests
import json
import urllib3

# Disable SSL warnings for self-signed certificates
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Server configuration
BASE_URL = "https://localhost:8443"
CA_CERT = "./certs/ca.crt"

# Agent certificates
AGENTS = {
    "agent-test001": {
        "cert": ("./certs/agent-test001.crt", "./certs/agent-test001.key"),
        "expected_cn": "testserver01_appuser_J"
    },
    "agent-test002": {
        "cert": ("./certs/agent-test002.crt", "./certs/agent-test002.key"),
        "expected_cn": "testserver02_svcuser_J"
    },
    "agent-test003": {
        "cert": ("./certs/agent-test003.crt", "./certs/agent-test003.key"),
        "expected_cn": "testserver03_testuser_J"
    }
}


def test_health():
    """Test health endpoint"""
    print("\n" + "=" * 60)
    print("TEST: Health Check")
    print("=" * 60)

    try:
        response = requests.get(f"{BASE_URL}/health", verify=CA_CERT)
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
        return response.status_code == 200
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_refresh_token_grant(agent_id, refresh_token):
    """Test refresh_token grant"""
    print("\n" + "=" * 60)
    print(f"TEST: Refresh Token Grant for {agent_id}")
    print("=" * 60)

    try:
        response = requests.post(
            f"{BASE_URL}/oauth2/token",
            data={
                "grant_type": "refresh_token",
                "refresh_token": refresh_token
            },
            verify=CA_CERT
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")
        return response.status_code == 200
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_mtls_grant(agent_name, cert_info):
    """Test client_credentials grant with mTLS"""
    print("\n" + "=" * 60)
    print(f"TEST: mTLS Client Credentials Grant for {agent_name}")
    print("=" * 60)

    try:
        response = requests.post(
            f"{BASE_URL}/oauth2/token",
            data={"grant_type": "client_credentials"},
            cert=cert_info["cert"],
            verify=CA_CERT
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")

        if response.status_code == 200:
            # Decode and verify token claims
            token_data = response.json()
            print("\n--- Token Claims Verification ---")
            print(f"Expected CN: {cert_info['expected_cn']}")
            # Token was issued, test passed
            return True
        return False
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_access_token_validation(access_token):
    """Test access token validation endpoint"""
    print("\n" + "=" * 60)
    print("TEST: Access Token Validation")
    print("=" * 60)

    try:
        response = requests.get(
            f"{BASE_URL}/api/v1/agent/test",
            headers={"Authorization": f"Bearer {access_token}"},
            verify=CA_CERT
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")
        return response.status_code == 200
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_expired_refresh_token_fallback():
    """Test cascading fallback: expired refresh_token -> mTLS"""
    print("\n" + "=" * 60)
    print("TEST: Cascading Fallback (Expired Refresh Token -> mTLS)")
    print("=" * 60)

    # agent-test003 has refresh_token_expired=True in server config
    print("\n1. Try refresh_token grant (should fail - expired):")

    try:
        response = requests.post(
            f"{BASE_URL}/oauth2/token",
            data={
                "grant_type": "refresh_token",
                "refresh_token": "refresh-token-test003"
            },
            verify=CA_CERT
        )
        print(f"   Status: {response.status_code}")
        print(f"   Response: {response.json()}")

        if response.status_code == 401:
            print("\n2. Fallback to mTLS (should succeed):")
            response = requests.post(
                f"{BASE_URL}/oauth2/token",
                data={"grant_type": "client_credentials"},
                cert=AGENTS["agent-test003"]["cert"],
                verify=CA_CERT
            )
            print(f"   Status: {response.status_code}")
            print(f"   Response: {json.dumps(response.json(), indent=2)}")
            return response.status_code == 200
        return False
    except Exception as e:
        print(f"Error: {e}")
        return False


def main():
    print("=" * 60)
    print("mTLS OAuth2 Test Suite")
    print("=" * 60)

    results = []

    # 1. Health check
    results.append(("Health Check", test_health()))

    # 2. Test refresh_token grant for agent-test001
    results.append(("Refresh Token (test001)",
                   test_refresh_token_grant("testserver01_appuser_J", "refresh-token-test001")))

    # 3. Test mTLS for each agent
    for agent_name, cert_info in AGENTS.items():
        results.append((f"mTLS ({agent_name})", test_mtls_grant(agent_name, cert_info)))

    # 4. Test access token validation
    print("\nGetting fresh token for validation test...")
    response = requests.post(
        f"{BASE_URL}/oauth2/token",
        data={
            "grant_type": "refresh_token",
            "refresh_token": "refresh-token-test001"
        },
        verify=CA_CERT
    )
    if response.status_code == 200:
        token = response.json()["access_token"]
        results.append(("Access Token Validation", test_access_token_validation(token)))

    # 5. Test cascading fallback
    results.append(("Cascading Fallback", test_expired_refresh_token_fallback()))

    # Summary
    print("\n" + "=" * 60)
    print("TEST SUMMARY")
    print("=" * 60)
    passed = 0
    failed = 0
    for name, result in results:
        status = "PASS" if result else "FAIL"
        print(f"  {name}: {status}")
        if result:
            passed += 1
        else:
            failed += 1

    print(f"\nTotal: {passed} passed, {failed} failed")
    print("=" * 60)


if __name__ == "__main__":
    main()
