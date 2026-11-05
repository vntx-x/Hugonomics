# HugoSMP Markt-Mod – Plan

Stand: 2026-11-05. Arbeitstitel `hugomarket` (Mod-ID), endgültiger Name offen.

## 1. Ziel

Client-Mod (Fabric) plus eigenes Backend für HugoSMP: bessere AH-Bedienung (Suche, Filter,
Sortierung, Verkaufs-Vorschlag), Orders, Rechner, Preis-Tooltips, Container-Wert, Historie,
Tracking, Streamer-Tools, Werkzeuge. Feature-Parität mit dem FWM Mod als Untergrenze,
Unterscheidung durch die AH-Bedienung selbst.

Leitplanken, nicht verhandelbar:

- Keine Server-API. Daten nur aus Container-GUIs, Item-Komponenten (Name, Lore, Enchants,
  Container-Inhalt) und Chat.
- hugosmp-market.net wird nicht gescraped.
- Kein Auto-Kauf, kein Sniping, keine Aktion im AH/Orders ohne Klick des Spielers.
  Automatisierung nur dort, wo FWM sie hat (Sell-Macro, AH-Blättern), über echte Klick-Pfade,
  mit Verzögerung, abschaltbar, erst nach Freigabe durch das Hugo-Team.
- Jedes Feature ein Modul mit eigenem Enable-Flag. Aus heißt: keine Requests, kein Zählen,
  keine Keys.
- Verkäufernamen verlassen den Client nur gehasht.

## 2. Recherche-Stand

### 2.1 HugoSMP

| Punkt | Befund | Quelle |
|---|---|---|
| Adresse | `hugosmp.net` (HugoSMP Tweaks prüft `address.contains("hugosmp.net")`) | Tweaks-Quellcode |
| Client-Versionen | FWM liefert Builds für 1.21.4 bis 26.3, also lässt der Server alle per Via zu | Modrinth-API fwm-mod |
| NoRisk-Profile | NRC 26.3, 1.21.11, 1.21.4 (und ältere) | norisk.gg |
| Lore-Zeilen | Hugo schreibt `Wert:` in Item-Tooltips, AH-Listings haben `Preis:`; Preise mit deutschem Tausenderpunkt | Tweaks `PricePerItemMixin`, `PriceParser` |
| Custom-Items | Item-Keys von hugosmp-market.net enthalten `oraxen:`-Präfix, Hugo nutzt also Oraxen/Nexo-Custom-Items | eigener Arbitrage-Scanner (`out/latest.json`) |
| Item-Key-Form dort | `diamond_pickaxe/efficiency:5`, `splash_potion/p7` – Basis-ID plus Varianten-Suffix | ebd. |
| Gebühren | unbekannt; FWM 6.5 trackt "auction and order income, spending, and fees", es gibt also welche | FWM-Changelog |
| Order-System | vorhanden ("Order-System für gezielte Aufträge") | minecraft-server.eu |
| Trade-Fenster | unbekannt | – |
| Regeln zu Mods | kein öffentliches Regelwerk gefunden; nur eine AFK-Dienst-Werbeseite behauptet, Sell-Macros seien erlaubt (unzuverlässig) | Websuche |
| Shop | `shop.hugosmp.net` verkauft Gems; Ingame-Shop-GUI unbekannt | Websuche |

### 2.2 FWM Mod (Modrinth `fwm-mod`, Projekt `nxQeqT7s`)

- Fabric, client-only, ARR, keine Dependencies, Mod Menu optional. 23.2K Downloads, wöchentliche Releases, 13 Builds pro Release (ein Jar pro MC-Version).
- Feature-Stand 6.6 (2026-10-31): Sell-Macro, Sell-Stats, Minutentest, Marktpreise (1 h–31 d, 30-Tage-Chart), Preis-Ping, Spawner-Preise, Enderchest-Wert, Auktions-Karte, RTP-Map, Color-Codes, Streamer-Auktionen, Giveaways, 7TV/BTTV-Emotes, Tickets, Clan-Badges, Money-Tracking für AH/Order-Einnahmen/Ausgaben/Gebühren, Order-Preis-Alerts, Spawner-Macro.
- In 6.6 entfernt: Command-Hotkeys und Auto-Reconnect. Grund nicht genannt, möglicherweise Regelkonflikt. Vor dem Bau unserer Command-Hotkeys beim Hugo-Team nachfragen.
- Privacy-Aussagen auf der Seite: nur Datenabruf, Koordinaten bleiben lokal, Feature aus = keine Requests. Das ist der Standard, den wir mindestens halten.

### 2.3 HugoSMP Tweaks (`derdavidoo/hugosmptweaks`, MIT)

- 0.1.0-alpha vom 2026-01-03, MC 1.21.4–1.21.11, Yarn, YACL 3.8, Mod Menu 13. Letzter Stand, seitdem nichts.
- Brauchbar als Referenz: `PriceParser` (de/en Zahlenformat), `PricePerItemMixin` (Hook auf `ItemStack.getTooltipLines`, Trigger `Wert:`/`Preis:`), `ServerCheck`, `TpaRequestMixin` (Hook auf `ClientPacketListener.handleSystemChat`).
- Kein AH-Parser, keine Container-Hooks, keine Historie. Als Vorlage für unsere Lore-Regex nur bedingt nützlich; die echten Formate kommen aus unseren Dumps.

### 2.4 Toolchain (entschieden am 2026-11-05)

Zielversionen laut Nutzer: 1.21.5 (eigene Spielversion) bis 1.21.11. Vier Build-Ziele über
Stonecutter, je eines pro kompatibler Gruppe:

