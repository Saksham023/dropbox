const API_BASE_URL = 'http://localhost:8080'

export async function createUser({ email, password, fullName }) {
  const payload = { email, password, fullName }
  console.log('[USER] Create request payload:', payload)

  const response = await fetch(`${API_BASE_URL}/users`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  console.log('[USER] Create response status:', response.status)

  let data = null
  try {
    data = await response.json()
    console.log('[USER] Create response body:', data)
  } catch (error) {
    console.log('[USER] Create response body parse failed:', error)
  }

  if (!response.ok) {
    throw new Error(data?.message || 'Failed to create user')
  }

  return data
}
