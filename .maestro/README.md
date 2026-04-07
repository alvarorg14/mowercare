# Maestro E2E flows

Flows are driven by [Maestro](https://maestro.mobile.dev/). Install the CLI:

```bash
curl -Ls "https://get.maestro.mobile.dev" | bash
```

See **[`docs/testing-e2e.md`](../docs/testing-e2e.md)** for prerequisites, seeding the API, environment variables, and running tests.

| File | Purpose |
|------|---------|
| `seed.sh` / `seed.py` | Bootstrap org (if needed), login, create **E2E Smoke Issue** |
| `issues_smoke.yaml` | Sign-in → issues list → issue detail |
| `notifications_smoke.yaml` | Sign-in → notifications tab |
