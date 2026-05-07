CREATE TYPE file_type AS ENUM (
    'FILE',
    'FOLDER'
);

CREATE TYPE file_status AS ENUM (
    'UPLOADING',
    'READY',
    'FAILED',
    'DELETED'
);

CREATE TYPE upload_status AS ENUM (
    'INITIATED',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED',
    'ABORTED'
);