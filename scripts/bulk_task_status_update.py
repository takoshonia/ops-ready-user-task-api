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
    parser = argparse.ArgumentParser(description="Bulk update task statuses safely via API.")
    parser.add_argument("--from-status", required=True, help="Current status filter")
    parser.add_argument("--to-status", required=True, help="New status value")
    parser.add_argument("--user-id", type=int, help="Optional user filter")
    parser.add_argument("--base-url", default="http://localhost:8081", help="API base URL")
    parser.add_argument("--dry-run", action="store_true", help="Preview changes without applying")
    parser.add_argument("--yes", action="store_true", help="Skip confirmation prompt")
    args = parser.parse_args()

    list_url = f"{args.base_url}/api/tasks"
    status, tasks = http_request("GET", list_url)
    if status != 200:
        print(f"Failed to fetch tasks. status={status} body={json.dumps(tasks)}")
        sys.exit(1)

    matching = [
        task for task in tasks
        if task["status"] == args.from_status and (args.user_id is None or task["userId"] == args.user_id)
    ]

    print(f"Found {len(matching)} tasks to update from {args.from_status} -> {args.to_status}")
    for task in matching:
        print(f"- taskId={task['id']} title={task['title']} userId={task['userId']}")

    if not matching:
        print("No matching tasks. Nothing to do.")
        return

    if args.dry_run:
        print("Dry-run mode: no updates executed.")
        return

    if not args.yes:
        confirm = input("Apply these updates? Type 'yes' to continue: ").strip().lower()
        if confirm != "yes":
            print("Aborted by user.")
            return

    updated_count = 0
    failed_count = 0
    for task in matching:
        update_url = f"{args.base_url}/api/tasks/{task['id']}"
        payload = {
            "title": task["title"],
            "description": task["description"],
            "status": args.to_status,
            "userId": task["userId"],
        }
        status, response = http_request("PUT", update_url, payload)
        if status == 200:
            updated_count += 1
        else:
            failed_count += 1
            print(f"Failed taskId={task['id']}. status={status} body={json.dumps(response)}")

    print(f"Done. updated={updated_count} failed={failed_count}")
    if failed_count > 0:
        sys.exit(1)


if __name__ == "__main__":
    main()
