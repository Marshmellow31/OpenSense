# XUV log collector (free tier)

Cloudflare Worker + D1. Receives opt-in, codes-only connection reports.

```bash
cd workers/log-collector
npx wrangler login
npx wrangler d1 create xuv-logs        # paste database_id into wrangler.toml
npx wrangler d1 execute xuv-logs --remote --file=schema.sql
npx wrangler deploy                    # prints https://xuv-log-collector.<you>.workers.dev
```

Put that URL in `DiagnosticReporter.ENDPOINT` and rebuild. Read reports with:

```bash
npx wrangler d1 execute xuv-logs --remote --command "SELECT received_at, phone, family, events FROM reports ORDER BY id DESC LIMIT 20"
```

Delete a user's data on request: `DELETE FROM reports WHERE install_id='<id>'`.
