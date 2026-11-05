# Anfrage an das HugoSMP-Team (Entwurf)

Kanal: Ticket oder Mod-Kontakt im HugoSMP-Discord. Vor dem Senden Name der Mod und den
Link zum Repo eintragen.

---

Hallo HugoSMP-Team,

ich entwickle eine Open-Source-Client-Mod (Fabric, MIT-Lizenz) für HugoSMP mit Fokus auf
Auktionshaus, Orders und Marktpreise, vergleichbar mit dem FWM Mod und HugoSMP Tweaks.
Bevor ich etwas veröffentliche, möchte ich klären, was aus eurer Sicht in Ordnung ist.

Was die Mod macht:

- Sie liest nur, was der Client ohnehin sieht: Container-Inhalte (AH, Orders, Shop, Kisten),
  Item-Namen und Lore sowie Chat-Meldungen. Keine Serverabfragen, keine Pakete außer den
  normalen Klicks des Spielers.
- Sie zeigt Zusatzinformationen an: Suchfeld und Filter über dem AH, Preisvorschlag beim
  Einstellen (der Spieler bestätigt selbst), Markt-Preise im Tooltip, Kisten-Gesamtwert,
  Taschenrechner, Statistiken aus den eigenen Verkaufsmeldungen.
- Preisdaten werden anonym auf einen eigenen Server hochgeladen und allen Nutzern bereitgestellt.
  Verkäufernamen werden vor dem Upload gehasht, Koordinaten und Accountdaten werden nicht
  gesendet.
- Kein Auto-Kauf, kein Sniping, keine automatischen Aktionen im AH oder bei Orders.

Dazu meine Fragen:

1. Ist eine solche rein lesende Mod mit Overlay-Anzeige erlaubt?
2. Der FWM Mod bietet ein Sell-Macro (Items per Shift-Klick ins Verkaufs-GUI, Sell-Button,
   Neuöffnen mit Verzögerung). Ist das erlaubt? Falls ja: unter welchen Bedingungen
   (Verzögerungen, Abbruch per Taste, nur solange der Spieler am Rechner ist)?
3. Ist automatisches Blättern durch AH-Seiten erlaubt, wenn es langsam läuft (1,5 bis 3
   Sekunden pro Seite), per Taste gestartet wird und bei jeder Mausbewegung stoppt?
4. Sind Command-Hotkeys erlaubt (eine Taste sendet einen Befehl wie /spawn)?
5. Wäre ein Zweitaccount auf einem Server erlaubt, der alle 30 bis 60 Sekunden nur die erste
   AH-Seite öffnet, um Preise aktuell zu halten? Falls nein, lasse ich das weg.
6. Welche Gebühren gelten im AH (Einstellgebühr, Verkaufssteuer) und bei Orders? Die Mod
   soll Margen korrekt berechnen.

Alles, was ihr nicht freigebt, baue ich nicht ein oder liefere es nur abschaltbar und
standardmäßig deaktiviert aus. Den Quellcode könnt ihr jederzeit einsehen: <Repo-Link>.

Danke und viele Grüße
<Name / Ingame-Name>
