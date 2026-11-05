# Hugonomics

Client-Mod für HugoSMP (Fabric, Minecraft 1.21.5 bis 1.21.11) mit eigenem Backend: Suche und
Filter im Auktionshaus, Verkaufs-Vorschlag mit Gebühren, Preis-Tooltips, Kisten- und
Shulker-Wert, Orders mit Flip-Marge, Markt-Übersicht mit Charts, Preis-Alarme, Rechner,
Verkaufs-Statistik, Minutentest, Farm-Tracker, Trade-Log und Profit.

Die Mod liest nur, was der Client ohnehin sieht. Sie kauft nicht automatisch, blättert nicht
automatisch und sendet keine Klicks ohne den Spieler. Gesehene AH-Preise werden anonym an den
Hugonomics-Server gemeldet und mit allen Nutzern geteilt; Verkäufernamen verlassen den Client
nur gehasht, Koordinaten und Accountdaten gar nicht. Das lässt sich im Menü abschalten.

## Installation

Fabric Loader 0.16 oder neuer, Fabric API, dazu die Jar für die eigene Minecraft-Version in den
`mods`-Ordner. Mod Menu ist optional und öffnet das Hugonomics-Menü über das Zahnrad.

## Bedienung

Mod Menu → Hugonomics → Übersicht listet jede Funktion mit Ort und Taste. Kurzfassung:

- Auktionshaus: Such-Panel rechts, Rahmen für Treffer, Schnäppchen und Watchlist, Übersicht
  der eigenen Auktionen.
- Amboss beim Einstellen: Lowest, Median, eigener letzter Preis, Gebühren-Vorschau.
- Orders: Flip-Marge pro Order.
- Kisten: Kistenwert, Zulauf pro Minute, Farm-Session.
- Verkaufsmenü: Verkaufs-Statistik und Minutentest.
- Menü: Markt mit Charts und Preis-Alarmen, Profit, Module an/aus.
- Taste N: Rechner. Im Chat `=12*48k` für Live-Ergebnisse.

## Entwicklung

Siehe `CONTRIBUTING.md` für Build-Befehle und Code-Regeln, `PLAN.md` für Architektur und Roadmap,
`docs/` für Hugo-Formate und Deployment.

```
cd mod
.\gradlew build
```

Backend:

```
cd backend
npm install
npm test
```

## Lizenz

MIT, siehe `LICENSE`. Hugonomics ist kein offizielles Projekt von HugoSMP.
