# Security Policy

## Project status

PKI Web Platform is an academic project intended for learning and demonstration.
It is not designed or supported for production use, and its private Certificate
Authority is not accredited: signatures it produces have no legal value.
Known design limitations are listed under
[Security considerations](README.md#security-considerations) in the README.

## Supported versions

Only the latest commit on the `main` branch receives fixes.

## Reporting a vulnerability

Please report vulnerabilities privately, not through public issues:

1. Open the repository's **Security** tab.
2. Click **Report a vulnerability** and describe the issue, the affected
   component (backend, frontend, CA script) and steps to reproduce.

I aim to acknowledge reports within 7 days. Confirmed issues are fixed on `main`
and credited in the fix commit unless you prefer to stay anonymous.

## Dependency alerts

Dependency alerts that do not affect this application are dismissed with a
documented reason (for example, vulnerable code paths that the app never uses,
or build-time-only tooling).
