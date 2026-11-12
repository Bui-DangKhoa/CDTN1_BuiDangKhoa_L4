# Security Checklist — Smart CRM L4

Status reflects source inspection available on 12 November 2026. A status of “CHƯA LÀM” means there is no complete verification evidence; it is not a claim that the application has no protection.

| # | Area | Status | Evidence and limitation |
|---:|---|---|---|
| 1 | Secrets in repository and Git history | CHƯA LÀM | `.env` is ignored and no `.env` path appears in tracked history. A basic scan of current text and reachable Git history found no matches for common AWS/GitHub/private-key token patterns. The tracked `.env.example` contains sample fallback values. `before-tc12.sql` is a local untracked backup and excluded from the submission. No dedicated credential-history scanner is installed, so a complete secret scan is not certified. |
| 2 | Input validation | ĐÃ LÀM | Jakarta validation annotations are present on request DTOs, including required ticket/technician IDs, required future appointment time, and required ticket fields. This does not establish exhaustive validation for every field or route. |
| 3 | Ownership and authorization | ĐÃ LÀM | REST controllers require a session and role; ticket status updates compare the logged-in technician with the ticket owner. TC-11 records HTTP 403 `FORBIDDEN_NOT_OWNER` and unchanged MySQL state. |
| 4 | Password and PII protection | ĐÃ LÀM | `AuthService` verifies passwords with BCrypt; `DatabaseSeeder` hashes sample passwords; `MaskingUtil` and DTO tests cover phone masking. Evidence is implementation/test-level and does not establish a full privacy assessment. |
| 5 | Safe errors | ĐÃ LÀM | `GlobalExceptionHandler` centralizes API error responses and application tests cover error handling. No live review of every failure route was possible in this run. |
| 6 | Safe logging | CHƯA LÀM | A masking utility and logging tests exist, but controllers log request DTOs and lookup keywords. Full route-by-route log review and runtime redaction verification remain incomplete. |
| 7 | Dependency pinning and basic vulnerability checks | CHƯA LÀM | Direct Maven dependencies have explicit versions and the Boot parent is pinned; container image tags use floating minor tags. No vulnerability scanner result is available, so dependency risk checks are incomplete. |

No production-readiness claim is made. The checklist must be revisited when the missing history and dependency scans can be run.
