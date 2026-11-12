# Changelog

## 0.1.2 – 2026-11-12

- Spawner-Börse (Knopf „Spawner“ über dem Inventar): Angebote aller Mod-Nutzer pro Spawner-Typ,
  eigene Angebote zum Kaufen oder Verkaufen, Annehmen oder Gegenangebot, Verhandlung über
  „Meine Deals“, Sterne-Bewertung nach jedem Deal, Hinweise im Chat bei neuen Anfragen.
  Anmeldung einmalig über Mojang, das Login-Token geht nur an Mojang
- Richtpreise pro Spawner-Typ, Deal-Rechner mit Abweichung vom Richtpreis, deutscher Chat-Text zum Kopieren
- Gestackte Spawner zählen mit ihrer echten Anzahl (Kistenwert, Vermögen, Tooltip)
- Gefüllte Shulker werden nach ihrem Inhalt bewertet statt nach Kit-Preisen
- AH-Seitenleiste: durchsichtig über die volle Höhe, Angebote aller Nutzer, Filter Schnäppchen,
  7-Tage-Verlauf beim Hover, Flip-Gewinn im Tooltip
- Klick auf ein Angebot sucht es im AH (`/ah <Item>`, sortiert, blättert bis zu 15 Seiten)
- Schnäppchen-Finder mit Order-Flips, Watchlist-Seite mit Alarm bei neuen Schnäppchen
- HUD-Box mit Tagesgewinn, Vermögen und Watchlist, frei verschiebbar
- Verkaufsliste: ganzes Inventar mit Preisvorschlag
- Profit-Diagramm über 14 Tage, Status „Heute · Vermögen“ neben dem Menü-Knopf
- Median nach Stückzahl gewichtet, sehr billige Angebote markiert statt versteckt
- Verzauberter goldener Apfel bekommt einen eigenen Preis
- Fehlerbehebungen beim Preis-Upload

## 0.1.1 – 2026-11-05

- Neue Optik: Kopfzeilen mit Titel, Schatten, zentrierte Knöpfe, abwechselnd schattierte Listen
- Menü als Dashboard: Banner, Kacheln mit Beschreibung, Module in Gruppen mit Erklärung,
  Taste J, Hugonomics-Knopf über jedem Container (Inventar, AH, Kisten, Shop)
- Übersicht mit Reitern und Pfeiltasten
- Rechner standardmäßig offen, N blendet aus und ein (wird gemerkt)
- Profit: AH-Erlöse aus Actionbar-Meldungen und aus den AH-Gesamtzahlen, Käufe werden nachgetragen
- Vermögen: Kontostand (Sidebar oder /money), Inventar, Enderchest, laufende Auktionen
- Farbcode-Editor für /rename und /sign mit Live-Vorschau im Chat
- Preis-Upload gedrosselt (ein Listing höchstens einmal pro Minute)
- Monitor-Modul für einen eigenen Zweitaccount (Account-gebunden, serverseitig freischaltbar)
- Spawner-Filter entfernt (gibt es auf HugoSMP nicht)

## 0.1.0 – 2026-11-05

Erste öffentliche Version.

- Auktionshaus: Suche, Sortierung, Filter, Watchlist, Schnäppchen-Rahmen, eigene Auktionen
- Einstellen: Preis-Vorschlag (Lowest, Median, −5 %, eigener Preis), Gebühren-Vorschau,
  Tippfehler-Warnung
- Tooltips: Stückpreis, AH-Preise, Order-Preis, Shulker-Inhaltswert
- Kisten: Kistenwert, Farm-Tracker mit Zulauf pro Minute
- Orders: Flip-Marge und Markierung lohnender Orders
- Verkaufsmenü: Verkaufs-Statistik, Minutentest mit Sellmulti-Tabelle
- Markt-Screen mit Preis-Historie und Preis-Alarmen
- Trade-Log und Profit-Übersicht
- Rechner (Taste N) und Chat-Kurzform `=…`
- Menü mit Übersicht und Modul-Schaltern, Mod-Menu-Integration
- Gemeinsamer Preis-Server mit anonymen Beobachtungen
