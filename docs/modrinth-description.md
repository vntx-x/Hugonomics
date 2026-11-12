# Hugonomics – Better AH & Orders für HugoSMP

*(English description below.)*

Client-Mod für HugoSMP. Macht das Auktionshaus durchsuchbar, zeigt Marktpreise überall dort, wo du sie brauchst, und rechnet mit: Gebühren, Flips, Kistenwerte, Spawner-Deals, Farm-Raten.

**Kein Auto-Kauf, kein Sniping, keine Klicks ohne dich.** Die Mod liest, was dein Client sowieso sieht. Der einzige Befehl, den sie schickt, ist ein einzelnes `/ah <Item>`, wenn du in einer ihrer Listen auf ein Angebot klickst. Danach sortiert sie und blättert höchstens 15 Seiten, um dir genau dieses Angebot zu zeigen. Kaufen klickst immer du.

## Auktionshaus
- Durchsichtige Seitenleiste neben dem AH: Suche nach Name, Enchant oder Verkäufer, mehrere Wörter gleichzeitig
- Angebote aller Hugonomics-Nutzer, nicht nur die Seiten, die du selbst gesehen hast
- Sortierung nach Stückpreis, Restzeit oder "neu gelistet", Filter Enchanted, Schnäppchen, Watchlist
- Hover zeigt den 7-Tage-Preisverlauf und den Flip-Gewinn (AH und Orders)
- Klick auf ein Angebot springt im AH direkt dorthin
- Übersicht deiner Auktionen: Listenwert, Netto nach Steuer, Marktwert, zu teure Einträge

## Schnäppchen und Watchlist
- Schnäppchen-Finder: Angebote unter dem Marktmedian und Order-Flips, mit Gewinn nach Steuer
- Watchlist-Seite mit günstigstem Angebot, Median und Abstand, Alarm mit Ton bei neuen Schnäppchen
- Verschiebbare HUD-Box mit Tagesgewinn, Vermögen und deinen Watchlist-Items

## Einstellen und Verkaufen
- Im Preisfeld: Lowest, Median, dein letzter Preis, ein Klick übernimmt den Betrag
- Live-Vorschau von Einstellgebühr, Verkaufssteuer und Netto, Warnung bei Tippfehlern (zehnfach daneben)
- Verkaufsliste: dein ganzes Inventar mit Preisvorschlag und Netto je Item

## Preise überall
- Tooltip auf jedem Item: Stückpreis, AH Lowest und Median, bester Order-Preis
- Kisten, Fässer, Enderchest: Gesamtwert nach Hugo und nach AH, gefüllte Shulker nach ihrem Inhalt bewertet
- Markt-Screen: alle Items, Suche, Chart für 1 Stunde bis 31 Tage, Preis-Alarme

## Spawner-Börse
- Öffnet über einen Knopf über deinem Inventar
- Angebote aller Hugonomics-Nutzer pro Spawner-Typ, eigene Kauf- und Verkaufsangebote einstellen
- Angebot annehmen oder Gegenangebot schicken, Verhandlung unter "Meine Deals", Hinweis im Chat bei neuen Anfragen
- Sterne-Bewertung nach jedem abgeschlossenen Deal, damit man sieht, wer vertrauenswürdig ist
- Richtpreis pro Typ und Deal-Rechner mit Abweichung davon, fertiger Chat-Text zum Kopieren
- Einmalige Anmeldung über Mojang (dieselbe Prüfung wie beim Joinen eines Servers), dein Login-Token geht nur an Mojang
- Bezahlt wird im Spiel, die Börse bringt Käufer und Verkäufer nur zusammen
- Gestackte Spawner zählen mit ihrer echten Anzahl beim Kistenwert und Vermögen

## Orders
- Pro Order die Marge, wenn du im AH kaufst und lieferst, lohnende Orders markiert

## Farm und Werkzeuge
- Zulauf in offene Kisten pro Minute, Stunde und Tag, Farm-Session mit Raten pro Stunde
- Verkaufs-Statistik und Minutentest mit Sellmulti-Tabelle bis 3.0x
- Trade-Log, Profit-Übersicht und Gewinn-Diagramm über 14 Tage
- Rechner mit K/M/B und Prozent (Taste N), Chat-Kurzform `=12*48k`
- Farbcode-Editor mit Chat-Vorschau für `/rename`
- Jedes Modul einzeln abschaltbar, Übersicht aller Funktionen im Menü (Taste J)

