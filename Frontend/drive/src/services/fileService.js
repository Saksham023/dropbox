const API_BASE_URL = 'http://localhost:8080'
const PENDING_UPLOAD_KEY = 'pendingChunkedUpload'

export async function listFiles({ ownerId, parentId = null }) {
  const params = new URLSearchParams({ ownerId })
  if (parentId) {
    params.set('parentId', parentId)
  }

  const url = `${API_BASE_URL}/files?${params.toString()}`
  console.log('[FILES] List request URL:', url)

  const response = await fetch(url)
  console.log('[FILES] List response status:', response.status)

  let data = []
  try {
    data = await response.json()
    console.log('[FILES] List response body:', data)
  } catch (error) {
    console.log('[FILES] List response parse failed:', error)
  }

  if (!response.ok) {
    throw new Error(data?.message || 'Failed to fetch files')
  }

  return data
}

export async function createFolder({ ownerId, parentId = null, name }) {
  const payload = { ownerId, parentId, name }
  console.log('[FOLDER] Create request payload:', payload)

  const response = await fetch(`${API_BASE_URL}/files/folders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  console.log('[FOLDER] Create response status:', response.status)

  let data = null
  try {
    data = await response.json()
    console.log('[FOLDER] Create response body:', data)
  } catch (error) {
    console.log('[FOLDER] Create response parse failed:', error)
  }

  if (!response.ok) {
    throw new Error(data?.message || 'Failed to create folder')
  }

  return data
}

export async function uploadFile({ ownerId, parentId = null, file }) {
  return uploadFileInChunks({ ownerId, parentId, file })
}

export function getPendingUpload() {
  const raw = localStorage.getItem(PENDING_UPLOAD_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw)
  } catch (error) {
    console.log('[UPLOAD] Pending upload parse failed:', error)
    localStorage.removeItem(PENDING_UPLOAD_KEY)
    return null
  }
}

export function clearPendingUpload() {
  localStorage.removeItem(PENDING_UPLOAD_KEY)
}

export async function uploadFileInChunks({ ownerId, parentId = null, file, onProgress = () => {} }) {
  const initiatePayload = {
    ownerId,
    parentId,
    name: file.name,
    mimeType: file.type || 'application/octet-stream',
    size: file.size,
  }

  console.log('[UPLOAD] Initiate request payload:', initiatePayload)

  const initiateResponse = await fetch(`${API_BASE_URL}/files`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(initiatePayload),
  })

  console.log('[UPLOAD] Initiate response status:', initiateResponse.status)

  let uploadData = null
  try {
    uploadData = await initiateResponse.json()
    console.log('[UPLOAD] Initiate response body:', uploadData)
  } catch (error) {
    console.log('[UPLOAD] Initiate response body parse failed:', error)
  }

  if (!initiateResponse.ok) {
    throw new Error(uploadData?.message || 'Failed to initiate upload')
  }

  const pendingUpload = {
    ownerId,
    parentId,
    fileId: uploadData.fileId,
    uploadSessionId: uploadData.uploadSessionId,
    objectKey: uploadData.objectKey,
    name: file.name,
    mimeType: file.type || 'application/octet-stream',
    size: file.size,
    chunkSize: uploadData.chunkSize,
    totalChunks: uploadData.totalChunks,
  }
  localStorage.setItem(PENDING_UPLOAD_KEY, JSON.stringify(pendingUpload))

  onProgress(buildProgress('uploading', 0, uploadData.totalChunks))

  try {
    await uploadMissingChunks({
      ownerId,
      file,
      uploadState: pendingUpload,
      partUploadUrls: uploadData.partUploadUrls,
      onProgress,
      alreadyUploadedChunks: 0,
    })

    onProgress(buildProgress('completing', uploadData.totalChunks, uploadData.totalChunks))
    const completed = await completeUpload({
      ownerId,
      fileId: uploadData.fileId,
      uploadSessionId: uploadData.uploadSessionId,
    })

    clearPendingUpload()
    onProgress(buildProgress('complete', uploadData.totalChunks, uploadData.totalChunks))

    console.log('[UPLOAD] Chunked upload completed successfully for fileId:', uploadData.fileId)

    return { ...uploadData, completed }
  } catch (error) {
    console.error('[UPLOAD] Chunked upload failed:', error)
    throw error
  }
}

export async function resumeUpload({ ownerId, file, uploadState, onProgress = () => {} }) {
  if (!uploadState?.fileId || !uploadState?.uploadSessionId) {
    throw new Error('No resumable upload session was found')
  }

  if (file.name !== uploadState.name || file.size !== uploadState.size) {
    throw new Error('Select the same file to resume this upload')
  }

  const session = await getUploadSession({ ownerId, fileId: uploadState.fileId })
  const totalChunks = session.totalChunks
  const uploadedChunks = session.uploadedChunks

  onProgress(buildProgress('uploading', uploadedChunks, totalChunks))

  await uploadMissingChunks({
    ownerId,
    file,
    uploadState: {
      ...uploadState,
      chunkSize: session.chunkSize,
      totalChunks,
    },
    partUploadUrls: session.partUploadUrls,
    onProgress,
    alreadyUploadedChunks: uploadedChunks,
  })

  onProgress(buildProgress('completing', totalChunks, totalChunks))
  const completed = await completeUpload({
    ownerId,
    fileId: uploadState.fileId,
    uploadSessionId: uploadState.uploadSessionId,
  })

  clearPendingUpload()
  onProgress(buildProgress('complete', totalChunks, totalChunks))

  return { ...session, completed }
}

export async function abortUpload({ ownerId, fileId, uploadSessionId }) {
  const response = await fetch(`${API_BASE_URL}/files/${fileId}/upload-session/abort`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ownerId, uploadSessionId }),
  })

  const data = await parseJson(response)
  if (!response.ok) {
    throw new Error(data?.message || 'Failed to abort upload')
  }
  clearPendingUpload()
  return data
}

async function uploadMissingChunks({
  ownerId,
  file,
  uploadState,
  partUploadUrls,
  onProgress,
  alreadyUploadedChunks,
}) {
  const urlsByPartNumber = new Map(partUploadUrls.map((part) => [part.partNumber, part]))
  let uploadedChunks = alreadyUploadedChunks

  for (const [partNumber, part] of urlsByPartNumber) {
    const chunk = chunkForPart(file, uploadState.chunkSize, partNumber)
    const etag = await uploadChunk({ file, chunk, uploadUrl: part.uploadUrl })

    const progress = await recordUploadPart({
      ownerId,
      fileId: uploadState.fileId,
      uploadSessionId: uploadState.uploadSessionId,
      partNumber,
      etag,
      size: chunk.size,
    })

    uploadedChunks = progress.uploadedChunks
    onProgress(buildProgress('uploading', uploadedChunks, uploadState.totalChunks))
  }
}

function chunkForPart(file, chunkSize, partNumber) {
  const start = (partNumber - 1) * chunkSize
  const end = Math.min(start + chunkSize, file.size)
  return file.slice(start, end)
}

async function uploadChunk({ file, chunk, uploadUrl }) {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: chunk,
  })

  console.log('[UPLOAD] Chunk PUT response status:', response.status)

  if (!response.ok) {
    throw new Error('Failed to upload file chunk to storage')
  }

  const etag = response.headers.get('ETag') || response.headers.get('etag')
  if (!etag) {
    throw new Error('Storage did not return an ETag for the uploaded chunk')
  }

  return etag
}

async function recordUploadPart({ ownerId, fileId, uploadSessionId, partNumber, etag, size }) {
  const response = await fetch(`${API_BASE_URL}/files/${fileId}/upload-session/parts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ownerId, uploadSessionId, partNumber, etag, size }),
  })

  const data = await parseJson(response)
  if (!response.ok) {
    throw new Error(data?.message || 'Failed to record uploaded chunk')
  }
  return data
}

async function getUploadSession({ ownerId, fileId }) {
  const params = new URLSearchParams({ ownerId })
  const response = await fetch(`${API_BASE_URL}/files/${fileId}/upload-session?${params.toString()}`)

  const data = await parseJson(response)
  if (!response.ok) {
    throw new Error(data?.message || 'Failed to resume upload')
  }
  return data
}

async function completeUpload({ ownerId, fileId, uploadSessionId }) {
  const response = await fetch(`${API_BASE_URL}/files/${fileId}/upload-session/complete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ownerId, uploadSessionId }),
  })

  const data = await parseJson(response)
  if (!response.ok) {
    throw new Error(data?.message || 'Failed to complete upload')
  }
  return data
}

async function parseJson(response) {
  try {
    return await response.json()
  } catch {
    return null
  }
}

function buildProgress(status, uploadedChunks, totalChunks) {
  const percentage = totalChunks > 0 ? Math.round((uploadedChunks / totalChunks) * 100) : 0
  return { status, uploadedChunks, totalChunks, percentage }
}
