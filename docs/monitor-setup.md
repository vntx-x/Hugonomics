# Monitor-Account einrichten

Ein eigener Minecraft-Account, der nichts anderes tut, als regelmäßig die erste AH-Seite zu
öffnen und zu melden. Risiko: das ist Automatisierung von Befehlen; ob HugoSMP das duldet, ist
nicht geklärt (Ticket an das Team läuft). Nur mit einem Account machen, dessen Verlust zu
verschmerzen wäre, und nicht mit dem Hauptaccount auf demselben Rechner gleichzeitig spielen.

## Variante A: zweites Profil auf PC oder Mac (empfohlen)

1. Im NoRisk Launcher den Zweitaccount hinzufügen und ein eigenes Profil "NRC 1.21.5 Monitor"
   anlegen, Fabric API drin lassen.
2. `hugomarket-0.1.0+1.21.5.jar` in den `custom_mods`-Ordner dieses Profils legen.
3. Einmal starten, auf Hugo verbinden, wieder beenden. Dann liegt
   `config/hugomarket/config.json` im Profilordner.
4. In dieser Datei setzen:

```json
"modules": { "monitor": true, "ahsearch": false, "calculator": false, "tooltips": false,
             "containervalue": false, "farm": false, "sellstats": false, "minutetest": false,
             "alerts": false, "orders": false, "myauctions": false, "trades": false },
"monitorIntervalSeconds": 45
```

5. Starten, auf Hugo verbinden, an einen ruhigen Ort stellen (eigenes Grundstück, AFK-Zone).
   Alle 45 Sekunden öffnet die Mod `/ah`, wartet auf die Seite, meldet sie und schließt sie.
   Der Server-Flag `monitor` steht auf `true`, sonst bleibt das Modul passiv.

Der Hauptaccount profitiert sofort: alles, was der Monitor meldet, bestätigt die eigenen
Beobachtungen (zweiter Client), und neue Listings tauchen innerhalb einer Minute im
Markt-Screen auf.

## Variante B: auf dem VPS

Braucht einen virtuellen Bildschirm (Xvfb), Software-OpenGL (Mesa llvmpipe), einen Fabric-
Client mit Login per Microsoft-Token und etwa 2 GB RAM plus einen Kern dauerhaft. Machbar,
aber Wartungsaufwand bei jedem Minecraft-Update. Erst sinnvoll, wenn PC und Mac nicht
durchgehend laufen sollen.

## Abschalten

Modul `monitor` im Menü aus oder auf dem Server `MONITOR_FLAG=false` in der `.env` und
`docker compose up -d` – dann stellen alle Monitor-Clients beim nächsten Meta-Abruf (spätestens
nach 5 Minuten) den Betrieb ein.
