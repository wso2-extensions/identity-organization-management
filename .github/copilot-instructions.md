## Comments
- All public methods should have a docstring.
- Comments should start with a space and first letter capitalized.
- Comments should always end with a period.

## Logs
### Debug
- If there's a string concatenation, then having `if (LOG.isDebugEnabled())` is mandatory.
    - Make sure to not use `LOG.debug` if the string concatenation is not used.

## DAO Layer
- All database queries should support the following database types:
    - DB2
    - H2
    - MS SQL Server
    - MySQL
    - Oracle
    - PostgreSQL
  If a query is not supported by one of the above databases, then it should be mentioned in the comment.

## Primary Security Checklist:
- Injection Flaws: Scrutinize all user-controlled input for potential SQL Injection, Cross-Site Scripting (XSS), or Command Injection.

- Hardcoded Secrets: Search for any exposed secrets like API keys, passwords, or private tokens.

- Broken Access Control: Verify that any new endpoints or data access functions have proper authorization and permission checks.

- Sensitive Data Exposure: Ensure that no sensitive user data (e.g., PII, credentials) is being logged or sent in error messages.

- Insecure Dependencies: Check if any newly added dependencies have known vulnerabilities.

### Reporting Format:
For each issue you find, provide a concise, inline review comment that includes:

- Vulnerability: A brief title for the issue (e.g., "Potential XSS").

- Risk: A one-sentence explanation of the potential impact.

- Suggestion: A clear, actionable recommendation for a fix.
