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
   - `uploaded_chunks = 0`
   - `status = INITIATED`
5. Backend returns presigned URLs for parts.
6. Client uploads parts directly to object storage.
7. After each successful part upload, the client records the part number, ETag, and size with the backend.
8. Client calls the resume endpoint when needed.
9. Backend reads `upload_parts`, computes missing parts, and returns fresh presigned URLs.
10. Client uploads only missing parts.
11. Client calls complete.
12. Backend completes the multipart upload in storage.
13. Backend marks `upload_sessions = COMPLETED` and `files = READY`.

## Resume Strategy

- The backend records each successfully uploaded chunk in `upload_parts`.
- Resume is driven by `GET /files/:fileId/upload-session?ownerId=...`.
- Backend uses `upload_parts` to determine which part numbers already exist.
- Frontend re-uploads only the missing part numbers.
- The object storage bucket CORS rules must expose the `ETag` response header for browser uploads.

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

### `POST /files/:fileId/upload-session/parts`

Records one successfully uploaded part.

Request:

- `ownerId`
- `uploadSessionId`
- `partNumber`
- `etag`
- `size`

Response:

- `uploadedChunks`
- `totalChunks`
- current upload session status

### `GET /files/:fileId/upload-session?ownerId=...`

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

- `ownerId`
- `uploadSessionId`

Response:

- file id
- final status

### `POST /files/:fileId/upload-session/abort`

Aborts the multipart upload and marks the session failed or aborted.

Request:

- `ownerId`
- `uploadSessionId`

## Key Rules

- Object storage is the source of truth for file bytes.
- DB is the source of truth for metadata and upload state.
- `object_key` should be generated once and never depend on the user-facing filename.
- Folders do not use multipart upload.
- The frontend should upload parts directly to storage, not through the API server.
