#!/usr/bin/env python3
import argparse
import json
import sys
import urllib.error
import urllib.request
import uuid


def http_request(method: str, url: str, payload: dict | None = None):
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Request-Id": str(uuid.uuid4()),
        "Idempotency-Key": str(uuid.uuid4()),
    }
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            body = response.read().decode("utf-8") if response.readable() else ""
            return response.status, json.loads(body) if body else {}
    except urllib.error.HTTPError as ex:
        body = ex.read().decode("utf-8")
        parsed = json.loads(body) if body else {}
        return ex.code, parsed


def main():
    parser = argparse.ArgumentParser(description="Safely update a user's email via API.")
    parser.add_argument("--user-id", type=int, required=True, help="User ID to update")
    parser.add_argument("--new-email", required=True, help="New email value")
    parser.add_argument("--base-url", default="http://localhost:8081", help="API base URL")
    parser.add_argument("--dry-run", action="store_true", help="Preview changes without applying")
    parser.add_argument("--yes", action="store_true", help="Skip confirmation prompt")
    args = parser.parse_args()

    get_url = f"{args.base_url}/api/users/{args.user_id}"
    status, user = http_request("GET", get_url)
    if status != 200:
        print(f"Failed to fetch user. status={status} body={json.dumps(user)}")
        sys.exit(1)

    print(f"Current user: id={user['id']} name={user['name']} email={user['email']}")
    print(f"Planned email update: {user['email']} -> {args.new_email}")

    if args.dry_run:
        print("Dry-run mode: no update executed.")
        return

    if not args.yes:
        confirm = input("Apply this update? Type 'yes' to continue: ").strip().lower()
        if confirm != "yes":
            print("Aborted by user.")
            return

    payload = {
        "name": user["name"],
        "email": args.new_email,
    }
    put_url = f"{args.base_url}/api/users/{args.user_id}"
    status, updated = http_request("PUT", put_url, payload)
    if status != 200:
        print(f"Update failed. status={status} body={json.dumps(updated)}")
        sys.exit(1)

    print(f"Update successful. id={updated['id']} email={updated['email']}")


if __name__ == "__main__":
    main()
