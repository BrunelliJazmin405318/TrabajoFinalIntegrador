CREATE TABLE IF NOT EXISTS mp_event_log (
                                            id BIGSERIAL PRIMARY KEY,
                                            request_id VARCHAR(100) UNIQUE,
    topic VARCHAR(50),
    data_id VARCHAR(50),
    received_at TIMESTAMP DEFAULT now(),
    processed BOOLEAN DEFAULT FALSE,
    error_msg TEXT
    );