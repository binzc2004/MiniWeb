
import json
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8080"


def request(method, path, body=None):
    url = BASE_URL + path
    headers = {}

    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(
        url,
        data=data,
        headers=headers,
        method=method
    )

    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            return response.status, response.read().decode("utf-8")

    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")

    except Exception as e:
        return None, str(e)


def test(name, method, path, body=None, expected_status=200):
    status, response = request(method, path, body)

    passed = status == expected_status

    print(
        f"[{'PASS' if passed else 'FAIL'}] "
        f"{name:<25} "
        f"Expected={expected_status}, Actual={status}"
    )

    if not passed:
        print(f"       Response: {response}")

    return passed


def main():
    tests = [
        ("GET hello", "GET", "/hello", None, 200),
        ("GET user path", "GET", "/user/100", None, 200),
        ("GET search query", "GET", "/search?name=Tom", None, 200),
        ("GET combined params", "GET",
         "/user/detail/100?name=Tom", None, 200),

        ("POST JSON", "POST", "/user",
         {"id": 100, "name": "Tom"}, 200),

        ("POST echo", "POST", "/echo",
         {"message": "Hello MiniWeb"}, 200),

        ("GET not found", "GET", "/not-found", None, 404),
        ("POST method not allowed", "POST", "/hello", None, 405),
        ("GET invalid path variable", "GET", "/user/abc", None, 400),
        ("GET missing query parameter", "GET", "/search", None, 400),
    ]

    passed = 0

    print(f"\nTesting MiniWeb: {BASE_URL}\n")

    for name, method, path, body, expected in tests:
        if test(name, method, path, body, expected):
            passed += 1

    print("\n" + "=" * 55)
    print(f"Result: {passed}/{len(tests)} passed")
    print("=" * 55)


if __name__ == "__main__":
    main()
