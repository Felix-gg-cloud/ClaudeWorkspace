CREATE TABLE ai_analysis_cache (
    id              BIGSERIAL PRIMARY KEY,
    content_hash    VARCHAR(64) UNIQUE NOT NULL,
    analysis_type   VARCHAR(50) NOT NULL,
    input_summary   TEXT,
    result          TEXT NOT NULL,
    model           VARCHAR(50),
    tokens_used     INT,
    created_at      TIMESTAMP DEFAULT NOW(),
    expires_at      TIMESTAMP
);

CREATE TABLE ai_call_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    api_provider    VARCHAR(50),
    call_type       VARCHAR(50),
    tokens_in       INT,
    tokens_out      INT,
    latency_ms      INT,
    status          VARCHAR(20),
    created_at      TIMESTAMP DEFAULT NOW()
);
