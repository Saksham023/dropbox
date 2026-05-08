const API_BASE_URL = 'http://localhost:8080'

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

  console.log('[UPLOAD] Uploading file to presigned URL:', uploadData.uploadUrl)

  const putResponse = await fetch(uploadData.uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: file,
  })

  console.log('[UPLOAD] PUT response status:', putResponse.status)
  console.log('[UPLOAD] PUT response ok:', putResponse.ok)

  const putText = await putResponse.text()
  console.log('[UPLOAD] PUT response body:', putText)

  if (!putResponse.ok) {
    throw new Error('Failed to upload file bytes to storage')
  }

  console.log('[UPLOAD] Upload flow completed successfully for fileId:', uploadData.fileId)

  return uploadData
}
