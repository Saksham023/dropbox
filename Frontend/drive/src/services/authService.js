const API_BASE_URL = 'http://localhost:8080'

export async function loginUser({ email, password }) {
  const payload = { email, password }
  console.log('[AUTH] Login request payload:', payload)

  const response = await fetch(`${API_BASE_URL}/users/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  console.log('[AUTH] Login response status:', response.status)

  let data = null
  try {
    data = await response.json()
    console.log('[AUTH] Login response body:', data)
  } catch (error) {
    console.log('[AUTH] Login response parse failed:', error)
  }

  if (!response.ok) {
    throw new Error(data?.message || 'Login failed')
  }

  return data
}