| Build-Ziel | Deckt ab | Fabric API | YACL | Mod Menu |
|---|---|---|---|---|
| 1.21.5 | 1.21.5 | 0.128.2+1.21.5 | 3.8.2+1.21.5-fabric | 14.0.2 |
| 1.21.8 | 1.21.6–1.21.8 | 0.136.1+1.21.8 | 3.8.2+1.21.6-fabric | 15.0.2 |
| 1.21.10 | 1.21.9–1.21.10 | 0.138.4+1.21.10 | 3.8.2+1.21.10-fabric | 16.0.1 |
| 1.21.11 | 1.21.11 | 0.141.6+1.21.11 | 3.8.2+1.21.11-fabric | 17.0.1 |

Gemeinsam: Fabric Loader 0.20.1 (Abhängigkeit `>=0.16`), Java 21, Gradle 9.8, Loom 1.17.21
(klassisches Plugin `fabric-loom`), Mojang-Mappings mit Parchment-Parameternamen,
Stonecutter 0.9.8 (zentrale `stonecutter.properties.toml`, kein `versions/`-Ordner nötig),
Entwicklungs- und VCS-Version 1.21.5. Spätere 26.x-Ziele brauchen Java 25 und das neue
Loom-Plugin; mit Mojang-Mappings ist das ein Build-Thema, kein Rename.

Bekannte Bruchstellen innerhalb 1.21.5–1.21.11, die Stonecutter-Bedingungen brauchen:
GUI-Rendering (1.21.6: neue `GuiGraphics`-Pipeline, HUD über `HudElementRegistry` statt
`HudRenderCallback`), `KeyMapping`-Kategorien (1.21.9), `ResourceLocation` heißt ab 1.21.11
`Identifier`. Darum liegt alles Zeichnen hinter einer eigenen `Gfx`-Fassade in `ui/`.

Lokal eingerichtet: Temurin JDK 21.0.13 unter `%USERPROFILE%\.jdks\temurin-21`, MinGit 2.56
unter `%USERPROFILE%\.tools\git`, beides im Benutzer-PATH, `JAVA_HOME` gesetzt. Node 24
vorhanden.

### 2.5 Spätere Versionen

26.1–26.3 (NoRisk-Standard 26.3) kommen als weitere Stonecutter-Ziele, sobald die
1.21.x-Nutzer versorgt sind. Hugo lässt 1.21.4 bis 26.3 zu.

## 3. Architektur

### 3.1 Repo (Monorepo)

```
hugosmp-mod/
  PLAN.md
  CLAUDE.md
  mod/                     Fabric-Mod (Gradle, Java 21, Stonecutter 1.21.5/1.21.8/1.21.10/1.21.11)
    stonecutter.properties.toml   zentrale Versions- und Abhängigkeitsliste
    src/main/java/dev/hugomarket/
      core/                Modul-System, Events, Scheduler, Server-Erkennung
      hook/                Mixins + Fabric-Event-Adapter (dünn)
      container/           Container-Snapshot, Slot-Diff, Screen-Erkennung
      parse/               Lore-/Chat-Parser, Item-Normalisierung
      model/               Domänenobjekte (ItemKey, Listing, Order, Trade, ...)
      store/               In-Memory-Stores, Persistenz, Upload-Queue
      net/                 Backend-Client (async), Cache-Fallback
      ui/                  Overlay-Framework, Widgets, Theme
      module/<feature>/    je Feature ein Paket
      config/              Config-Schema, YACL-Screen, Hotkeys
      i18n/                Sprachumschaltung unabhängig von Spielsprache
    src/test/java/...      JUnit-Tests
    src/test/resources/dumps/   echte Container-/Chat-Dumps (JSON)
  backend/                 Node 24 + TypeScript
    src/api/               Fastify-Routen
    src/ingest/            Validierung, Konsens, Aggregation
    src/db/                Drizzle-Schema, Migrationen
    src/jobs/              Rollups, Retention, Index
    src/bot/               Discord-Bot (Phase 3)
    test/
  web/                     Dashboard (Phase 3), SvelteKit oder Astro, statisch
  shared/                  Item-Key-Spezifikation, Kategorie-Liste, API-Typen (JSON-Schema)
  docs/                    Dump-Format, API, Datenschutz, Impressum-Vorlage
  docker-compose.yml       Postgres + Backend + Caddy
```

### 3.2 Mod: Schichten

```
Hooks (Mixins/Fabric-Events)
  -> ContainerTracker (Slot-Updates, Debounce, Snapshot)
  -> ScreenClassifier (welches Hugo-GUI ist offen: AH-Seite, AH-eigene, Orders, Sell, Shop, Kiste)
  -> Parser (Snapshot -> Listing[] / Order[] / ShopEntry[] / ContainerContents)
  -> Stores (PriceStore, ListingStore, OrderStore, TradeLog, SessionStore)
  -> Module (lesen Stores, zeichnen UI, lösen keine Server-Aktionen aus)
Chat-Hook -> ChatClassifier -> TradeEvent / PayoutEvent / PayEvent / MsgEvent -> Stores
Stores -> UploadQueue -> BackendClient (Thread-Pool) -> Backend
BackendClient -> PriceCache (Datei) -> PriceStore
```

Regeln:

- Hooks enthalten keine Logik, nur Weiterleitung an eine Klasse in `container/`, `parse/` oder `core/`.
- Parser laufen nur bei Slot-Änderung (Debounce 2 Ticks), nie im Render.
- Render-Pfad liest ausschließlich vorberechnete Werte aus Stores (Maps, Arrays). Keine Allokation von Strings/Listen pro Frame, Texte werden beim Store-Update vorformatiert.
- Netzwerk nur über `BackendClient` mit eigenem Executor; Antworten werden auf den Client-Thread gepostet und still in Stores eingesetzt.

