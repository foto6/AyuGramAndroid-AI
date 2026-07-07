# AyuGram Android AI

This fork adds a local AyuGram Android client integration for an OpenAI-compatible Web AI Bridge.

## Side-By-Side Install

The AI build uses a separate Android package:

```text
com.radolyn.ayugram.ai
```

It installs next to the regular AyuGram app and does not replace or delete it.
The launcher name is `AyuGram AI`.

Google Services/Firebase processing is disabled for this debug AI build by default,
because the bundled `google-services.json` belongs to the original package.
Set `ENABLE_GOOGLE_SERVICES=true` only after adding a `google-services.json`
client for `com.radolyn.ayugram.ai`.

## Build

Tools are expected on drive `E:`:

```powershell
.\build-ai-debug.ps1
```

For a normal non-debug universal APK:

```powershell
.\build-ai-universal.ps1
```

By default, the universal release build is signed with the local debug key
(`AI_USE_DEBUG_SIGNING_FOR_RELEASE=true`) so it can be installed without the
original exteraGram release keystore. Set it to `false` only when valid release
keystore credentials are configured.

The debug APK is written to:

```text
TMessagesProj\build\outputs\apk\beta\debug\ayuGram-ai-beta-universal-*.apk
```

The universal release APK is written to:

```text
TMessagesProj\build\outputs\apk\afat\release\ayuGram-ai-universal-*.apk
```

## Install

Enable USB debugging on the phone, connect it, then run:

```powershell
.\install-ai-debug.ps1
```

For the normal universal APK:

```powershell
.\install-ai-universal.ps1
```

## Configure In App

Open:

```text
AyuGram Preferences -> AI Bridge
```

Recommended local PC bridge settings when the phone is on the PC hotspot:

```text
Bridge URL: http://192.168.137.1:17448/v1
Model: chatgpt-web
API key: the bridge key from WebAIBridge
Debounce seconds: 10
History messages: 20
Private chats only: enabled
Enable auto replies: enabled only after Test bridge passes
```

Use `Test bridge` before enabling auto replies.

## Behavior

- Incoming messages are grouped per dialog.
- AI request starts only after no new messages arrive for the configured debounce interval.
- If newer messages arrive while an old response is running, the old response is ignored.
- Empty AI responses are treated as "do not answer".
- Multi-line AI responses can be sent as separate Telegram messages.

## Current Limits

- Media is described as placeholders such as `[photo]`, `[sticker]`, `[voice]`; files are not uploaded to the bridge yet.
- Chat history is in-memory after app start; full Telegram DB bootstrap is not implemented yet.
- The unavailable upstream proprietary history submodule is replaced with safe fallback classes.
