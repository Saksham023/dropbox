const API_BASE_URL = 'http://localhost:8080'

export async function uploadFile({ ownerId, parentId = null, file }) {
  const initiatePayload = {
    ownerId,
    parentId,
    name: file.name,
    mimeType: file.type || 'application/octet-stream',
    size: file.size,
  }

  console.log('[UPLOAD] Initiate request payload:', initiatePayload)

  const initiateResponse = await fetch(`${API_BASE_URL}/uploads`, {
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
