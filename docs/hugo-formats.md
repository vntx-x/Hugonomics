# Hugo-Formate (aus Dumps vom 2026-11-05, Client 1.21.5, Spielsprache Englisch)

Alle Hugo-GUIs sind Chest-Menüs mit Steuer-Items aus `minecraft:paper`, die über
`minecraft:item_model` (`hugosmp:…`) und `minecraft:custom_model_data.floats[0]` identifiziert
werden. Die Float-ID ist stabil pro Funktion und über GUIs hinweg gleich, die Titel bestehen
aus zwei Zeichen einer eigenen Schriftart: immer `U+F80E` (negativer Abstand) gefolgt vom
GUI-Glyph (unten angegeben). Jedes Item eines Hugo-GUIs trägt
`custom_data.PublicBukkitValues["minecraft:secured"]` mit dem GUI-Bereich (`auction`, `order`,
`input`). Vanilla-Kisten haben das nicht.

## Steuer-Items (custom_model_data)

| ID | Name | Vorkommen |
|---|---|---|
| 20000 | Suche | AH, Orders, Order-Itemwahl |
| 20001 | Vorherige Seite | Listen |
| 20002 | Nächste Seite | Listen |
| 20003 | Filter | AH (Sortierung + Stack-Filter), Orders (Sortierung) |
| 20004 | Auktion erstellen / Order erstellen | AH, Orders |
| 20005 | Aktualisieren | AH, Orders |
| 20006 | Deine Auktionen / Deine Orders | AH, Orders |
| 20007 | Zurück | Order-Itemwahl |
| 20009 | Bestätigen / Order aufgeben / Verkaufen | Anvil, Order-Bestätigung, Sell-GUI |
| 20010 | Erstellgebühr (Info) | Order-Bestätigung |
| 20011 | Abbrechen / Zurück | Bestätigungsdialoge |
| 20014 | Preis festlegen | AH-Einstell-Dialog |
| 80017 | Eingabe-Item (leerer Name) | Anvil-Eingabe |

## GUIs

### AH-Liste: Titel `ꐭ` (U+A42D), `generic_9x6`, secured `auction`

- Slots 0–44: Listings. Slot 45 Aktualisieren, 46 Suche, 48 Vorherige Seite, 49 Filter,
  50 Nächste Seite, 52 Deine Auktionen, 53 Auktion erstellen. 47 und 51 leer.
- Listing-Lore (Strings): `""`, `Preis: $23.323`, `Verkäufer: Teufels_Affe_YT`,
  `Läuft ab in: 10h 43m 26s`. Dieselben Zeilen stehen roh in
  `PublicBukkitValues["minecraft:display_extra_lore"]` mit §-Codes.
- Der Stack ist das echte Item mit allen Komponenten (Enchants, Custom-Namen), plus
  `custom_data`. Preis gilt für den ganzen Stack (`count`).
- Filter-Lore: Sortierung `Neueste Auktionen / Älteste Auktionen / Niedrigster Preis /
  Höchster Preis`, Stack-Filter `Alle / Nur ganze Stacks / Nur einzelne Items`, aktive Option
  mit `> ` markiert. Linksklick Sortierung, Rechtsklick Stack-Filter.
- "Deine Auktionen"-Lore: Ansichten `Alle / Laufende / Verkaufte / Gekaufte / Abgelaufene`,
  dazu `Insgesamt ausgegeben: $7.454.495`, `Insgesamt verdient: $6.972.083`.

### AH-Einstellen: Titel `ꐷ` (U+A437), `generic_9x1`, secured `auction`

Slot 2 Abbrechen, 4 das einzustellende Item, 6 Preis festlegen. Klick auf 6 öffnet die
Anvil-Eingabe.

### Preiseingabe: Titel `Preis eingeben`, `minecraft:anvil`, secured `input`

Slot 0 Eingabe-Item (ID 80017, `addons:anvil_session` UUID), Slot 2 Bestätigen mit Lore
`Eingabe: 10000` und `addons:anvil_value = "10000"`, `addons:anvil_valid = 1`. Der Preis wird
also über das Anvil-Textfeld getippt. Verkaufs-Vorschlag setzt dieses Textfeld, der Spieler
klickt Bestätigen selbst.

### Auktion erstellen, Bestätigung: Titel `ꐶ` (U+A436), `generic_9x1`, secured `auction`

Gleiches Glyph wie die Order-Bestätigung, Unterscheidung nur über `secured`. Slot 2
Abbrechen, Slot 4 Item mit Lore `Preis: $60.000 / Einstellgebühr: $600 / "" / Die
Einstellgebühr wird beim / Zurückholen nicht erstattet.`, Slot 6 `Angebot erstellen`,
Slot 8 Info `Steuern`:

```
Einstellgebühr: 1%
Verkaufssteuer:
> bis $10.000: 1%
> bis $1.000.000: 2%
> über $1.000.000: 3%
Die Verkaufssteuer wird nur bei erfolgreichem Verkauf abgezogen.
```

Gebührenmodell AH also: 1 % beim Einstellen (verfällt beim Zurückholen) plus gestaffelte
Verkaufssteuer auf den Verkaufspreis.

