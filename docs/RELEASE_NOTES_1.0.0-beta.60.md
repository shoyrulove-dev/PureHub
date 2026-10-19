# PureHub 1.0.0-beta.60

PureHub beta.60 makes the Android visual experience consistent and readable across phones, regardless of the device system theme.

## Consistent dark-violet interface

- Uses PureHub's dark-violet palette by default instead of switching to a bright canvas on phones using light system mode.
- Synchronizes the status bar and navigation bar with the application background.
- Removes the remaining system-theme dependency from shared mini-app canvases, accent colors, and accent containers.

## Home polish

- Replaces the bright Wi-Fi, OCR, and Flashlight recommendation cards with dark, category-tinted surfaces and high-contrast labels.
- Adds balanced spacing below Quick access and On-device recommendations in all four supported languages.
- Keeps the search field, quick actions, daily Lunar card, recommendation row, and bottom navigation within one coherent palette.

## Verification

- Android unit tests and debug assembly passed.
- The updated Home and Settings screens were verified on a physical Android phone with system dark mode disabled.
- The beta.59 encrypted-storage and modern Wi-Fi API changes remain intact.
