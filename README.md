# SmartStock

Inventory and billing system for retail stores: a native Android app and a Spring MVC website on one Java 21 Spring Boot backend with PostgreSQL. UCS553 (Enterprise Web Application), TIET Patiala.

Goals and plan: [GOAL.md](GOAL.md). Architecture: [docs/architecture.md](docs/architecture.md). REST API: [docs/api.md](docs/api.md).

## Layout

| Path | Contents |
| --- | --- |
| `server/` | Spring Boot application (Gradle) |
| `android/` | Android app (Kotlin, Jetpack Compose, Material 3 Expressive) |
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
| `COOKIE_SECURE` | `false` | `true` marks the website session cookie Secure (use behind HTTPS) |

Product images are stored in the `app-data` volume, the database in `db-data`.

## Website

Open http://localhost:8080 and sign in. Each role lands on its own page: management on the dashboard, cashiers on the till, customers on their purchases. New customers can register at `/register`. Set `COOKIE_SECURE=true` when the site is served over HTTPS.

## Android app

Install the APK from a GitHub release (Android 8.0 or later):

| APK | Server connection |
| --- | --- |
| `smartstock-android-<version>.apk` | HTTPS only |
| `smartstock-android-<version>-http.apk` | Also plain HTTP, for a laptop or local-network server |

On the sign-in screen, tap **Server** to set the address, e.g. `http://192.168.1.20:8080`. The emulator default `http://10.0.2.2:8080` reaches a server on the host machine. The session token is stored encrypted with an Android Keystore key and is not backed up.

Screens by role: management gets Dashboard, Products (add, edit, restock, adjust, photo, stock history), Sell, Orders, Alerts, Reports and Users (admin); cashiers get Sell, Orders and Products; customers get their purchases and the shop catalog. The till prices every cart through the server's quote endpoint, so totals, offers and tax always match the receipt.

## Develop

Requires JDK 21. Server tests start their own embedded PostgreSQL, so they need no Docker; the Android build needs the Android SDK (`ANDROID_HOME`).

```sh
docker compose up -d db
cd server
DB_PASSWORD=change-me TOKEN_SECRET=<32+ chars> ./gradlew bootRun --args=--spring.profiles.active=demo
./gradlew build

cd ../android
./gradlew lintDebug assembleDebug    # app/build/outputs/apk/debug
```

Release APKs are signed in CI with the `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` repository secrets.

## Releases

Commits follow [Conventional Commits](https://www.conventionalcommits.org). Work lands on `dev`; `main` receives merges from `dev`. After CI passes, semantic-release picks the next version, updates `CHANGELOG.md`, `server/gradle.properties` and `android/gradle.properties`, tags and publishes a GitHub release with notes and artifacts:

| Artifact | Where |
| --- | --- |
| `smartstock-server-<version>-all.jar`: executable fat JAR (includes the website) | GitHub release asset |
| `smartstock-server-<version>.jar`: plain JAR, dependencies not included | GitHub release asset |
| Server image | `ghcr.io/<owner>/<repo>-server:<version>`, plus `latest` (main) or `dev` |
| `smartstock-android-<version>.apk` and `-http.apk`: signed Android app | GitHub release asset |

| Branch | Release |
| --- | --- |
| `dev` | Prerelease `X.Y.Z-dev.N` |
| `main` | Stable `X.Y.Z` |

Pushes to `dev`/`main` run CI, then semantic-release (`.releaserc.json`, scripts in `.github/scripts/`). A prerelease on `dev` opens a ``chore: Merge branch `dev` to `main` `` pull request if none is open. Merge it with a merge commit, not a squash, so the conventional commits reach `main`. After a stable release, `main` is back-merged into `dev`. Dependabot targets `dev`.

Repository setting required: Actions → General → Allow GitHub Actions to create pull requests.

## Team

Ishant Mehndiratta, Satyam Tiwari, Anshaj, Aayush Bindal. Instructor: Dr. Kanupriya.
