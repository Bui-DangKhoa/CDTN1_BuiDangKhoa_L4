# BT3 v1.0 – Smart CRM L4

Draft release notes for the L4 technician assignment and appointment scheduling submission.

## Included

- The BT3 source package, tests, Maven Wrapper, Docker Compose setup, schema, sample seeds, and supporting documentation.
- The three requested bug records: actor user ID, appointment date in the past, and time-zone consistency.
- The latest recorded full test run: 108 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in an isolated builder container.
- Fresh isolated deployment check: 11 tickets, 4 technicians, 2 users; app and MySQL healthy.
- Schema re-imported twice and both seed files re-imported once in the isolated database; every operation exited successfully and counts stayed 11/4/2.

## Start

After clone and `cd` into the repository root in PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Preserve an existing `.env`; sample values are development defaults. Do not expose them as production credentials.

## Verification limits

- The Windows Maven Wrapper invocation failed in this environment before test execution because of Maven cache path/access errors. The successful 108-test run used a disposable Docker builder on the same host with cached dependencies and network access for the Surefire JUnit provider.
- The Docker image build used cache; this is not a no-cache build on a clean machine.
- API examples and OpenAPI were captured from the isolated application. Example PII fields are redacted. TC-09 observed the UI rejection and database invariants, not HTTP 409.
- Security review did not complete historical-secret or vulnerability scanning. The checklist records this limitation.
- This package does not claim production readiness.

## Submission status

Draft only. The BT3 report source DOCX/PDF was not present in the project and no office/PDF export tool was available. A fabricated PDF is intentionally not included. The GitHub release, tag and public assets have not been created.
