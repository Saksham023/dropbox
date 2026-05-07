CREATE TABLE files (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    parent_id UUID REFERENCES files(id),
    name VARCHAR(1024) NOT NULL,
    type file_type NOT NULL,
    mime_type VARCHAR(255),
    object_key TEXT,
    size BIGINT NOT NULL DEFAULT 0,
    status file_status NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);