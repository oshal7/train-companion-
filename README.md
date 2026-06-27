# Train Travel Companion

A native Android app (Kotlin + Jetpack Compose) for Indian Railways
passengers, built around the v1 scope of the Train Travel Companion PRD:
live running status, PNR/seat status, platform alerts, offline-encrypted
ticket & Aadhaar storage, smart arrival alarms, and emergency
contacts/helplines.

The APK is built entirely in the cloud via GitHub Actions — no local
Android Studio install is required to produce an installable build.

## Getting the APK

1. Push to this branch (or trigger the workflow manually) to run
   **Actions -> Android APK Build**.
2. Open the workflow run and download the `train-companion-debug-apk`
   artifact (or `train-companion-release-apk`). Both are signed with the
   fixed keystore checked into `app/keystore/`, so they install directly
   on a device with "install unknown apps" enabled — no separate signing
   step is needed for v1.
3. Pushing a tag like `v1.0.0` also attaches both APKs to a GitHub
   Release.

Every build is signed with the same key and gets a strictly higher
`versionCode` (driven by the GitHub Actions run number). That means
installing a newly downloaded APK over an existing install just
**updates it in place** — no need to uninstall the old one first, and
your stored documents/alarms/settings carry over.

## Feature map (PRD -> code)

| PRD section | Feature | Implementation |
|---|---|---|
| 3.1 | Live running status | `rail/RailDataProvider*`, `ui/screens/LiveStatusScreen.kt`; optional offline GPS refinement via `rail/gps/` |
| 3.2 | PNR / seat / coach / berth | `pnrStatus()` in the provider layer, `ui/screens/PnrScreen.kt` |
| 3.3 | Platform alerts | Surfaced as part of `TrainStatus.platform` / route stops in the live status feed (no separate API) |
| 3.4 | Offline encrypted ticket & Aadhaar storage | `data/SecureFileStore.kt` (Jetpack `EncryptedFile`), `ui/screens/DocumentsScreen.kt` |
| 3.5 | Smart auto-alarms (Stage 1) | `ticket/TicketTextParser.kt` + `alarm/AlarmScheduler.kt`, `ui/screens/AlarmsScreen.kt` |
| 3.6 | Emergency contacts & helplines | `location/LocationHelper.kt`, `ui/screens/EmergencyScreen.kt` |

## Rail data provider

IRCTC/Indian Railways has no free official live-status/PNR API. The app
ships with `RailProviderType.MOCK` enabled by default, which generates
deterministic sample data so every screen is fully usable and demoable
offline with zero setup. Three real-data options are wired in via
`rail/RailDataProviderFactory.kt`, selectable in the **Settings** tab:

