# SmartStock

Inventory and billing system for retail stores: a native Android app and a Spring MVC website on one Java 21 Spring Boot backend with PostgreSQL. UCS553 (Enterprise Web Application), TIET Patiala.

Goals and plan: [GOAL.md](GOAL.md). Architecture: [docs/architecture.md](docs/architecture.md).

## Layout

| Path | Contents |
| --- | --- |
| `server/` | Spring Boot application (Gradle) |
| `android/` | Android app (planned) |
| `docs/` | Architecture and design notes |
| `compose.yaml` | PostgreSQL + application |

## Run with Docker

```sh
cp .env.example .env    # set DB_PASSWORD
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_USER` | `smartstock` | Database user |
| `DB_PASSWORD` | required | Database password |
| `DB_PORT` | `5432` | Host port for PostgreSQL (localhost only) |
| `APP_PORT` | `8080` | Host port for the application |

## Develop

Requires JDK 21. Tests need Docker (Testcontainers).

```sh
docker compose up -d db
cd server
DB_PASSWORD=change-me ./gradlew bootRun   # or: ./gradlew bootTestRun (throwaway Testcontainers database)
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
