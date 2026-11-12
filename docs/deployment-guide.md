# Deployment Guide — Smart CRM L4

## 1. Environment requirements

- Windows 10/11 with PowerShell, Git and Docker Desktop running with Linux containers.
- Docker Engine 29.7.2 and Docker Compose v5.5.0 (`docker compose`); both were read from the authoring host.
- Java 21 is required to run Maven Wrapper tests locally; the host used Java 21.0.11. The wrapper properties request Maven 3.6.3, but that Windows wrapper run did not complete in this environment. Docker build image `maven:3.9-eclipse-temurin-21-alpine` resolved Maven 3.9.16 and Java 21.0.12; runtime image is `eclipse-temurin:21-jre-alpine`. MySQL image tag is `mysql:8.0`.
- A free host port 8080 for the application and 3306 for MySQL by default. Existing `.env` values may override these ports.

## 2. Deployment steps

From the cloned repository root in PowerShell, run these two commands:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

The copy command creates `.env`; if one already exists, preserve it and configure the variables below instead of overwriting it. Compose starts MySQL, waits for its health check, then starts the application. On first initialization MySQL loads `docs/schema.sql`, `seeds/data-demo.sql` and `seeds/auth_data.sql` into the named volume. Starting the app again does not rerun those MySQL initialization scripts.

## 3. Environment variables

Compose reads these variables from `.env` or the process environment. Defaults below are development examples from the Compose file or `.env.example`; replace passwords and the JWT secret before exposing the service beyond a local development machine.

| Name | Purpose | Example | Required |
|---|---|---|---|
| `MYSQL_DATABASE` | Database created and used by the app | `smart_crm_db` | No; Compose default |
| `MYSQL_USER` | Application database account | `crm_user` | No; Compose default |
| `MYSQL_PASSWORD` | Application database password | `<set-a-local-password>` | No syntactically; use a private value |
| `MYSQL_ROOT_PASSWORD` | MySQL root password and health check | `<set-a-local-password>` | No syntactically; use a private value |
| `MYSQL_HOST` | Documented host setting; current Compose JDBC URL uses service name `db` directly | `db` | No; not consumed by Compose |
| `MYSQL_PORT` | Host port mapped to MySQL 3306 | `3306` | No; Compose default |
| `APP_PORT` | Application listen port and matching published host port | `8080` | No; keep `8080` with the current Compose mapping |
| `JWT_SECRET` | Application signing secret passed to the app | `<private-secret>` | No syntactically; Compose supplies a sample fallback |
| `LOG_LEVEL` | Application log-level setting passed by Compose | `INFO` | No; Compose default |
| `TZ` | Example timezone setting; Compose services currently set `Asia/Ho_Chi_Minh` directly | `Asia/Ho_Chi_Minh` | No; not used for service timezone substitution |

The default passwords and JWT value are sample values, not production credentials. Do not commit a populated `.env`. Current Compose maps `${APP_PORT}` to container port 8080 while the application also reads `APP_PORT` as its listen port; keep this value at 8080 unless the Compose port mapping is changed accordingly.

## 4. Sample data

The checked-in initialization scripts provide 11 tickets, 4 technicians and 2 user accounts in the verified fresh-volume run. A manager account and a technician account are included for local evaluation; obtain their demo credentials from the repository's sample-data documentation and do not reuse them outside the sample environment. The public lookup page does not require login. The MySQL initialization scripts execute only when the database volume is first created.

## 5. Verification

Run `docker compose ps` and wait until both `db` and `app` report `healthy`. Then open `http://localhost:8080/login`, sign in with a sample manager account, and confirm the unassigned-ticket list loads. The public portal is at `http://localhost:8080/lookup`; Swagger UI is at `http://localhost:8080/swagger-ui/index.html`; the generated OpenAPI document is at `http://localhost:8080/v3/api-docs`.

Expected fresh-volume sample counts are 11 tickets, 4 technicians and 2 users; the manager view was observed to show 2 unassigned tickets. These values describe the checked-in sample fixture, not production readiness. Do not remove an existing project volume to obtain a fresh database.