| Provider | Cost | Setup | Covers |
|---|---|---|---|
| [IndianRailAPI.com](https://indianrailapi.com) | Free "Starter" tier, 100 requests/day | Signup + API key | Live status + PNR |
| Free community PNR lookup (`pnrapi.dfth.in`) | Free | None — no key, no signup | PNR only |
| [RailwayAPI.com](https://railwayapi.com) | Paid | Signup + API key | Live status + PNR |

To use real data:

1. **Fastest, no signup**: in Settings, select "Free community PNR
   lookup" — PNR status starts returning real data immediately. It's an
   informal, community-run proxy ([source](https://github.com/sanketsaurav/pnrapi))
   with no uptime guarantee, served over plain HTTP, so the app scopes a
   cleartext exception to just that domain in
   `res/xml/network_security_config.xml` — every other request still
   requires HTTPS.
2. **Free, with live status**: sign up for IndianRailAPI.com's free
   tier, select it in Settings, and paste the key. Covers both live
   running status and PNR within the 100-requests/day cap.
3. **Paid, most reliable**: sign up with RailwayAPI.com for the same
   coverage without the free-tier rate limit.
4. `rail/RailApiProvider.kt` contains the HTTP client and provider-specific
   response parsing for IndianRailAPI.com and RailwayAPI.com;
   `rail/CommunityPnrProvider.kt` and the shared `rail/PnrSchemaParsing.kt`
   handle the free PNR-only lookup. All three degrade gracefully (defensive
   `optString`/`optInt` field access) since some response fields aren't
   fully documented by the providers — adjust the parsing functions if a
   provider changes its schema.

## GPS offline tracking (no API, no internet)

Apps like "Where is my Train" mostly work without calling any live-status
API at all: they use the phone's own GPS plus a bundled database of station
coordinates to figure out where the train physically is. The Live Status
screen has a **"Track with GPS (offline, no internet)"** button that does
the same thing:

- `rail/gps/StationCoordinates.kt` bundles ~100 major Indian Railways
  junctions with approximate lat/lon, shipped in the APK — no download, no
  API key, no network call.
- `rail/gps/GpsPositionEstimator.kt` takes the route already returned by
  whichever provider is active (mock or real), matches its station names
  against that bundled list, and uses the device's current GPS fix
  (haversine distance) to report the nearest matched station and distance
  to the next one — entirely on-device.
- This only refines *position within an already-known route*; it can't
  replace PNR lookups (booking/coach/berth only exists in IRCTC's systems)
  and it can't discover a route for an unrecognized train number on its
  own.
- Coverage is best-effort: routes through smaller halts not in the bundled
  list will show a partial or no match. Expanding the list (or swapping it
  for a real open dataset, e.g. OpenStreetMap rail data) is a natural
  follow-up.

## Security & privacy

- Tickets and Aadhaar files are written with Jetpack Security's
  `EncryptedFile` (AES-256-GCM, Android Keystore-backed) and are never
  uploaded anywhere — `data/SecureFileStore.kt`.
- Settings (including the rail-API key) are stored in
  `EncryptedSharedPreferences` — `data/SecurePrefs.kt`.
- `android:allowBackup="false"` is set in the manifest so Aadhaar/ticket
  data is excluded from Android's automatic cloud backup.
- Ticket parsing (Stage 1) works on plain text the user pastes from their
  PDF — there is no OCR step and no document image leaves the device.

## Alarms

Stage 1 (this build): user pastes the ticket text, `TicketTextParser`
extracts PNR/train number/stations/times, and the user sets a static
arrival alarm with a configurable buffer (5-60 min) before the scheduled
time. Alarms survive reboot (`alarm/BootRescheduleReceiver.kt`) and fall
back to inexact scheduling if "exact alarms" permission isn't granted.

Stage 2 (not in v1, per PRD): cross-referencing live delay data to
auto-shift the alarm is a natural follow-up once a real rail-data
provider is wired in, since `TrainStatus` already exposes a delay field.

## Local development

A local Android SDK/Studio install is **not** required to build via CI,
but if you have one:

```bash
./gradlew assembleDebug
```

Project layout:

- `app/src/main/java/com/traincompanion/app/data` — models, encrypted
  storage, repositories, manual DI (`AppContainer`)
- `app/src/main/java/com/traincompanion/app/rail` — pluggable rail data
  provider (mock + real API client)
- `app/src/main/java/com/traincompanion/app/ticket` — ticket text parser
- `app/src/main/java/com/traincompanion/app/alarm` — `AlarmManager`
  scheduling, notifications, boot rescheduling
- `app/src/main/java/com/traincompanion/app/location` — location share +
  dial intents
- `app/src/main/java/com/traincompanion/app/ui` — theme, navigation,
  Compose screens

## Known limitations (v1)

- Mock data is used until a real provider is configured in Settings (a
  free, no-signup option is available — see "Rail data provider" above).
- The free community PNR lookup is an informal third-party service with
  no uptime guarantee; the free IndianRailAPI.com tier is rate-limited
  to 100 requests/day.
- Ticket parsing is paste-text-based, not OCR/photo-based.
- Release APK is debug-signed (no production signing key configured
  yet); replace `signingConfigs.debug` in `app/build.gradle` with a real
  keystore before any store distribution.
- Booking, food ordering, route planning and cab booking are explicitly
  out of scope per the PRD.
