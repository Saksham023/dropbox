CREATE TABLE upload_parts (
    id UUID PRIMARY KEY,
    upload_session_id UUID NOT NULL REFERENCES upload_sessions(id),
    part_number INT NOT NULL,
    etag TEXT NOT NULL,
    size BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(upload_session_id, part_number)
);
