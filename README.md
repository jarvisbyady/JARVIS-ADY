# JARVIS by Ady — Android starter

An Android-first personal assistant starter for Ady's Motorola phone.

## What is included
- Native Kotlin Android app
- Hindi speech input via Android speech recognizer
- Hindi text-to-speech
- Chat interface
- Optional connection to a private AI backend
- GitHub Actions workflow to build a debug APK

## Important setup
The app expects a backend URL that accepts `POST /chat` with JSON `{ "message": "..." }`
and returns JSON containing `reply`. Do not put a Gemini API key inside the Android app.

The current screen is a foundation build. It does not yet have persistent cloud memory,
wake-word listening, background automation, or direct phone-control permissions.

## Build APK
Push this project to GitHub. Open **Actions** → **Build JARVIS APK**. When the workflow
finishes, open the run and download the `JARVIS-ADY-debug-apk` artifact.

## Backend
Deploy a secure server/Cloudflare Worker that stores the Gemini API key as a secret and
implements `/chat`. Add its base URL to the app's backend setting when that setting is
implemented in the next build. For this first foundation build, the URL is read from the
app's private preference `worker_url`; a setup screen will be added next.
