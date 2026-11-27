# Code review

Scope: current working tree, Android client, Spring Boot API, tests, and CI. Static review only. The working tree contains substantial uncommitted changes, including removal of the website. No build or device test was run for this review. AFT language diagnostics are unavailable for Kotlin and Java.

## Findings

1. **Fixed locally, unverified: changing server address sends the existing bearer token to the new host.** `SessionStore.setServerUrl` (`android/app/src/main/java/com/smartstock/app/data/SessionStore.kt:36-38`) changes only the URL. `Backend` (`android/app/src/main/java/com/smartstock/app/data/Backend.kt:47-54`) immediately attaches the existing session token to all requests routed to that URL. More exposes this while signed in (`android/app/src/main/java/com/smartstock/app/ui/more/MoreScreens.kt:103-126`). Changing stores or entering an untrusted address discloses the old server's token. Clear the old session when changing hosts, before making any request to the new host.

2. **Fixed locally, unverified: image replacement is not atomic with the product row.** `ProductImageService.store` (`server/src/main/java/com/smartstock/inventory/ProductImageService.java:34-50`) writes the new file and deletes the previous file inside a database transaction, before commit. A later transaction failure rolls back the new filename but cannot restore the old file, leaving the product pointing at a deleted image. Keep the old file until commit succeeds, and clean up newly written files on rollback.

3. **Medium: historical category reports follow today's product category.** `ReportService.categorySales` (`server/src/main/java/com/smartstock/reporting/ReportService.java:92-104`) joins `order_items` through the product's current `category_id`. `OrderItem` stores name and price snapshots but no category snapshot (`server/src/main/java/com/smartstock/billing/OrderItem.java:24-47`). Moving a product to a new category rewrites past category sales totals. Snapshot the category on each order item if these reports are meant to reflect categories at the time of sale.

4. **Medium: CI does not exercise Android runtime behavior.** `.github/workflows/ci.yml:22-34` runs lint and assembles a debug APK, but there is no Android test source set. The recent offline Products crash was a coroutine runtime failure invisible to compilation and lint. Add a focused ViewModel test for a failing product/categories request, retry, and successful recovery.

## Follow-up

`SessionStore` now clears the session in the same DataStore edit as a server change and binds each token to its originating URL; the interceptor also checks that origin before sending it. `ProductImageService` now retains the old file until commit and deletes the new file on rollback. The full server build and PostgreSQL integration suite now pass, including an image rollback test; the Android release build installs on the Galaxy. Server-switch token isolation is compiled but has not been exercised against two live server origins. Historical category attribution and Android offline runtime test coverage remain open. Do not automatically deploy a category-snapshot migration without an explicit database backup and compatibility review.

## Navigation observation

The normal pop used a 240 ms right slide, but Navigation Compose 2.10.2 has separate predictive-pop defaults: its outgoing page scales to 0.7, explaining the minimize motion reported on the installed Galaxy. Both signed-in and signed-out NavHosts now explicitly use the right slide for predictive Back as well. This correction was built and installed on the Galaxy. Tabs now use a floating dock with a moving selection pill; the Back transition remains a right slide.
