export interface Env { DB: D1Database }

const MIN_CONTRIBUTORS = 5;
const BUCKET_MS = 6 * 60 * 60 * 1000;
const RETENTION_MS = 30 * 24 * 60 * 60 * 1000;
const CATEGORIES = new Set(["BLE_PATTERN", "WIFI_POSTURE", "MAGNETIC_ANOMALY", "MOTION_ANOMALY"]);

type Submission = { schemaVersion: number; category: string; coarseCell: string; timeBucket: number; rotatingToken: string };
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), { status, headers: { "content-type": "application/json", "cache-control": "no-store", "x-content-type-options": "nosniff" } });

function validSubmission(value: unknown, now: number): value is Submission {
  if (!value || typeof value !== "object") return false;
  const v = value as Record<string, unknown>;
  return v.schemaVersion === 1 && typeof v.category === "string" && CATEGORIES.has(v.category) && typeof v.coarseCell === "string" && /^[a-z0-9_-]{3,16}$/.test(v.coarseCell) && typeof v.timeBucket === "number" && Number.isInteger(v.timeBucket) && Math.abs(v.timeBucket - Math.floor(now / BUCKET_MS)) <= 2 && typeof v.rotatingToken === "string" && /^[a-f0-9]{32,64}$/.test(v.rotatingToken);
}

function rounded(value: number) { return Math.floor(value / MIN_CONTRIBUTORS) * MIN_CONTRIBUTORS; }

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") return json({ service: "scrying-public-hub", privacy: "aggregate-only", minimumContributors: MIN_CONTRIBUTORS });
    if (request.method === "POST" && url.pathname === "/v1/aggregate") {
      let body: unknown;
      try { body = await request.json(); } catch { return json({ error: "invalid_json" }, 400); }
      const now = Date.now();
      if (!validSubmission(body, now)) return json({ error: "invalid_aggregate" }, 400);
      const item = body as Submission;
      const result = await env.DB.prepare("INSERT OR IGNORE INTO submissions(category, coarse_cell, time_bucket, rotating_token, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)").bind(item.category, item.coarseCell, item.timeBucket, item.rotatingToken, now, now + RETENTION_MS).run();
      if (result.meta.changes > 0) await env.DB.prepare("INSERT INTO aggregates(category, coarse_cell, time_bucket, contributor_count, updated_at) VALUES (?, ?, ?, 1, ?) ON CONFLICT(category, coarse_cell, time_bucket) DO UPDATE SET contributor_count = contributor_count + 1, updated_at = excluded.updated_at").bind(item.category, item.coarseCell, item.timeBucket, now).run();
      const aggregate = await env.DB.prepare("SELECT contributor_count FROM aggregates WHERE category = ? AND coarse_cell = ? AND time_bucket = ?").bind(item.category, item.coarseCell, item.timeBucket).first<{ contributor_count: number }>();
      return json({ accepted: true, visible: (aggregate?.contributor_count ?? 0) >= MIN_CONTRIBUTORS });
    }
    if (request.method === "GET" && url.pathname === "/v1/insights") {
      const cell = url.searchParams.get("cell") ?? "";
      if (!/^[a-z0-9_-]{3,16}$/.test(cell)) return json({ error: "invalid_cell" }, 400);
      const earliest = Math.floor((Date.now() - 7 * 24 * 60 * 60 * 1000) / BUCKET_MS);
      const { results } = await env.DB.prepare("SELECT category, time_bucket, contributor_count FROM aggregates WHERE coarse_cell = ? AND time_bucket >= ? AND contributor_count >= ? ORDER BY time_bucket DESC LIMIT 100").bind(cell, earliest, MIN_CONTRIBUTORS).all<{ category: string; time_bucket: number; contributor_count: number }>();
      return json({ cell, minimumContributors: MIN_CONTRIBUTORS, insights: results.map((r) => ({ category: r.category, timeBucket: r.time_bucket, contributorCount: rounded(r.contributor_count) })) });
    }
    return json({ error: "not_found" }, 404);
  },
  async scheduled(_: ScheduledEvent, env: Env): Promise<void> {
    const now = Date.now();
    await env.DB.batch([env.DB.prepare("DELETE FROM submissions WHERE expires_at < ?").bind(now), env.DB.prepare("DELETE FROM aggregates WHERE time_bucket < ?").bind(Math.floor((now - RETENTION_MS) / BUCKET_MS))]);
  }
} satisfies ExportedHandler<Env>;