### Order anlegen, Bestätigung: Titel `ꐶ` (U+A436), `generic_9x1`, secured `order`

Slot 2 Zurück, 4 Item mit Lore `Redstone-Staub / Anzahl: 64 / Preis pro Stück: $100 / "" /
Einzahlung: $6.400 / Erstellgebühr: $64 / Gesamtzahlung: $6.464 / …`, 6 Order aufgeben,
8 Info `Erstellgebühr: 1%`. Lieferungen müssen innerhalb `5d 0h` abgeholt werden.
Chat danach: `[HugoSMP] Order erstellt.`

### Order-Liste: Titel `ꐸ` (U+A438), `generic_9x6`, secured `order`

- Slots 0–44 Orders, Steuer-Items wie AH (45 Aktualisieren, 46 Suche, 48/50 Seiten,
  49 Filter, 52 Deine Orders, 53 Order erstellen).
- Order-Item: Name `Order von Zyit77`, Stack ist das gesuchte Item mit Komponenten.
  Lore: `Elytren Reparatur 1 Haltbarkeit 3` (Hugos deutsche Item-Beschreibung),
  `Preis pro Stück: $1.720.000.000`, `""`, `0/1 Geliefert`, `$0/$1.720.000.000 Ausgezahlt`,
  `""`, `Klicke, um Zyit77 Elytren Reparatur 1 Haltbarkeit 3 zu liefern`.
- Filter: `Neueste Orders / Am meisten ausgezahlt / Am meisten geliefert / Am meisten Geld
  pro Item`.
- "Deine Orders": `Alle / Offene / Abgeschlossene / Geleerte / Stornierte`, Summen wie im AH.
- Orders sind reine Kauf-Orders (Spieler liefert Items gegen Geld). Verkaufs-Orders gibt es
  nicht, der Spread ist also Order-Preis gegen AH-Preis.

### Order-Itemwahl: Titel `ꐹ` (U+A439), `generic_9x6`, secured `order`

Items mit Lore `Klicken, um auszuwählen`, Enchanted Books mit `Linksklick, um direkt
auszuwählen / Rechtsklick, um Anpassungen zu bearbeiten`. Slot 45 Zurück, 46 Suche,
48/50 Seiten.

### Sell-GUI: Titel `Items verkaufen` (dark_gray), `generic_9x4`, secured `input`

Slots 0–34 frei für Items, Slot 35 Verkaufen (`minecraft:disable_worth = 1`), Lore
`Verkauft alle Items in diesem Menü.`

### Kisten (Vanilla-Titel, z. B. `container.chestDouble`, `container.shulkerBox`)

Hugo hängt an verkaufbare Items eine Lore-Zeile: `Wert: $42,24 (+$3,84 durch 1.1x)` oder
`Wert: $4` ohne Bonus. Der Wert gilt für den ganzen Stack. `1.1x` ist der persönliche
Verkaufsmultiplikator des Spielers; der Bonus erscheint nur bei einem Teil der Items (Blöcke,
Pflanzen, Redstone-Bauteile), bei anderen (Eimer, Flint, Smaragd, Redstone-Staub,
Glowstone) nicht. Items ohne Zeile sind nicht an den Server verkaufbar.

### Enderchest: Titel `Enderchest` (dark_purple), `generic_9x6`

Hugos eigene Enderchest mit 54 Slots statt 27. Keine Steuer-Items, kein `secured`.
Shulker-Boxen darin tragen `minecraft:container` mit dem kompletten Inhalt und eine eigene
`Wert:`-Zeile (`$362,10 (+$1,50 durch 1.1x)`), vermutlich Inhalt plus Box. Vanilla-Tooltip
ergänzt `Contains 27 stack(s)` und `Left Shift: view contents`.

## Chat

- Prefix `[HugoSMP] ` als Farbverlauf (`H` #DE0307 … `P` #51257B), Nachricht danach.
- Payout: `[HugoSMP] +$159,16 (Basis: $144,80, Bonus: +$14,36 durch 1.1x)` nach dem
  Verkaufen-Klick. Basis = Summe der Wert-Zeilen ohne Bonus.
- `[HugoSMP] Order erstellt.`
- `[HugoSMP] Du hast die Auktion erfolgreich gekauft!` nach einem AH-Kauf. Kein
  Bestätigungsdialog gesehen, die Meldung nennt weder Item noch Preis. Das Trade-Log muss
  sich Item und Preis vom zuletzt angeklickten Listing merken.
- `[HugoSMP] Befehl nicht gefunden.`
- `[Info] …` Servertipps.
- Keine Actionbar-Nachrichten gesehen.

## Zahlen

Dollarzeichen vorangestellt, Tausenderpunkt, Dezimalkomma: `$1.720.000.000`, `$42,24`,
`$0,66`. Zeiten `23h 33m 55s`, `5d 0h`.

## Noch nicht gesehen

Deine-Auktionen-Liste, Verkaufsmeldung für den Verkäufer, Meldung nach dem Einstellen,
Shop, `/pay`, `/msg`, Spawner-Items, Oraxen/Nexo-Custom-Items. Ein Trade-Fenster gibt es
auf Hugo nicht.