### 3.3 Hooks (minimal)

| Zweck | Mechanismus |
|---|---|
| Container-Inhalt / Slot-Updates | Mixin `ClientPacketListener#handleContainerContent`, `#handleContainerSetSlot` (TAIL) → `ContainerTracker.markDirty(containerId)` |
| Container geöffnet/geschlossen | Fabric `ScreenEvents.AFTER_INIT`, `ScreenEvents.remove` |
| Overlay zeichnen | Fabric `ScreenEvents.afterRender(screen)` |
| Maus/Tastatur abfangen | Fabric `ScreenMouseEvents.allowMouseClick/allowMouseScroll`, `ScreenKeyboardEvents.allowKeyPress` (Overlay konsumiert zuerst) |
| Slot-Positionen für Rahmen | Accessor-Mixin auf `AbstractContainerScreen` (`leftPos`, `topPos`, `hoveredSlot`) |
| Tooltips | Fabric `ItemTooltipCallback` |
| Chat (System + Spieler) | Fabric `ClientReceiveMessageEvents.GAME`, `.CHAT`, `ALLOW_GAME` für Chat-Kompakt |
| Chat-Eingabe lesen (`=12*48k`) | Accessor-Mixin auf `ChatScreen.input`, Render über `ScreenEvents.afterRender` |
| Chat-Eingabe setzen (Ergebnis einfügen, Preis-Vorschlag) | `ChatScreen.input.setValue`, nie `sendChat` |
| Tastenkürzel | Fabric `KeyBindingHelper`, Konfliktprüfung beim Binden |
| Slot-Klick (nur Sell-Macro, Phase 3) | `MultiPlayerGameMode#handleInventoryMouseClick`, identisch zum Vanilla-Klickpfad, mit Delays |
| Server-Erkennung | `ServerData.ip` enthält `hugosmp.net`; außerhalb sind alle Module passiv |

### 3.4 Screen-Erkennung

Hugo-GUIs werden über Titel-Muster plus Slot-Signaturen erkannt (z. B. Pfeil-Items auf festen Slots, Titelzeile mit Seitenzahl). Erkennungsregeln liegen in einer Datendatei (`screens.json`), nicht im Code, damit sie nach Dumps ohne Rebuild angepasst werden können. Jede Regel liefert einen `ScreenKind` und Slot-Bereiche (Inhalt, Navigation, Buttons).

### 3.5 Item-Normalisierung (ItemKey)

Eigenes Schema, unabhängig von hugosmp-market.net, aber auf deren Keys abbildbar:

```
<base>[#<variant>]
base    = Minecraft-ID ohne Namespace (minecraft) oder "oraxen:<id>" / "nexo:<id>"
variant = sortierte Merkmale, ";"-getrennt, nur relevante Komponenten
```

Beispiele: `diamond_pickaxe#eff5;unb3;mend`, `spawner#creeper`, `splash_potion#strength2`,
`enchanted_book#prot4`, `shulker_box#` (Inhalt ist kein Key-Merkmal), `oraxen:ruby_sword`.

Quellen pro Merkmal, in dieser Reihenfolge:

1. Item-ID aus dem Stack.
2. Komponenten: `enchantments`, `stored_enchantments`, `potion_contents`, `item_model` oder `custom_model_data` (Custom-Item-Erkennung), `block_entity_data` (Spawner-Typ, falls mitgesendet).
3. Lore-Fallback, wenn der Server Komponenten strippt (Spawner-Typ steht meist im Namen oder in der Lore).
4. `custom_name` nur zur Anzeige, nie als Key-Merkmal, außer bei Custom-Items ohne `item_model`.

Was ins `variant` kommt und was nicht (Haltbarkeit nein, Enchants ja, Anzahl nein, Rename nein) wird in `shared/itemkey.md` festgeschrieben und von Backend und Mod identisch implementiert (Testvektoren in `shared/itemkey-vectors.json`).

Verdächtige Umbenennungen (Vanilla-Item mit Custom-Namen, der wie ein Custom-Item aussieht) werden als `suspicious=true` markiert und nicht hochgeladen.

### 3.6 Preislogik (Mod und Backend identisch, `shared/`)

- Lowest: niedrigster Stückpreis aktiver Listings, Alter ≤ 6 h, mindestens 2 unabhängige Beobachtungen oder 2 Scans desselben Clients im Abstand ≥ 60 s.
- Median: Median der Stückpreise aller aktiven Listings im gleichen Fenster, mit Ausreißerschnitt (× 0,1 und × 10 um den gleitenden 24-h-Median entfernt).
- Stückpreis = Preis / Anzahl; bei Spawnern Preis pro Spawner.
- Trend = Median jetzt vs. Median vor 24 h / 7 d.
- Frische-Ampel: grün ≤ 15 min, gelb ≤ 2 h, rot älter oder nur 1 Beobachter.
- Tippfehler-Warnung: Eingabe < Median/10 oder > Median×10 bei ≥ 3 Beobachtungen.
- Flip-Marge = AH-Lowest × (1 − AH-Verkaufssteuer) − AH-Einstellgebühr − Order-Preis × (1 + Order-Erstellgebühr). Aus Dumps: Order-Erstellgebühr 1 %, AH-Einstellgebühr 1 % (verfällt beim Zurückholen), AH-Verkaufssteuer gestaffelt nach Preis: bis $10.000 1 %, bis $1.000.000 2 %, darüber 3 %, nur bei Verkauf. Sätze liegen in einer Datendatei und kommen später aus `/v1/meta`.
- Server-Verkaufswert: Hugos `Wert:`-Zeile ist Stack-Wert mal persönlichem Multiplikator (z. B. 1.1x). Basiswert = Wert / Multiplikator; für Vergleiche zwischen Spielern zählt der Basiswert.

