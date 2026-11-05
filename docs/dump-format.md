# Dump-Format (Phase 0)

Das Modul `dumper` schreibt nach `<minecraft>/config/hugomarket/dumps/`. Aufzeichnung startet
erst nach Druck auf die Dump-Taste (Standard F8, änderbar unter Steuerung → HugoMarket) und
nur auf `hugosmp.net`. Solange aufgezeichnet wird, steht oben links rot "HugoMarket DUMP".

## Container: `containers/<yyyyMMdd-HHmmss-SSS>_<Titel>.json`

Eine Datei pro Inhaltsänderung eines geöffneten Containers (Debounce 2 Ticks, identische
Inhalte werden nicht doppelt geschrieben).

```json
{
  "ts": "2026-11-05T14:03:09.007Z",
  "server": "hugosmp.net",
  "minecraft": "1.21.5",
  "mod": "0.0.1+1.21.5",
  "title": "Auktionshaus (1/12)",
  "titleJson": { "text": "..." },
  "menuType": "ChestMenu",
  "containerId": 7,
  "slotCount": 90,
  "slots": [
    {
      "index": 0,
      "player": false,
      "id": "minecraft:spawner",
      "count": 1,
      "name": "Creeper Spawner",
      "nameJson": { "text": "..." },
      "lore": ["Preis: 1.250.000$", "Verkäufer: seller_1", "..."],
      "tooltip": ["Creeper Spawner", "Preis: 1.250.000$", "..."],
      "stack": { "id": "minecraft:spawner", "count": 1, "components": { "...": "..." } }
    }
  ]
}
```

- `slots` enthält nur belegte Slots. `player: true` markiert Slots des Spielerinventars.
- `lore` und `tooltip` sind reine Strings ohne Formatierung; Farben und Klick-/Hover-Events
  stehen in `stack.components` (Lore) und `nameJson`.
- `stack` ist die vollständige Item-Kodierung inklusive aller Komponenten, so wie der Server
  sie geschickt hat.

## Chat: `chat/chat-<yyyyMMdd>.jsonl`

Eine Zeile pro Nachricht.

```json
{"ts":"2026-11-05T14:03:10.120Z","kind":"system","text":"Du hast 3x Diamant für 15.000$ verkauft.","json":{"text":"..."}}
{"ts":"2026-11-05T14:03:11.000Z","kind":"chat","sender":"Spieler","text":"...","json":{"...":"..."}}
{"ts":"2026-11-05T14:03:11.500Z","kind":"actionbar","text":"Geld: 1.234.567$","json":{"...":"..."}}
```

`kind` ist `system` (Servermeldungen, Befehlsantworten, Payouts), `chat` (signierte
Spielernachrichten mit `sender`) oder `actionbar`.

## Vor dem Einchecken als Testdaten

Verkäufer- und Spielernamen durch `seller_N` beziehungsweise `player_N` ersetzen, eigene
Kontostände dürfen bleiben. Dateien nach `mod/src/test/resources/dumps/<bereich>/` legen und
daneben eine `<name>.expected.json` mit dem erwarteten Parser-Ergebnis.
