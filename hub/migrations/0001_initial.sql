CREATE TABLE IF NOT EXISTS submissions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  category TEXT NOT NULL,
  coarse_cell TEXT NOT NULL,
  time_bucket INTEGER NOT NULL,
  rotating_token TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  UNIQUE(category, coarse_cell, time_bucket, rotating_token)
);

CREATE TABLE IF NOT EXISTS aggregates (
  category TEXT NOT NULL,
  coarse_cell TEXT NOT NULL,
  time_bucket INTEGER NOT NULL,
  contributor_count INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL,
  PRIMARY KEY(category, coarse_cell, time_bucket)
);

CREATE INDEX IF NOT EXISTS idx_aggregates_lookup ON aggregates(coarse_cell, time_bucket);
CREATE INDEX IF NOT EXISTS idx_submissions_expiry ON submissions(expires_at);