### 3.7 Lokale Persistenz

Keine SQLite (native Lib, NoRisk-Risiko). Dateien unter `config/hugomarket/`:

| Datei | Inhalt | Format |
|---|---|---|
| `config.json` | Einstellungen | JSON |
| `price-cache.json` | letzte Backend-Antwort pro ItemKey mit Zeitstempel | JSON, atomar geschrieben |
| `upload-queue.jsonl` | ausstehende Beobachtungen | JSONL, append-only, nach Upload gekürzt |
| `trades.jsonl` | Trade-Log (lokal, enthält Klarnamen des Spielers selbst) | JSONL |
| `sessions.json` | Farm-/Sell-Sessions, Minutentests | JSON |
| `templates.json` | Preis-Vorlagen, Watchlist, Favoriten, Presets, Hotkeys | JSON |
| `rtp-points.json` | eigene RTP-Punkte | JSON |
| `dumps/` | Debug-Dumps (Phase 0), standardmäßig aus | JSON |

Schreiben nur vom Client-Thread über einen `PersistenceScheduler` (gebündelt, max. alle 5 s, plus bei Disconnect/Shutdown).

### 3.8 Overlay-Framework (`ui/`)

- `OverlayPanel`: rechteckige Fläche neben/über dem Container-Screen, eigenes Widget-Baum, Hit-Test vor dem Vanilla-Screen. Konsumiert Klicks innerhalb seiner Fläche; alles außerhalb geht unverändert an Vanilla.
- Positionen: links, rechts, unten vom Container; auto-Layout nach Fenstergröße; Skalierung aus Config.
- Widgets: `TextField`, `Button`, `ToggleChip`, `ListView` (virtualisiert, nur sichtbare Zeilen), `ItemRow` (Icon + Text + Preis), `Chart` (Linien, 30 Tage, vorgerechnete Punkte), `NumberPad`.
- Theme: ein `Theme`-Objekt mit Farben, Abständen, Schrift; keine Magic-Numbers in Widgets.
- Eingabefokus: Overlay-Textfeld hält Fokus, Vanilla-Inventar-Tasten (E) werden während Fokus unterdrückt.

### 3.9 Modul-System

```java
interface Module {
  String id();
  boolean enabledByDefault();
  void onEnable(ModuleContext ctx);   // registriert Listener, Keys, Panels
  void onDisable();                   // entfernt alles wieder
}
```

`ModuleRegistry` lädt Module aus einer statischen Liste, prüft Config-Flag und Build-Flag (`-Pmodules.exclude=sellmacro` entfernt Klassen aus dem Jar). Module reden nur über Stores und den Event-Bus miteinander, nie direkt.

### 3.10 Netzwerk

- `BackendClient`: HTTP/2 über `java.net.http.HttpClient`, eigener Executor (2 Threads), Timeout 5 s, Retry mit Backoff, Circuit-Breaker 60 s nach 3 Fehlern.
- Abfragen: Preise gebündelt (`POST /v1/prices/batch` mit bis zu 200 Keys), Antwort in `PriceStore`, Datei-Cache aktualisiert.
- Uploads: `UploadQueue` sammelt Beobachtungen, sendet alle 30 s oder ab 100 Einträgen, komprimiert (gzip). Offline: Queue wächst bis 10 000 Einträge, dann älteste verwerfen.
- Beim Start: `price-cache.json` sofort in `PriceStore`, dann Refresh der Watchlist und der zuletzt gesehenen Keys.
- Identität: zufällige Client-ID (UUID) lokal erzeugt, bei Upload als Header; keine Spielernamen, keine UUIDs des Accounts, keine Koordinaten.
- Versions-Check: `GET /v1/meta` liefert `minClientVersion`; darunter nur lesen, kein Upload, Hinweis im Menü.

## 4. Backend

### 4.1 Stack (Vorschlag)

Node 24 + TypeScript, Fastify, Postgres 17 (TimescaleDB-Extension für Snapshots, optional), Drizzle ORM, Zod-Validierung, Docker Compose, Caddy (TLS). Grund gegen Go: Dashboard, Discord-Bot und Validierung teilen Typen und Item-Key-Code in TS; Node ist beim Nutzer schon im Einsatz.

Hosting: vorhandener Contabo Cloud VPS 6 (NVMe) des Nutzers. Speicherbedarf ist klein: Postgres
und Backend zusammen unter 1 GB RAM, Datenbank im ersten Jahr unter 10 GB bei 30-Tage-Retention
der Rohbeobachtungen. Läuft als eigenes Docker-Compose-Projekt neben allem, was dort schon
liegt; Caddy nur, wenn kein anderer Reverse-Proxy den Port 443 hält (prüfen). Domain fehlt noch.

### 4.2 Endpoints (v1)

| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/v1/meta` | minClientVersion, Serverzeit, Gebührensätze, Kategorien-Version |
| POST | `/v1/observations` | Batch-Upload: Listings, Orders, Shop-Preise, anonyme Volumen-Events |
| POST | `/v1/prices/batch` | Lowest/Median/Trend/Frische für bis zu 200 Keys |
| GET | `/v1/items` | Item-Liste mit Kategorie, Anzeigename, Volumen-Rang (ETag) |
| GET | `/v1/items/:key/history?range=1h..31d` | Zeitreihe für Chart |
| GET | `/v1/categories` | Kategorien + Meta-Ranking |
| GET | `/v1/spawners` | Spawner-Tabelle |
| GET | `/v1/index` | Inflations-Index-Verlauf |
| GET/POST | `/v1/rtp` | RTP-Punkte lesen / beitragen (opt-in) |
| GET | `/v1/contrib/:clientId` | eigener Scan-Beitrag |

### 4.3 Validierung und Konsens

1. Schema (Zod), Client-Version, Rate-Limit pro Client-ID und IP (Token-Bucket).
2. Plausibilität: Preis > 0, Anzahl 1–64 (bzw. Stack-Größe des Items), Restzeit ≤ Maximaldauer, bekannter ItemKey oder neuer Key mit Mindestbelegen.
3. Listing-Identität: Hash aus (seller_hash, item_key, price, amount, end_time auf 1 min gerundet). Gleiche Listing-ID von verschiedenen Clients erhöht `observer_count`.
4. Preis wird "bestätigt", wenn `observer_count ≥ 2` oder derselbe Client es in zwei Scans ≥ 60 s auseinander sah. Nur bestätigte Listings fließen in Lowest/Median.
5. Anti-Manipulation: Client, dessen Beobachtungen in > 20 % der Fälle von niemandem bestätigt werden, bekommt Gewicht 0 (Shadow-Ban, stiller Modus).
6. Verkäufernamen: Client hasht `SHA-256(lowercase(name) + pepper)`; Pepper kommt aus `/v1/meta` und rotiert monatlich. Backend speichert nur den Hash.

### 4.4 Aggregation (Jobs)

- Alle 5 min: aktive Listings pro Key → Snapshot (lowest, median, p25, p75, count).
- Stündlich/täglich: Rollups, Volumen (verschwundene Listings vor Ablauf = verkauft, geschätzt), Meta-Ranking (Volumen × Beobachterzahl, 7-Tage-Fenster).
- Täglich: Inflations-Index (Laspeyres über Kern-Item-Korb, Basis = Monat 1), Retention (Rohbeobachtungen 30 Tage, Listings 90 Tage, Snapshots unbegrenzt).

### 4.5 Recht

Impressum und Datenschutzerklärung als statische Seiten unter derselben Domain; Verarbeitungsverzeichnis in `docs/`. Gespeichert: Client-ID (zufällig), IP in Rate-Limit-Speicher max. 24 h, Verkäufer-Hashes. Kein Login, keine Cookies außer technisch nötig.

## 5. Datenmodell

### 5.1 Backend (Postgres)

```
items            item_key PK, base_id, variant, display_name, category_id, stack_size,
                 first_seen, last_seen, suspicious bool
categories       id PK, name_de, name_en, sort, rule (JSON: match auf base/variant)
listings         listing_id PK, item_key FK, price, amount, unit_price, seller_hash,
                 expires_at, first_seen, last_seen, state (active|gone_early|expired),
                 observer_count, confirmed bool
observations     id, client_id, listing_id, seen_at, page, slot, source
                 (partitioniert nach Tag, 30 Tage)
orders           order_id PK, item_key, side (buy|sell), unit_price, amount_total,
                 amount_open, owner_hash, first_seen, last_seen, state
order_snapshots  item_key, bucket_ts, best_buy, best_sell, depth (JSON), PK (item_key, bucket_ts)
price_snapshots  item_key, bucket_ts, lowest, median, p25, p75, active_count,
                 volume_est, PK (item_key, bucket_ts)   -- 5-min, 1-h, 1-d Tabellen
volume_events    id, item_key, unit_price, amount, kind (ah_sale|order_fill|shop_sell),
                 ts   -- anonym, kein Client-Bezug
shop_prices      item_key, buy_price, sell_price, seen_at
spawner_types    type PK, item_key, display_name
index_baskets    id, name, weights (JSON), base_month
index_values     basket_id, day, value
rtp_points       id, dimension, x, z, y, submitted_at, client_id  (client_id nach 7 Tagen genullt)
clients          client_id PK, first_seen, last_seen, version, weight, obs_count
contributions    client_id, day, observations, confirmed_by_others
```

### 5.2 Mod-lokal

```
ItemKey           base, variant, display, suspicious
Listing           id, itemKey, price, amount, sellerName (nur lokal), sellerHash,
                  expiresAt, page, slot, seenAt
Order             id, itemKey, side, unitPrice, amountTotal, amountOpen, ownerHash, seenAt
PriceInfo         itemKey, lowest, median, p25, p75, trend24h, trend7d, lastSeen, freshness,
                  volume7d, textCache (vorformatiert)
TradeEvent        ts, kind (ah_buy|ah_sell|order_buy|order_sell|shop_sell|pay_in|pay_out),
                  itemKey?, amount?, price, counterparty?, rawLine
