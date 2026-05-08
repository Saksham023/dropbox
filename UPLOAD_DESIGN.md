# Upload Design

## Storage Model

- `files` stores user-facing metadata and lifecycle state.
- `upload_sessions` stores multipart upload state.
- `upload_parts` is optional for live tracking, but can remain for audit/debugging.

## File States

- `UPLOADING`: file record exists, upload not finished.
- `READY`: upload completed successfully.
- `FAILED`: upload or finalize step failed.
- `DELETED`: file is soft-deleted.

## Multipart Upload Flow

1. Client requests upload initialization.
2. Backend creates a `files` row with `status = UPLOADING`.
3. Backend initiates multipart upload in S3/R2.
4. Backend creates an `upload_sessions` row with:
   - `file_id`
   - `storage_upload_id`
   - `chunk_size`
   - `total_chunks`
   - `status = INITIATED`
5. Backend returns presigned URLs for parts.
6. Client uploads parts directly to object storage.
7. Client calls the resume endpoint when needed.
8. Backend calls `ListParts`, computes missing parts, and returns resume info.
9. Client uploads only missing parts.
10. Client calls complete.
11. Backend completes the multipart upload in storage.
12. Backend marks `upload_sessions = COMPLETED` and `files = READY`.

## Resume Strategy

- No backend call is made after every chunk.
- Resume is driven by a file-scoped upload session endpoint such as `POST /files/:fileId/upload-session/resume`.
- Backend uses `ListParts` from storage to determine which part numbers already exist.
- Frontend re-uploads only the missing part numbers.

## APIs

### `POST /files`

Creates the file record and multipart upload session.

Request:

- `name`
- `parent_id`
- `mime_type`
- `size`

Response:

- `file_id`
- `upload_session_id`
- `chunk_size`
- `total_chunks`
- presigned part upload data

### `POST /files/:fileId/upload-session/resume`

Returns current multipart progress.

Response:

- `chunk_size`
- `total_chunks`
- `uploaded_parts`
- `missing_parts`
- optional presigned URLs for missing parts

### `POST /files/:fileId/upload-session/complete`

Completes the multipart upload.

Request:

- ordered list of `part_number` and `etag`

Response:

- file metadata
- final status

### `POST /files/:fileId/upload-session/abort`

Aborts the multipart upload and marks the session failed or aborted.

## Key Rules

- Object storage is the source of truth for file bytes.
- DB is the source of truth for metadata and upload state.
- `object_key` should be generated once and never depend on the user-facing filename.
- Folders do not use multipart upload.
- The frontend should upload parts directly to storage, not through the API server.
