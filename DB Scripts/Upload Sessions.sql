CREATE TABLE upload_sessions (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL REFERENCES files(id),
    storage_upload_id TEXT NOT NULL,
    chunk_size BIGINT NOT NULL,
    total_chunks INT NOT NULL,
    uploaded_chunks INT NOT NULL DEFAULT 0,
    status upload_status NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);