PayoutEvent       ts, amount, itemsSold?, rawLine
InventoryDiff     ts, deltas: Map<ItemKey, int>, source (chest|inventory|enderchest)
Session           id, kind (farm|sell|macro), start, end, income, expenses, deltas, payouts
MinuteTest        id, name, ts, baseAmount, payout, multiplier
PriceTemplate     itemKey, price, mode (lowest|median|fixed|minus5)
WatchlistEntry    itemKey, alertUnitPrice?, pinned
RtpPoint          dimension, x, z, y, ts
```

Farm-Tracker, ROI, Sell-Stats und Profit-Tracker lesen alle aus `TradeEvent`, `PayoutEvent`,
`InventoryDiff` und `Session`. Es gibt genau einen `InventoryDiffEngine` (vergleicht
Container-Snapshots beim Öffnen/Schließen und das Spielerinventar bei Änderung) und einen
`ChatTradeParser`.

## 6. Modul-Liste

| Modul-ID | Phase | Default | Server-Aktionen |
|---|---|---|---|
| `dumper` | 0 | aus | keine |
| `ahsearch` | 1 | an | keine (Anzeige, Filter, Markierung) |
| `ahpaging` | 1 (gesperrt) | aus | Seitenklicks, nach Freigabe |
| `sellassist` | 1 | an | setzt Text ins Eingabefeld, Spieler bestätigt |
| `calculator` | 1 | an | keine |
| `pricepanel` | 1 | an | keine; eigenes Modul, abhängig von `calculator` |
| `tooltips` | 1 | an | keine |
| `containervalue` | 1 | an | keine |
| `sync` | 1 | an | nur Backend |
| `market` | 2 | an | keine |
| `orders` | 2 | an | keine |
| `pricealert` | 2 | an | keine (Sound, Karte, Webhook) |
| `myauctions` | 2 | an | keine |
| `sellstats` | 2 | an | keine |
| `farmtracker` | 2 | an | keine |
| `cmdhotkeys` | 2 (nach Rückfrage) | an | sendet Befehl auf Tastendruck des Spielers |
| `sellmacro` | 3 (gesperrt) | aus | Shift-Klicks + Sell-Button, nach Freigabe |
| `minutetest` | 3 | an | keine |
| `spawners` | 3 | an | keine |
| `profit` | 3 | an | keine |
| `inflation` | 3 | an | keine |
| `colorcodes` | 3 | an | keine (Befehl kopieren) |
| `auctions` (Streamer) | 4 | aus | keine; sendet nichts automatisch |
| `giveaways` | 4 | aus | keine |
| `rtpmap` | 4 | aus | keine |
| `tradeoverlay` | entfällt, Hugo hat kein Trade-Fenster (geprüft 2026-11-05) | – | – |
| `hud` | 4 | aus | keine |
| `monitor` | 4 (gesperrt, separater Build) | aus | AH öffnen + Seite 1 lesen, nach Freigabe |

"Gesperrt" = Code vorhanden, aber nur aktivierbar, wenn `/v1/meta` ein Freigabe-Flag liefert
und der Nutzer es lokal einschaltet. So lässt sich die Freigabe zentral zurückziehen.

## 7. Roadmap

### Phase 0 – Analyse (Woche 1)

Ziel: Hugo-Formate kennen, Parser-Testdaten haben.

Dateien:
- `mod/build.gradle.kts`, `gradle.properties`, `fabric.mod.json`, Mixin-Config, Entrypoint
- `core/ServerDetector`, `core/ModuleRegistry`, `core/ClientScheduler`
- `hook/ContainerPacketMixin`, `container/ContainerTracker`, `container/ContainerSnapshot`
- `module/dumper/DumperModule`: schreibt bei jedem Slot-Update eines Hugo-Containers
  `dumps/<ts>-<titel>.json` (Titel, Typ, Slot-Index, Item-ID, Anzahl, Name, Lore, alle
  Komponenten als SNBT), plus `dumps/chat-<datum>.jsonl` (Zeit, Typ system/chat, Rohtext,
  Komponenten-JSON). Hotkey zum Ein-/Ausschalten, Hinweis im HUD wenn aktiv.
- `docs/dump-format.md`
- Backend: nur `docker-compose.yml` mit Postgres, noch kein Code.

Tasks des Nutzers am Dump-Tag: AH alle Kategorien und mehrere Seiten, eigene Auktionen,
Einstell-Dialog komplett durchspielen (bis kurz vor Bestätigung und einmal echt), Orders
(Buch, eigene, erstellen), Shop, Sell-GUI mit Verkauf, Kiste/Enderchest/Shulker, `/pay`
senden und empfangen, `/msg`, AH-Kauf und -Verkauf samt Chat-Meldungen, Order-Fill-Meldung,
Trade-Fenster falls vorhanden, `/rtp`.

Testplan: Mod lädt auf 26.3, Dumps entstehen nur auf hugosmp.net, keine Dumps ohne Hotkey,
keine FPS-Einbrüche beim Blättern (Dump-Schreiben asynchron).

### Phase 1 – MVP (Wochen 2–5)

Ziel: AH-Suche, Verkaufs-Vorschlag, Rechner, Tooltips, Container-Wert, Backend-Sync.

Mod-Dateien:
- `parse/ItemKeyFactory`, `parse/LorePriceParser`, `parse/AhListingParser`,
  `parse/SellGuiParser`, `parse/ScreenClassifier` + `screens.json`
- `model/*` aus 5.2
- `store/PriceStore`, `store/ListingStore`, `store/PersistenceScheduler`, `store/UploadQueue`
- `net/BackendClient`, `net/PriceCacheFile`
- `ui/OverlayPanel`, `ui/widget/*`, `ui/Theme`, `ui/SlotHighlighter`
- `module/ahsearch/`: `AhSearchPanel` (Suchfeld, Filterchips, Sortierung), `ListingIndex`
  (alle gesehenen Listings der aktuellen AH-Sitzung, Volltext über Name/Enchant/Spawner),
  `ResultList` (virtuell, zeigt Seite/Slot), `UnderMarketMarker`, `Watchlist`
- `module/ahpaging/`: `PageWalker` (gesperrt, Delay 1,5–3 s pro Seite, Stop bei Mausbewegung
  über Container, Hotkey)
- `module/sellassist/`: `SellPriceSuggester`, `TypoGuard`, `PriceTemplateStore`,
  Buttons (−5 %, Lowest, Median) im Overlay über dem Einstell-Dialog
- `module/calculator/`: `CalcEngine` (Parser mit K/M/B, Prozent, Klammern), `CalcPanel`,
  `CalcHistory`, `ChatInlineCalc`, Ausgabeaktionen (Kopieren, ins Preisfeld, in den Chat)
- `module/pricepanel/`: `CategoryBar`, `CategoryItemList`, `MetaCategory`, `QuantityButtons`
- `module/tooltips/`: `PriceTooltipProvider` (Lowest, Median, 7-d-Range, Trend, Alter,
  Stückpreis, Order-Wert)
- `module/containervalue/`: `ContainerValuator`, `ShulkerTooltipCache`, Anzeige unter Hugos
  `Wert:`-Zeile als AH-Wert und Order-Wert
- `module/sync/`: `ObservationCollector`, Upload-Timing, Frische-Ampel
- `config/`: YACL-Screen, Mod-Menu-Integration, Hotkeys, Sprache, Menügröße, Feature-Kacheln
- `i18n/`: `de_de.json`, `en_us.json`, Umschalter

Backend-Dateien: Schema (5.1), Migrationen, `/v1/meta`, `/v1/observations`,
`/v1/prices/batch`, `/v1/items`, `/v1/categories`, Konsens-Job, 5-min-Snapshot-Job,
Rate-Limit, Dockerfile, Caddyfile, Impressum/Datenschutz-Seiten.

Shared: `itemkey.md`, `itemkey-vectors.json`, `categories.json` (nach Dumps festgelegt),
`api.schema.json`.

Testplan:
- JUnit: `ItemKeyFactory` gegen Vektoren, alle Parser gegen Dumps (jede Dump-Datei ein
  Testfall, Erwartung als JSON daneben), `CalcEngine` (Präzedenz, K/M/B, Prozent, Fehler),
  `SellPriceSuggester`, `TypoGuard`, `ContainerValuator` (Shulker verschachtelt, unbewertete
  Items), `UploadQueue` (offline, Kürzung).
- Vitest: Konsens, Plausibilität, Rate-Limit, Snapshot-Berechnung, ItemKey-Parität.
- Manuell: Suche filtert live, Klicks außerhalb des Panels erreichen Vanilla, E schließt
  nicht bei Fokus im Suchfeld, Tooltip-Render ohne Allokation (Spark/JFR-Probe), Start ohne
  Netz zeigt Cache-Werte mit Alter.

### Phase 2 – v1 (Wochen 6–9)

- `module/market/`: `MarketScreen` (Liste, Suche, Sortierung, 7-d-Änderung), `ItemDetail`
  (Zeitfenster 1 h–31 d, 30-Tage-Chart), `ChartWidget`
- `module/orders/`: `OrderBookParser`, `SpreadView`, `UndercutNotifier`, `FlipCalculator`
- `module/pricealert/`: `AlertRule`, `AlertEvaluator` (bei jedem AH-Snapshot), Sound, Karte,
  `DiscordWebhook` (optional, URL lokal)
- `module/myauctions/`: `OwnAuctionTracker` aus "eigene Auktionen"-GUI und Chat, Einnahmen
- `module/sellstats/`: `PayoutParser`, `RollingRate` (gleitend 5 min), Session/All-Time/24 h
- `module/farmtracker/`: `InventoryDiffEngine`, `SessionRecorder`, Übersicht
- `module/cmdhotkeys/`: `CommandBinding`, Bestätigungstaste; erst nach Rückfrage, siehe 2.2
- Alle vier 1.21.x-Ziele grün, Jar-Test auf 1.21.6 und 1.21.9 (nicht nur den Build-Versionen)
- Backend: `/history`, Volumen-Schätzung, Rollups, Retention, Order-Snapshots
- Modrinth-Release (Alpha), Anfrage an NoRisk-Team

Tests: Order-Parser und Payout-Parser gegen Dumps, `FlipCalculator` mit Gebühren,
`RollingRate`, `InventoryDiffEngine` (Teil-Stacks, Shulker), Chart-Downsampling.

### Phase 3 – v2 (Wochen 10–14)

- `module/sellmacro/` (gesperrt): `MacroStateMachine` (Open → Fill → Sell → Wait → Reopen),
  `ItemPicker` (Registry, Icons, Suche, Presets), Delays, Optimal-Preset, Abbruch per Hotkey,
  Abbruch bei jedem unerwarteten Screen; Build-Flag `-Pmodules.exclude=sellmacro`
- `module/minutetest/`: 60 s nur mit echten Verkaufs-Ticks, Speichern/Umbenennen,
  `SellMultiCalculator` (Basis = Basismenge)
- `module/spawners/`: Preistabelle, `RoiCalculator` mit Drop-Raten aus Farm-Tracker
- `module/profit/`: Tag/Woche, Netto-Vermögen-Schätzung (Geldstand aus Chat/Actionbar +
  Container-Wert)
- `module/inflation/`: Index-Anzeige
- `module/colorcodes/`: Editor, Zähler (35/30), Templates
- Backend: `/spawners`, `/index`, Dashboard (`web/`), Discord-Bot `/preis`
- Comfort: Chat-Kompakt, letzte Suche merken, Tutorial

Tests: `MacroStateMachine` als reiner Zustandsautomat ohne Minecraft, Minutentest-Zählung,
ROI, Index-Berechnung, Color-Code-Ausgabe.

### Phase 4 – v3 (ab Woche 15)

- Streamer-Auktionen (`/pay`-Parser, Bieter-Fenster, TPA/HERE/Copy, Advertise-Text,
  verdeckte Einnahmen), Giveaways (`/msg`-Stichwort, Reel, Pause/Resume/Cancel/Early-Draw)
- RTP-Map (lokal + opt-in Upload), Minimap-Overlay (aus)
- Item-Check im AH-Tooltip (echtes Custom-Item vs. umbenanntes Vanilla-Item)
- HUD-Ticker, Geld-Verlauf
- Monitoring-Account: separater Gradle-Build `monitor` ohne UI-Module, Headless-Fabric auf
  VPS, nur erste AH-Seite "neu gelistet", Refresh 30–60 s, nach Freigabe

## 8. Performance-Vorgaben

- Render: nur Lesen aus Stores, vorformatierte `Component`-Objekte, keine Streams, keine
  String-Konkatenation, keine Lambdas mit Capture pro Frame.
- Parser: nur bei `markDirty`, pro Container maximal einmal je 2 Ticks, Dauer < 1 ms für
  54 Slots (Messung im Test).
- Tooltips: Preis-Lookup O(1) über ItemKey-Cache am Stack (WeakHashMap auf Stack-Identität
  plus Komponenten-Hash), Shulker-Wert gecacht bis Komponenten-Hash sich ändert.
- Netzwerk: nie auf dem Client-Thread; Antworten über `ClientScheduler.post`.
- Dateien: gebündelt, atomar (tmp + rename), nie im Render.

## 9. Regelkonformität

| Aktion | Status |
|---|---|
| Lesen von GUIs, Lore, Chat | immer |
| Overlay-UI, Filter, Markierungen, Tooltips | immer |
| Text ins Chat-/Anvil-Feld setzen (Spieler sendet) | immer |
| Befehl auf Tastendruck senden (Command-Hotkeys) | nach Rückfrage (FWM hat es entfernt) |
| AH-Seiten automatisch blättern | nur nach Freigabe |
| Sell-Macro | nur nach Freigabe, Default aus, ausbaubar |
| Monitoring-Account | nur nach Freigabe |
| Auto-Kauf, Sniping, Order-Automatik, Pakete fälschen | nie |

Freigabe-Flags kommen aus `/v1/meta` und werden zusätzlich lokal bestätigt. Ohne Flag sind
die Module nicht aktivierbar, auch nicht per Config-Datei.

## 10. Risiken

- Hugo strippt Komponenten aus GUI-Items (nur Name/Lore sichtbar): dann hängt die
  Normalisierung an Lore-Regex. Phase-0-Dumps klären das zuerst.
- AH-Einstellen läuft über Chat-Eingabe, Anvil oder Sign: bestimmt, wie der Preis-Vorschlag
  eingesetzt wird. Alle drei Pfade sind ohne Auto-Senden umsetzbar.
- Kaltstart: ohne Nutzer keine Preise. Gegenmaßnahme: AH-Features funktionieren aus der
  eigenen Sitzung (lokaler `ListingIndex`) ohne Backend; Backend-Daten verbessern nur.
- FWM-Reaktion: closed source, 23K Downloads, wöchentliche Releases. Unser Vorteil muss die
  AH-Bedienung und Offenheit sein, nicht Feature-Zahl.
- Vier Build-Ziele von Anfang an: jede UI-Änderung muss gegen 1.21.5 und 1.21.11 kompilieren.
  Die `Gfx`-Fassade und `gradlew build` über alle Ziele vor jedem Commit halten das klein.
- Rechtlich: Verkäufer-Hashes und Impressum reichen voraussichtlich, aber kein Rechtsrat.

## 11. Vorab klären

Hugo-Team:
1. Ist ein lesender Client-Mod mit Overlay erlaubt (Vergleich FWM, Tweaks)?
2. Sell-Macro erlaubt? Unter welchen Bedingungen (Delays, Hotkey-Abbruch)?
3. Automatisches AH-Blättern erlaubt?
4. Monitoring-Zweitaccount auf VPS erlaubt?
5. Command-Hotkeys erlaubt (FWM hat sie entfernt)?
6. Gebührensätze: AH-Einstellgebühr, AH-Verkaufssteuer, Order-Gebühr, Shop-Spanne.

Im Spiel feststellen (Phase 0), Stand 2026-11-05 nach erstem Dump-Tag (`docs/hugo-formats.md`):
7. Geklärt: kein Trade-Fenster. Trade-Overlay und Scam-Warnung entfallen; der Item-Check
   (echtes Custom-Item vs. umbenanntes Vanilla-Item) bleibt als Tooltip-Hinweis im AH.
8. Geklärt: Sell-GUI 9x4 mit Verkaufen-Button, Einstell-Dialog mit Anvil-Preiseingabe,
   Orders sind reine Kauf-Orders mit 1 % Erstellgebühr, AH hat eigene Suche, Sortierung
   (neu/alt/billig/teuer) und Stack-Filter. Shop-GUI offen.
9. AH hat keine Kategorien, nur eine Liste mit Suche. Custom-Items noch nicht gesehen.
10. Geklärt: GUI-Items tragen alle Komponenten (Enchants, Custom-Namen), Hugo-Daten liegen in
    `custom_data.PublicBukkitValues`. Steuer-Items haben stabile `custom_model_data`-IDs.
11. Payout geklärt (`+$159,16 (Basis: $144,80, Bonus: +$14,36 durch 1.1x)`), Rest offen:
    AH-Kauf/-Verkauf, Order-Fill, `/pay`, `/msg`, Geldstand (keine Actionbar).
12. Auktionsdauer 24 h (Restzeiten bis 23h 56m gesehen). Gebühren vollständig bekannt
    (siehe 3.6). Listing-Limit offen.

Infrastruktur:
13. Domain, Modrinth-Account, GitHub-Repo, endgültiger Mod-Name (Arbeitstitel `hugomarket`).
14. Welcher Launcher für 1.21.5 (Lunar-Profil vorhanden; Fabric-Jar braucht NoRisk, Prism
    oder Vanilla-Fabric).

Entschieden am 2026-11-05: MC 1.21.5–1.21.11, Node/TS-Backend auf dem Contabo-VPS, Open Source
unter MIT, Phase 0 startet ohne Freigabe (nur Lesen). Anfrage ans Hugo-Team läuft parallel.
