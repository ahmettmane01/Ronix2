# Ronix — Android e-learning app (Kotlin, Jetpack Compose, Supabase)

Dark-only (black and purple). Courses are Free or Paid. A student enrolling in a Paid course sends a request that the admin accepts or rejects. The app starts empty: the admin adds all courses and lessons (YouTube links) inside the app.

## What is included
- Student: register / sign in, Home, Explore (search + Free/Paid filter), course page with curriculum, in-app YouTube lesson player with comments below it (no previous/next buttons, no auto-advance), My Learning with progress and streak, Profile, Notifications (announcements).
- Admin (signs in on the same screen as students; the database role opens the admin area, and students can never reach it): Overview, add/edit course (thumbnail from the device, title, optional description, Free/Paid, chapters, lessons with YouTube link validation), publish/unpublish, delete with confirmation, enrollment requests (accept/reject), students list, announcements.
- Security: `FLAG_SECURE` blocks screenshots, screen recording and the recent-apps preview. Roles are enforced by Supabase Row Level Security; the app never trusts a client-side role. Paid video IDs are only readable by approved students (`lesson_videos` table + `can_watch()`).

## Keys and `.env`
- `.env` (project root, git-ignored) holds `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`. Gradle injects them into `BuildConfig` at build time. In CI they come from GitHub Secrets of the same names.
- `supabase/.env` holds the secret key for **server-side use only** (setup scripts, Edge Functions). Gradle never reads it, so it can never end up inside the APK. Do not commit or share it. If it was ever shared, rotate it in the Supabase dashboard.

## Setup
1. Supabase → SQL editor → run `supabase/migrations/001_init.sql` (creates tables, RLS, storage bucket).
2. Authentication → Providers → Email enabled. (Turn off "Confirm email" if you want instant sign-in.)
3. Register the owner account in the app, then in the SQL editor run:
   `update profiles set role = 'admin' where email = 'OWNER_EMAIL';`
   The owner then signs in on the normal sign-in screen. No admin password exists in the source code.
4. Open the project in Android Studio (JDK 17) and run, or run `gradle wrapper` once and then `./gradlew assembleDebug`.
   APK: `app/build/outputs/apk/debug/app-debug.apk`.
5. GitHub Actions (`.github/workflows/android.yml`): add secrets `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`; the debug APK is uploaded as an artifact.

## Video playback
Lessons play inside the app through YouTube's embedded player (WebView). The app sends a `Referer` of `https://<package name>` as YouTube requires (otherwise error 153). Videos must be **public or unlisted with embedding allowed**; private videos or videos whose owner disabled embedding show "Video unavailable". Test each video once from the admin account after adding it.

## GitHub Actions
Add repository secrets `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` (Settings → Secrets and variables → Actions); the workflow stops with a clear message if they are missing. `.env` is git-ignored, so CI cannot read it.

## Tests
`gradle testDebugUnitTest` runs unit tests for YouTube link parsing, progress percentage and streak.

## Known limitations
- Not compiled or run in the environment where it was generated (no Android SDK or network there). Dependency versions are pinned in `gradle/libs.versions.toml`; if one fails to resolve, bump it.
- Not included: Google sign-in (removed on request), push notifications (FCM needs your `google-services.json`), realtime comments (comments refresh every 15 seconds), payments (paid access is approved manually), UI/instrumentation tests.
- `FLAG_SECURE` cannot stop someone filming the screen with another device.
