# Mitmachen

Monorepo für die HugoSMP Markt-Mod (`mod/`, Fabric, Java 25) und ihr Backend (`backend/`,
Node 24, TypeScript). Architektur, Datenmodell und Roadmap stehen in `PLAN.md`; dieses
Dokument regelt, wie Code hier geschrieben wird.

## Sprache

- Code, Bezeichner, Commit-Messages, Dateinamen: Englisch.
- UI-Texte nur in `lang/de_de.json` und `lang/en_us.json`, nie hartcodiert.
- Dokumente in `docs/` und Antworten an den Nutzer: Deutsch.

## Kommentare und Dateien

- Keine erklärenden Kommentare, keine Lehrbuch-Kommentare, keine Platzhalter
  (`// TODO`, `// hier wird ...`, `// implement later`). Ein Kommentar nur, wenn er einen
  nicht offensichtlichen Grund dokumentiert (Workaround, Server-Eigenheit, Performance-Grund).
- Keine Emojis, keine ausführlichen Docstrings/Javadoc, keine Zusammenfassungs- oder
  Header-Blöcke in Dateien.
- Keine toten Codepfade, keine auskommentierten Blöcke, keine ungenutzten Imports,
  Felder oder Parameter.

## Struktur

- Kleine Klassen mit einer Aufgabe, klare Namen, keine God-Classes, keine `Utils`-Sammelklassen.
- Jedes Feature ist ein Modul unter `module/<id>/` mit `Module`-Implementierung und eigenem
  Enable-Flag. Module kommunizieren nur über Stores und den Event-Bus, nie direkt.
- Mixins enthalten keine Logik: ein Inject, ein Aufruf in eine Klasse außerhalb von `hook/`.
  Fabric-API-Events gehen vor Mixins. Accessor-Mixins statt Reflection.
- Parser, Preislogik, Rechner und Zustandsautomaten sind reine Klassen ohne
  Minecraft-Abhängigkeit, damit sie ohne Spiel testbar sind.
- Erkennungsregeln (Screen-Titel, Slot-Layouts, Chat-Regex) liegen in Datendateien unter
  `resources/`, nicht im Code.
- `shared/` definiert ItemKey-Schema, Kategorien und API-Schema. Mod und Backend
  implementieren es identisch und testen gegen dieselben Vektoren.

## Performance

- Nichts Teures im Render-Tick: keine Parser, keine Netzwerkaufrufe, keine Datei-IO, keine
  Streams, keine String-Konkatenation, keine Allokationen in Schleifen. Render liest nur
  vorberechnete Werte aus Stores.
- Parser nur bei Slot-Updates (`ContainerTracker.markDirty`), höchstens einmal je 2 Ticks.
- Netzwerk ausschließlich über `BackendClient` auf dessen Executor; Ergebnisse per
  `ClientScheduler.post` auf den Client-Thread. Uploads gebündelt, komprimiert, asynchron.
- Caches mit klarer Invalidierung (Komponenten-Hash), keine unbegrenzten Maps.
- Datei-Schreiben gebündelt und atomar (tmp + rename).

## Verhalten gegenüber dem Server

- Keine Aktion im AH, in Orders oder im Shop ohne Klick des Spielers. Kein Auto-Kauf, kein
  Sniping, keine Paket-Manipulation.
- Text ins Chat-/Anvil-Feld setzen ist erlaubt, Absenden nicht. Ausnahme: ein einzelner
  `/ah <suchbegriff>` direkt auf Klick des Spielers. Danach darf die Mod sortieren und bis zu 15
  Seiten blättern, um genau dieses Angebot zu finden. Kaufen klickt immer der Spieler.
- Module mit Server-Aktionen (`ahpaging`, `sellmacro`, `monitor`, `cmdhotkeys`) sind nur mit
  Freigabe-Flag aus `/v1/meta` plus lokaler Bestätigung aktivierbar und benutzen den echten
  Klick-Pfad (`handleInventoryMouseClick`) mit konfigurierbaren Verzögerungen.
- Außerhalb von `hugosmp.net` sind alle Module passiv.

## Daten und Datenschutz

- Hochgeladen werden nur Beobachtungen (Listings, Orders, Shop-Preise, anonyme
  Volumen-Events, opt-in RTP-Punkte). Keine Spielernamen, keine Account-UUID, keine
  Koordinaten außer RTP-Opt-in.
- Verkäufernamen vor dem Upload mit dem Pepper aus `/v1/meta` hashen.
- Trade-Log, Sessions und Vorlagen bleiben lokal.
- Feature aus bedeutet: keine Requests, kein Zählen, keine Tasten-Listener.

## Tests

- JUnit 5 (Mod) und Vitest (Backend). Pflicht für Parser, ItemKey-Normalisierung,
  Preislogik, Rechner, Sell-Stats, Zustandsautomaten, Konsens und Plausibilitätsprüfung.
- Parser-Tests laufen gegen echte Dumps in `mod/src/test/resources/dumps/`. Jede Dump-Datei
  hat eine Erwartungsdatei daneben. Keine erfundenen Beispiel-Lores.
- Dumps enthalten keine fremden Klarnamen; vor dem Einchecken Verkäufer durch `seller_N`
  ersetzen.

## Vor jedem Commit

- `mod`: `gradlew build` ohne Warnings, Client startet, Tests grün.
- `backend`: `npm run typecheck`, `npm run lint`, `npm test` grün.
- Keine ungenutzten Imports, keine Debug-Ausgaben, keine Versionsbumps nebenbei.
- Ein Commit = eine Änderung. Message im Imperativ, erste Zeile ≤ 72 Zeichen.

## Build-Befehle

Voraussetzungen: JDK 21 (`%USERPROFILE%\.jdks\temurin-21`, `JAVA_HOME` gesetzt), Git
(`%USERPROFILE%\.tools\git\cmd` im PATH), Node 24.

Mod (`mod/`, Stonecutter-Ziele 1.21.5, 1.21.8, 1.21.10, 1.21.11; aktiv ist 1.21.5):

```
.\gradlew :1.21.5:build          Build + Tests für ein Ziel
.\gradlew build                  Build + Tests für alle Ziele (Pflicht vor Commit)
.\gradlew :1.21.5:runClient      Dev-Client, Run-Verzeichnis mod/run
.\gradlew "Set active project to 1.21.11"   aktives Ziel wechseln (IDE, runClient)
.\gradlew "Reset active project"
```

Jars liegen unter `mod/versions/<ziel>/build/libs/`. Dumps landen im Run-Verzeichnis unter
`config/hugomarket/dumps/`.

Backend (`backend/`, Node 24 + TypeScript):

```
npm install
npm run typecheck && npm run lint && npm test   Pflicht vor Commit
npm run dev                                     lokaler Server ohne Datenbank (Memory-Store)
docker compose up -d --build                    Postgres + API (Zugangsdaten aus .env)
docker compose exec api node dist/db/migrate.js Tabellen anlegen
```

Deployment siehe `docs/deploy.md`.
