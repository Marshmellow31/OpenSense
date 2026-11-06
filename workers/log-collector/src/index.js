// Free-tier collector for opt-in, codes-only XUV connection reports.
const CODES = new Set(["DEMO_STARTED","DEMO_STOPPED","SAMPLE_ACCEPTED","FRAME_REJECTED","RECONNECTING","STREAM_FAILED","PERMISSION_DENIED","DISCOVERY_STARTED","DISCOVERY_STOPPED","SERVICES_OBSERVED","ADAPTER_UNAVAILABLE","VEHICLE_SOCKET_CONNECTED","VEHICLE_MESSAGE_SENT","VEHICLE_SESSION_FAILED"]);
const short = (v, n) => String(v ?? "").slice(0, n);

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return new Response("XUV log collector", { status: 405 });
    const text = await request.text();
    if (text.length > 8192) return new Response("too large", { status: 413 });
    let body;
    try { body = JSON.parse(text); } catch { return new Response("bad json", { status: 400 }); }
    const events = Array.isArray(body.events) ? body.events.slice(0, 100) : null;
    if (!events || !events.length || !/^[0-9a-f-]{36}$/.test(body.installId ?? ""))
      return new Response("invalid", { status: 400 });
    // Allowlist: drop anything that is not a known code with a numeric offset.
    const clean = events.filter(e => CODES.has(e.c) && Number.isFinite(e.t)).map(e => ({ t: e.t, c: e.c }));
    if (!clean.length) return new Response("invalid", { status: 400 });
    // Crude abuse limit: 30 reports per install per hour.
    const row = await env.DB.prepare("SELECT COUNT(*) AS n FROM reports WHERE install_id=? AND received_at > datetime('now','-1 hour')").bind(body.installId).first();
    if (row.n >= 30) return new Response("slow down", { status: 429 });
    // Global cap so a flood of fake install IDs cannot fill the free tier.
    const day = await env.DB.prepare("SELECT COUNT(*) AS n FROM reports WHERE received_at > datetime('now','-1 day')").first();
    if (day.n >= 3000) return new Response("busy", { status: 429 });
    await env.DB.prepare("INSERT INTO reports (install_id, app, android, phone, family, events) VALUES (?,?,?,?,?,?)")
      .bind(body.installId, short(body.app, 24), Number(body.android) || 0, short(body.phone, 60), short(body.family, 24), JSON.stringify(clean)).run();
    return new Response("ok");
  },
};
