# Update veröffentlichen

## Vorbereitung (macht Claude)

1. `mod.version` in `mod/stonecutter.properties.toml` erhöhen.
2. Eintrag in `CHANGELOG.md`.
3. `cd mod && .\gradlew build` – vier Jars unter `mod/versions/<ziel>/build/libs/`.
4. Commit, Push, Tag `vX.Y.Z` pushen. Die CI baut dieselben Jars nochmal und hängt sie als
   Artefakt an.
5. Release-Ordner `%USERPROFILE%\Downloads\hugomarket-release-X.Y.Z` mit den vier Jars,
   `CHANGELOG.md` und `modrinth-description.md`.

## Modrinth (macht der Betreiber)

1. Projektseite → Versions → "Create version".
2. Viermal, je eine Datei:

| Version number | Datei | Game versions |
|---|---|---|
| X.Y.Z+1.21.5 | hugomarket-X.Y.Z+1.21.5.jar | 1.21.5 |
| X.Y.Z+1.21.8 | hugomarket-X.Y.Z+1.21.8.jar | 1.21.6, 1.21.7, 1.21.8 |
| X.Y.Z+1.21.10 | hugomarket-X.Y.Z+1.21.10.jar | 1.21.9, 1.21.10 |
| X.Y.Z+1.21.11 | hugomarket-X.Y.Z+1.21.11.jar | 1.21.11 |

   Channel Release, Loader Fabric, Dependency Fabric API (Required), Changelog aus
   `CHANGELOG.md` (nur der neue Abschnitt).
3. Fertig. Launcher mit Modrinth-Anbindung (Prism, Modrinth App) zeigen das Update von allein.

## Spieler (so geht das Update)

- **NoRisk Client**: alte `hugomarket-*.jar` aus `custom_mods` des Profils löschen, neue
  hineinlegen, Spiel neu starten. Nie zwei Hugonomics-Jars gleichzeitig im Ordner lassen.
- **Prism / Modrinth App**: Mod im Profil anklicken → Update.
- **Vanilla Fabric**: `mods`-Ordner, alte Jar raus, neue rein.

Einstellungen, Watchlist, Preise, Trade-Log und Profil-Daten bleiben erhalten, sie liegen in
`config/hugomarket/` und nicht in der Jar.

## Server-seitig

Soll eine alte Version ausgesperrt werden: `MIN_CLIENT_VERSION=X.Y.Z` in der Backend-`.env`
und `docker compose up -d`. Ältere Clients bekommen dann keine Upload-Erlaubnis mehr, lesen
aber weiter Preise.
