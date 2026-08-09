# Security policy

This repository is private. Report suspected vulnerabilities privately to the
repository owner. Do not open a public issue or include credentials, tokens,
personal data, provider payloads, exploit details, or production logs in an
issue.

Do not commit secrets. Use runtime environment variables or the approved
secret-management mechanism. If a credential may have been exposed, stop its
use, report the type and affected location without reproducing its value, and
arrange rotation with the owner.

Google provider access is disabled by default. Automated tests and ordinary CI
must never enable paid Google calls or use a live Google credential. Internal
service tokens must contain at least 32 bytes and must be supplied at runtime.

CI verifies the Maven build, scans complete Git history for secrets, builds the
non-root container and scans the image for Critical and High vulnerabilities.
