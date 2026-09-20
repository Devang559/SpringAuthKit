# SpringAuthKit Demo

A small Spring Boot application showing how to integrate SpringAuthKit.

## Prerequisites

SpringAuthKit must be available in your local Maven repository:

```bash
# from the library project root
../mvnw install -DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true
```

> The `-Dmaven.source.skip=true` / `-Dmaven.javadoc.skip=true` flags are only needed in a
> restricted/offline environment where the source & javadoc plugins can't be downloaded.

## Run

```bash
../mvnw spring-boot:run
```

Then exercise the API (replace `demo` with a real JWT secret in production, and bind
`spring.authkit.jwt.secret` to the `AUTHKIT_JWT_SECRET` environment variable):

```bash
# 1) register (check the console for the verification OTP logged by the no-op EmailService)
curl -s -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"Secret123","firstName":"Alice"}'

# 2) verify email (paste the OTP printed by the app)
curl -s -X POST http://localhost:8080/auth/verify-otp \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","otp":"<OTP>","purpose":"EMAIL_VERIFICATION"}'

# 3) login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"alice@example.com","password":"Secret123"}')
echo "$TOKEN"

# 4) call a protected endpoint
curl -s http://localhost:8080/api/notes \
  -H "Authorization: Bearer $(echo "$TOKEN" | jq -r .accessToken)"

# 5) refresh + logout
RT=$(curl -s -X POST http://localhost:8080/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$(echo "$TOKEN" | jq -r .refreshToken)\"}")
echo "$RT" | jq -r .accessToken  # new access token
curl -s -X POST http://localhost:8080/auth/logout \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$(echo "$TOKEN" | jq -r .refreshToken)\"}"
```

## Notes

- The default `EmailService` only logs OTP issuance. Override the `EmailService` bean to send
  real email (see the main README).
- The H2 console is enabled at `/h2-console` (JDBC URL `jdbc:h2:mem:demo`).
- The library is verified end-to-end by its test suite (see `src/test`), which runs the full
  register → verify → login → protected → refresh → logout flow against an in-memory H2 database.
