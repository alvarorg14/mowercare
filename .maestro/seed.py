#!/usr/bin/env python3
"""Seed API data for Maestro E2E flows (bootstrap, login, create issue)."""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from uuid import UUID


def req(
    method: str,
    url: str,
    *,
    headers: dict[str, str] | None = None,
    body: bytes | None = None,
) -> tuple[int | None, bytes]:
    r = urllib.request.Request(url, data=body, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            return resp.getcode(), resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()
    except urllib.error.URLError as e:
        print(f"error: cannot reach API ({url}): {e.reason}", file=sys.stderr)
        return None, b""


def parse_json_object(data: bytes, what: str) -> dict[str, object] | None:
    try:
        obj = json.loads(data.decode())
    except (json.JSONDecodeError, UnicodeDecodeError) as e:
        print(f"error: invalid JSON in {what}: {e}", file=sys.stderr)
        return None
    if not isinstance(obj, dict):
        print(f"error: expected JSON object in {what}", file=sys.stderr)
        return None
    return obj


def main() -> int:
    api = os.environ.get("E2E_API_URL", "http://localhost:8080").rstrip("/")
    token = os.environ.get("E2E_BOOTSTRAP_TOKEN", "")
    org_name = os.environ.get("E2E_ORG_NAME", "E2E Org")
    email = os.environ.get("E2E_ADMIN_EMAIL", "e2e-admin@example.local")
    password = os.environ.get("E2E_ADMIN_PASSWORD", "").strip()
    issue_title = os.environ.get("E2E_ISSUE_TITLE", "E2E Smoke Issue")
    org_id_env = os.environ.get("E2E_ORG_ID", "").strip()

    if not token:
        print("error: set E2E_BOOTSTRAP_TOKEN (same as MOWERCARE_BOOTSTRAP_TOKEN)", file=sys.stderr)
        return 1

    if not password:
        print("error: set E2E_ADMIN_PASSWORD (min 8 characters; never commit real values)", file=sys.stderr)
        return 1

    org_id: str | None = org_id_env or None

    if not org_id:
        bootstrap_body = json.dumps(
            {
                "organizationName": org_name,
                "adminEmail": email,
                "adminPassword": password,
            }
        ).encode()
        code, data = req(
            "POST",
            f"{api}/api/v1/bootstrap/organization",
            headers={
                "Content-Type": "application/json",
                "X-Bootstrap-Token": token,
            },
            body=bootstrap_body,
        )
        if code is None:
            return 1
        if code == 201:
            parsed = parse_json_object(data, "bootstrap response")
            if parsed is None:
                return 1
            raw_id = parsed.get("organizationId")
            if raw_id is None:
                print("error: bootstrap response missing organizationId", file=sys.stderr)
                return 1
            org_id = str(raw_id)
            print(f"bootstrap: created organization {org_id}", file=sys.stderr)
        elif code == 409:
            print(
                "error: bootstrap already completed (HTTP 409). "
                "Export E2E_ORG_ID=<uuid> and ensure E2E_ADMIN_EMAIL / E2E_ADMIN_PASSWORD "
                "match a user in that org, then re-run.",
                file=sys.stderr,
            )
            return 1
        else:
            print(f"error: bootstrap failed (HTTP {code})", file=sys.stderr)
            sys.stderr.buffer.write(data)
            print(file=sys.stderr)
            return 1
    else:
        print(f"bootstrap: using existing E2E_ORG_ID={org_id}", file=sys.stderr)

    assert org_id is not None
    try:
        UUID(org_id)
    except ValueError:
        print(f"error: E2E_ORG_ID is not a valid UUID: {org_id!r}", file=sys.stderr)
        return 1

    login_body = json.dumps(
        {
            "organizationId": org_id,
            "email": email,
            "password": password,
        }
    ).encode()
    code, data = req(
        "POST",
        f"{api}/api/v1/auth/login",
        headers={"Content-Type": "application/json"},
        body=login_body,
    )
    if code is None:
        return 1
    if code != 200:
        print(f"error: login failed (HTTP {code})", file=sys.stderr)
        sys.stderr.buffer.write(data)
        print(file=sys.stderr)
        return 1
    parsed_login = parse_json_object(data, "login response")
    if parsed_login is None:
        return 1
    access = parsed_login.get("accessToken")
    if not isinstance(access, str) or not access:
        print("error: login response missing accessToken", file=sys.stderr)
        return 1

    issue_body = json.dumps(
        {
            "title": issue_title,
            "description": "Created by .maestro/seed.py for UI smoke tests.",
            "status": "OPEN",
            "priority": "MEDIUM",
        }
    ).encode()
    code, data = req(
        "POST",
        f"{api}/api/v1/organizations/{org_id}/issues",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {access}",
        },
        body=issue_body,
    )
    if code is None:
        return 1
    if code != 201:
        print(f"error: create issue failed (HTTP {code})", file=sys.stderr)
        sys.stderr.buffer.write(data)
        print(file=sys.stderr)
        return 1

    print(f"seed: created issue {issue_title!r}", file=sys.stderr)
    print()
    print("# Paste into your shell before running Maestro (never commit secrets):")
    print(f'export E2E_API_URL="{api}"')
    print(f'export E2E_ORG_ID="{org_id}"')
    print(f'export E2E_ADMIN_EMAIL="{email}"')
    print(f'export E2E_ADMIN_PASSWORD="{password}"')
    print(f'export E2E_ISSUE_TITLE="{issue_title}"')
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
