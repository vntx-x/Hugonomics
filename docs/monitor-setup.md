# Monitor-Account einrichten

Ein eigener Minecraft-Account, der nichts anderes tut, als regelmäßig die erste AH-Seite zu
öffnen und zu melden. Risiko: das ist Automatisierung von Befehlen; ob HugoSMP das duldet, ist
nicht geklärt (Ticket an das Team läuft). Nur mit einem Account machen, dessen Verlust zu
verschmerzen wäre, und nicht mit dem Hauptaccount auf demselben Rechner gleichzeitig spielen.

## Variante A: gleiches Profil, zweiter Account (empfohlen)

Das Modul ist an einen Accountnamen gebunden und bleibt auf jedem anderen Account passiv.
Deshalb reicht das normale Profil:

1. Im NoRisk Launcher den Zweitaccount hinzufügen.
2. In `config/hugomarket/config.json` des Profils setzen:

```json
"modules": { "monitor": true },
"monitorAccount": "KOVAAV",
"monitorIntervalSeconds": 45
```

3. Profil mit dem Zweitaccount starten, auf Hugo verbinden, an einen ruhigen Ort stellen
   (eigenes Grundstück, AFK-Zone). Alle 45 Sekunden öffnet die Mod `/ah`, wartet auf die
   Seite, meldet sie und schließt sie. Der Server-Flag `monitor` muss auf `true` stehen.
4. Hauptaccount wie gewohnt aus demselben Profil starten; dort tut das Modul nichts.

Läuft das Profil zweimal gleichzeitig, teilen sich beide Instanzen dieselbe Config und
Client-ID. Falls der Launcher einen zweiten Start desselben Profils verweigert, ein zweites
Profil anlegen und dort dieselbe Jar plus dieselbe Config-Zeile verwenden.

Der Hauptaccount profitiert sofort: alles, was der Monitor meldet, bestätigt die eigenen
Beobachtungen (zweiter Client), und neue Listings tauchen innerhalb einer Minute im
Markt-Screen auf.

## Variante B: auf dem VPS (läuft seit 2026-11-05)

Headless-Client in Docker, Anleitung in `monitor/README.md`. Gemessen: etwa 1,2 GB RAM und
ein Drittel Kern im Betrieb, Login aus dem Rechenzentrum funktioniert. Wichtig: Der
Monitor-Account darf währenddessen nirgendwo sonst eingeloggt sein, ein zweiter Login
desselben Accounts bleibt beim Beitritt stumm hängen und der Container startet dann alle
90 s neu. Statusdatei im Container: `/home/mc/data/work/config/hugomarket/monitor.log`.

## Abschalten

Modul `monitor` im Menü aus oder auf dem Server `MONITOR_FLAG=false` in der `.env` und
`docker compose up -d` – dann stellen alle Monitor-Clients beim nächsten Meta-Abruf (spätestens
nach 5 Minuten) den Betrieb ein.
