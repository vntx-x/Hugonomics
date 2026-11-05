# Backend auf dem VPS

Ziel: `backend/` als Docker-Compose-Projekt auf dem Contabo-VPS, hinter einem Reverse-Proxy
mit TLS.

## Ist-Zustand (2026-11-05)

Deployt unter `~/hugomarket` des Deploy-Benutzers (Compose-Projekt `hugomarket`, Dateien in
`app/`, Secrets in `.env` mit Rechten 600, nicht im Repo). Öffentlich unter
`https://hugomarket.duckdns.org` über den Coolify-Traefik (Netz `coolify`, Zertifikat von
Let's Encrypt, HTTP leitet auf HTTPS um). DNS: kostenloser DuckDNS-Eintrag. Zusätzlich hört
die API nur auf einer privaten Netzadresse (`API_BIND`), Postgres nur auf `127.0.0.1:5432`.
SSH ist von außen geschlossen; Benutzer, Adresse und Schlüssel stehen in der lokalen,
nicht eingecheckten Datei `docs/deploy.local.md`.

Update von Windows aus (`<user>@<host>` aus `deploy.local.md`):

```
cd backend
tar -czf backend.tar.gz --exclude=node_modules --exclude=dist --exclude=.env .
scp backend.tar.gz <user>@<host>:~/hugomarket/
scp deploy/remote-deploy.sh <user>@<host>:~/hugomarket/
ssh <user>@<host> "API_BIND=<private-ip> API_PORT=8081 API_HOST=hugomarket.duckdns.org ~/hugomarket/remote-deploy.sh"
```

`remote-deploy.sh` entpackt, baut, startet, wartet auf den Healthcheck und führt die
Migration aus. Mit gesetztem `API_HOST` hängt es zusätzlich die Traefik-Override-Datei ein.

## Ressourcen

Die Compose-Datei begrenzt Postgres auf 384 MB und die API auf 192 MB RAM. Realer Verbrauch
im Leerlauf: Postgres 60–120 MB, API 60–90 MB, zusammen unter 250 MB. CPU im Normalbetrieb
nahe null, Spitzen beim Upload-Burst weit unter einem Kern.

Platte: ein Listing-Eintrag belegt etwa 300 Bytes, eine Beobachtung etwa 80 Bytes. Bei
10 000 neuen Listings und 100 000 Beobachtungen am Tag sind das rund 11 MB pro Tag, mit
Index und Postgres-Overhead etwa das Doppelte. Mit der geplanten Retention (Beobachtungen
30 Tage, Listings 90 Tage) bleibt die Datenbank unter 2 GB; die Docker-Images brauchen
einmalig etwa 400 MB. Der Retention-Job ist noch nicht gebaut, bis dahin wächst die
Datenbank linear.

## Einmalig

1. Docker und Docker Compose auf dem VPS installieren (Debian/Ubuntu: `apt install docker.io
   docker-compose-plugin`).
2. Repo klonen oder nur `backend/` kopieren: `scp -r backend/ user@vps:/opt/hugomarket/`.
3. `.env` aus `.env.example` anlegen, `POSTGRES_PASSWORD` und `PEPPER` mit langen Zufallswerten
   füllen (`openssl rand -hex 32`). Der Pepper darf später nicht mehr wechseln, sonst passen
   alte Verkäufer-Hashes nicht mehr zu neuen.
4. Starten und Tabellen anlegen:

```
cd /opt/hugomarket/backend
docker compose up -d --build
docker compose exec api node dist/db/migrate.js
curl -s localhost:8080/healthz
curl -s localhost:8080/v1/meta
```

`migrate.js` ist idempotent und kann nach jedem Update erneut laufen.

DuckDNS: `~/hugomarket/duckdns.sh` (Token, Domain und IP stehen in der `.env` auf dem Server)
läuft täglich um 04:17 per Crontab des Deploy-Benutzers und hält den Eintrag frisch; Log in
`~/hugomarket/duckdns.log`.

## Reverse-Proxy

Die API hört standardmäßig nur auf `${API_BIND}:${API_PORT}` (Default `127.0.0.1:8080`).
Auf dem Contabo-VPS läuft Coolify mit Traefik auf 80/443 (Netz `coolify`, Zertifikate über
den Resolver `letsencrypt`). Dafür liegt `docker-compose.traefik.yml` bereit: `API_HOST` in
`.env` setzen, DNS-A-Record auf die Server-IP zeigen lassen, dann

```
docker compose -f docker-compose.yml -f docker-compose.traefik.yml up -d
```

Traefik holt das Zertifikat selbst. Bis dahin ist die API nur im privaten Netz erreichbar
(`API_BIND=<private-ip>`, `API_PORT=8081`, weil 8080 dort schon belegt ist).

## Mod anbinden

In `config/hugomarket/config.json` des Spielprofils:

```json
"backendUrl": "https://api.<domain>",
"modules": { "sync": true }
```

Danach Minecraft neu starten. Der Client holt `/v1/meta` (Pepper, Mindestversion,
Gebühren), lädt alle 30 s gesehene AH-Seiten hoch und fragt alle 15 s Preise für Items ab,
die lokal noch keinen Preis haben. Antworten landen in `price-cache.json` und stehen nach
dem Neustart sofort bereit.

## Updates

```
git pull
docker compose up -d --build
docker compose exec api node dist/db/migrate.js
```

## Daten

- `listings`: ein Eintrag pro gesehener Auktion (Identität aus Verkäufer-Hash, ItemKey,
  Anzahl, Preis und Ablaufzeit), mit Beobachtern und Bestätigungsstatus.
- `observations`: wer wann welches Listing gemeldet hat, Client-ID ist eine zufällige UUID.
- `clients`: Version und Zähler pro Client-ID.

Keine Spielernamen, keine Account-UUIDs, keine Koordinaten. Impressum und
Datenschutzerklärung müssen vor dem öffentlichen Betrieb auf derselben Domain stehen.
