# Monitor-Client auf dem VPS

Ein echter Fabric-Client ohne Bildschirm (Xvfb + Mesa llvmpipe) in Docker, der mit dem
Monitor-Account auf HugoSMP steht und über das Monitor-Modul der Mod regelmäßig die erste
AH-Seite meldet. Nur für einen eigenen Zweitaccount gedacht. Das Modul läuft ausschließlich,
wenn `monitorAccount` zum eingeloggten Account passt und der Preis-Server das Flag
`monitor` setzt.

## Einmalig auf dem PC

```
pip install portablemc
portablemc login <microsoft-mail des monitor-accounts>
```

Danach liegt `%USERPROFILE%\.portablemc\portablemc_auth.json` vor. Diese Datei (und nur
diese) nach `monitor/data/main/portablemc_auth.json` auf den VPS kopieren. Sie enthält
ein Refresh-Token, kein Passwort.

## Auf dem VPS

```
cd ~/hugomarket/monitor
mkdir -p data/main mods
# fabric-api-<version>+1.21.5.jar und hugomarket-<version>+1.21.5.jar nach mods/
# config.json: clientId auf eine neue UUID setzen, monitorAccount prüfen
printf 'MC_LOGIN=<microsoft-mail>\nMC_UID=%s\n' "$(id -u)" > .env
docker compose up -d --build
docker compose logs -f
```

Erster Start lädt Assets und Fabric (einige Minuten). Danach verbindet der Client direkt
mit `hugosmp.net`. Bricht die Verbindung ab, beendet die Mod den Client nach 60 s und
Docker startet ihn neu (`restart: unless-stopped`).

## Ressourcen

Speicher 2,8 GB Limit (Java 2 GB Heap), CPU 1,5 Kerne, Sichtweite 2, 10 FPS, Ton aus,
640x360. Erwartung: 1,5 bis 2,2 GB RAM, 0,5 bis 1 Kern dauerhaft.

## Abschalten

`docker compose down` auf dem VPS oder `MONITOR_FLAG=false` in der Backend-`.env` (dann
bleibt der Client verbunden, öffnet aber nichts mehr).
