import type { FastifyInstance } from "fastify";

export interface LegalInfo {
  name: string;
  address: string;
  email: string;
}

export function legalFromEnv(env: NodeJS.ProcessEnv = process.env): LegalInfo | null {
  if (!env.IMPRESSUM_NAME || !env.IMPRESSUM_ADDRESS || !env.IMPRESSUM_EMAIL) {
    return null;
  }
  return { name: env.IMPRESSUM_NAME, address: env.IMPRESSUM_ADDRESS, email: env.IMPRESSUM_EMAIL };
}

export function registerLegalRoutes(app: FastifyInstance, info: LegalInfo | null): void {
  app.get("/", async (_request, reply) => reply.type("text/html; charset=utf-8").send(page("HugoMarket", home())));
  app.get("/impressum", async (_request, reply) => reply.type("text/html; charset=utf-8").send(page("Impressum", impressum(info))));
  app.get("/datenschutz", async (_request, reply) => reply.type("text/html; charset=utf-8").send(page("Datenschutz", datenschutz(info))));
}

function page(title: string, body: string): string {
  return `<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title} – HugoMarket</title><style>body{font-family:system-ui,sans-serif;max-width:720px;margin:40px auto;padding:0 16px;line-height:1.5;color:#1b1b1f}a{color:#5b2a86}footer{margin-top:40px;font-size:.9em;color:#666}</style></head><body>${body}<footer><a href="/">Start</a> · <a href="/impressum">Impressum</a> · <a href="/datenschutz">Datenschutz</a></footer></body></html>`;
}

function home(): string {
  return `<h1>HugoMarket</h1><p>Preis-Server der HugoMarket-Mod für HugoSMP. Diese Adresse wird von der Mod benutzt, um gesehene Auktionshaus-Preise anonym zu teilen. Es gibt hier nichts zu klicken.</p>`;
}

function escape(text: string): string {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/\n/g, "<br>");
}

function impressum(info: LegalInfo | null): string {
  if (!info) {
    return `<h1>Impressum</h1><p>Noch nicht hinterlegt.</p>`;
  }
  return `<h1>Impressum</h1><p>Angaben gemäß § 5 DDG</p><p>${escape(info.name)}<br>${escape(info.address)}</p><p>Kontakt: <a href="mailto:${escape(info.email)}">${escape(info.email)}</a></p><p>HugoMarket ist ein privates, nicht kommerzielles Projekt und steht in keiner Verbindung zu HugoSMP oder Mojang.</p>`;
}

function datenschutz(info: LegalInfo | null): string {
  const responsible = info ? `${escape(info.name)}, ${escape(info.address)}, ${escape(info.email)}` : "siehe Impressum";
  return `<h1>Datenschutzerklärung</h1>
<p>Verantwortlich: ${responsible}</p>
<h2>Was dieser Server verarbeitet</h2>
<p>Die HugoMarket-Mod sendet Beobachtungen aus dem Auktionshaus von HugoSMP: Item, Anzahl, Preis, Restzeit und einen Hash des Verkäufernamens. Dazu eine zufällig erzeugte Client-ID, die Mod-Version und die Minecraft-Version. Es werden keine Minecraft-Accountnamen, keine Account-UUIDs und keine Koordinaten übertragen.</p>
<h2>Wofür</h2>
<p>Aus den Beobachtungen werden Marktpreise und Preisverläufe berechnet und allen Nutzern der Mod bereitgestellt. Die Client-ID dient der Missbrauchsabwehr (Ratenbegrenzung, Plausibilitätsprüfung).</p>
<h2>Speicherdauer</h2>
<p>Einzelne Beobachtungen werden nach 30 Tagen gelöscht, Auktionseinträge nach 90 Tagen. Aggregierte Preisverläufe ohne Personenbezug bleiben erhalten. IP-Adressen werden nur flüchtig zur Ratenbegrenzung im Arbeitsspeicher gehalten und nicht protokolliert.</p>
<h2>Rechtsgrundlage</h2>
<p>Berechtigtes Interesse am Betrieb des Dienstes (Art. 6 Abs. 1 lit. f DSGVO). Die Übertragung lässt sich in der Mod jederzeit im Menü abschalten (Modul "Backend-Sync").</p>
<h2>Weitergabe</h2>
<p>Keine Weitergabe an Dritte. Der Server steht bei Contabo GmbH in Deutschland.</p>
<h2>Deine Rechte</h2>
<p>Auskunft, Berichtigung, Löschung, Einschränkung, Widerspruch. Da keine Namen gespeichert werden, nenne bei Anfragen bitte deine Client-ID aus der Datei <code>config/hugomarket/config.json</code>.</p>`;
}
