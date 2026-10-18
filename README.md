# AU mBanking

Android banking UI with silent intruder detection.

- Wrong MPIN → bank home opens normally; camera/GPS/Firebase + email alert run in the background.
- Owner MPIN → Security Center (changeable in Settings).

## Owner customization (Security Center → Settings)

- Change MPIN
- Display name, greeting, tagline, UPI ID, account last-4, FD banner text
- Alert email (Brevo SMTP)
- Capture / location / stealth toggles

## SMTP

Credentials live in `local.properties` (gitignored): `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`.

Set your **Alert email** in Settings so alerts are delivered.

## Run

Open in Android Studio and Run (`minSdk 24`).
