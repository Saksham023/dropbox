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
