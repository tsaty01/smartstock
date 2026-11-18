# SmartStock

Inventory and billing system for retail stores: a native Android app and a Spring MVC website on one Java 21 Spring Boot backend with PostgreSQL. UCS553 (Enterprise Web Application), TIET Patiala.

Goals and plan: [GOAL.md](GOAL.md). Architecture: [docs/architecture.md](docs/architecture.md). REST API: [docs/api.md](docs/api.md).

## Layout

| Path | Contents |
| --- | --- |
| `server/` | Spring Boot application (Gradle) |
| `android/` | Android app (planned) |
| `docs/` | Architecture and design notes |
| `compose.yaml` | PostgreSQL + application |

## Run with Docker

```sh
cp .env.example .env    # set DB_PASSWORD and TOKEN_SECRET
docker compose up -d --build
curl http://localhost:8080/actuator/health
curl -H 'Content-Type: application/json' -d '{"username":"manager","password":"smartstock"}' http://localhost:8080/api/auth/login
```

With `SPRING_PROFILES_ACTIVE=demo`, sample data is loaded: products, discount rules and the accounts `admin`, `manager`, `cashier`, `cashier2`, `customer`, `customer2`, all with password `smartstock`. Leave the profile empty for a clean database.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_USER` | `smartstock` | Database user |
| `DB_PASSWORD` | required | Database password |
| `DB_PORT` | `5432` | Host port for PostgreSQL (localhost only) |
| `APP_PORT` | `8080` | Host port for the application |
| `TOKEN_SECRET` | required | API token signing key, at least 32 characters |
| `SPRING_PROFILES_ACTIVE` | empty | `demo` loads sample data |

Product images are stored in the `app-data` volume, the database in `db-data`.

## Develop

Requires JDK 21. Tests need Docker (Testcontainers).

```sh
docker compose up -d db
cd server
DB_PASSWORD=change-me TOKEN_SECRET=<32+ chars> ./gradlew bootRun --args=--spring.profiles.active=demo
./gradlew build
```

## Releases

Commits follow [Conventional Commits](https://www.conventionalcommits.org). Work lands on `dev`; `main` receives merges from `dev`. After CI passes, semantic-release picks the next version, updates `CHANGELOG.md` and `server/gradle.properties`, tags and publishes a GitHub release with notes and artifacts:

| Artifact | Where |
| --- | --- |
| `smartstock-server-<version>-all.jar`: executable fat JAR (includes the website) | GitHub release asset |
| `smartstock-server-<version>.jar`: plain JAR, dependencies not included | GitHub release asset |
| Server image | `ghcr.io/<owner>/<repo>-server:<version>`, plus `latest` (main) or `dev` |
| Android APK | GitHub release asset (once `android/` exists) |

| Branch | Release |
| --- | --- |
| `dev` | Prerelease `X.Y.Z-dev.N` |
| `main` | Stable `X.Y.Z` |

Pushes to `dev`/`main` run CI, then semantic-release (`.releaserc.json`, scripts in `.github/scripts/`). A prerelease on `dev` opens (or retitles) a `dev → main` pull request. Merge it with a merge commit, not a squash, so the conventional commits reach `main`. After a stable release, `main` is back-merged into `dev`. Dependabot targets `dev`.

Repository setting required: Actions → General → Allow GitHub Actions to create pull requests.

## Team

Ishant Mehndiratta, Satyam Tiwari, Anshaj, Aayush Bindal. Instructor: Dr. Kanupriya.
