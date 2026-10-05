# Block Puzzle Bot — Android

Standalone Android project for the supplied Block Puzzle screenshot. It uses Android AccessibilityService screenshot + gesture APIs; no AutoJs6 is required.

## Build
Open this folder in Android Studio and Build > Build APK(s).

## First run
1. Install APK.
2. Open Block Puzzle Bot.
3. Open Special app access / Accessibility and enable Block Puzzle Bot.
4. Return to the app and tap Start.
5. Open the game and let the bot play.

The coordinates are normalized for the supplied 1080x2400 POCO X7 Pro layout. If the game UI differs, tune the constants in BotAccessibilityService.java.