## Daten
Gesehene AH-Listings werden anonym an den Hugonomics-Server gemeldet, damit alle Nutzer aktuelle Preise bekommen. Verkäufernamen gehen nur gehasht raus, Koordinaten und Accountdaten gar nicht. Trade-Log, Gewinne und Einstellungen bleiben auf deinem PC. Wenn du die Spawner-Börse nutzt, sehen andere Nutzer deinen Minecraft-Namen, deine Angebote und deine Bewertung. Das Teilen der Preise ist im Menü abschaltbar ("Backend-Sync"). Datenschutz: https://hugomarket.duckdns.org/datenschutz

## Installation
Fabric Loader, Fabric API, Jar für deine Minecraft-Version (1.21.5, 1.21.6–1.21.8, 1.21.9–1.21.10, 1.21.11). Mod Menu optional.

Hugonomics ist Open Source (MIT) und kein offizielles Projekt von HugoSMP.

---

# Hugonomics – Better AH & Orders for HugoSMP

Client-side mod for the HugoSMP server. Makes the auction house searchable, shows market prices wherever you need them, and does the math for you: fees, flips, chest values, spawner deals, farm rates.

**No auto-buy, no sniping, no clicks without you.** The mod reads what your client already sees. The only command it sends is a single `/ah <item>` when you click an offer in one of its lists. It then sorts and pages through at most 15 pages to show you exactly that offer. Buying is always your click.

## Auction house
- Translucent sidebar next to the AH: search by name, enchantment or seller, several words at once
- Offers from all Hugonomics users, not just the pages you have seen yourself
- Sort by unit price, time left or newly listed, filters for enchanted, bargains and watchlist
- Hover a result for its 7 day price history and flip profit (AH and orders)
- Click an offer to jump straight to it in the AH
- Overview of your own auctions: list value, net after tax, market value, overpriced entries

## Deals and watchlist
- Bargain finder: offers below the market median and order flips, with profit after tax
- Watchlist page with cheapest offer, median and difference, alert with sound for new bargains
- Movable HUD box with today's profit, net worth and your watched items

## Listing and selling
- In the price field: lowest, median, your last price, one click fills the amount
- Live preview of listing fee, sale tax and net, typo warning (ten times off)
- Sell list: your whole inventory with a price suggestion and net per item

## Prices everywhere
- Tooltip on every item: unit price, AH lowest and median, best order price
- Chests, barrels, ender chest: total value by Hugo and by AH, filled shulker boxes valued by their contents
- Market screen: all items, search, chart from 1 hour to 31 days, price alerts

## Spawner exchange
- Opens from a button above your inventory
- Offers from all Hugonomics users per spawner type, post your own buy or sell offers
- Accept an offer or send a counter offer, negotiate under "My deals", chat notice for new requests
- Star ratings after every agreed deal, so you can see who is trusted
- Reference price per type and deal calculator with the difference to it, ready German chat text to copy
- Sign-in once via Mojang (the same check a server does when you join), your login token only goes to Mojang
- Paying happens in game, the exchange only brings buyers and sellers together
- Stacked spawners count with their real amount in chest value and net worth

## Orders
- Margin per order when buying in the AH and delivering, profitable orders highlighted

## Farming and tools
- Inflow into open chests per minute, hour and day, farm session with hourly rates
- Sell statistics and minute test with sell multiplier table up to 3.0x
- Trade log, profit overview and 14 day profit chart
- Calculator with K/M/B and percent (key N), chat shortcut `=12*48k`
- Color code editor with chat preview for `/rename`
- Every module can be switched off, overview of all features in the menu (key J)

## Data
AH listings you see are reported anonymously to the Hugonomics server so every user gets current prices. Seller names leave the client only as hashes, coordinates and account data never. Trade log, profits and settings stay on your PC. If you use the spawner exchange, your Minecraft name, your offers and your rating are visible to other users. Price sharing can be disabled in the menu ("Backend sync"). Privacy policy: https://hugomarket.duckdns.org/datenschutz

## Installation
Fabric Loader, Fabric API, the jar for your Minecraft version (1.21.5, 1.21.6–1.21.8, 1.21.9–1.21.10, 1.21.11). Mod Menu optional.

Hugonomics is open source (MIT) and not an official HugoSMP project.
