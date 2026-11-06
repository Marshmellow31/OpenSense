CREATE TABLE IF NOT EXISTS reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  received_at TEXT NOT NULL DEFAULT (datetime('now')),
  install_id TEXT NOT NULL,
  app TEXT, android INTEGER, phone TEXT, family TEXT,
  events TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS reports_received ON reports(received_at);
