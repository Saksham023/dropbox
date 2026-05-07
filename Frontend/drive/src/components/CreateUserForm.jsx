import { useState } from 'react'
import { createUser } from '../services/userService'

export default function CreateUserForm() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    console.log('[USER] Submit clicked')

    setLoading(true)
    setMessage('')

    try {
      const user = await createUser({ email, password, fullName })
      console.log('[USER] Create success:', user)
      setMessage(`User created: ${user.email} (${user.id})`)
      setEmail('')
      setPassword('')
      setFullName('')
    } catch (error) {
      console.error('[USER] Create failed:', error)
      setMessage(error.message)
    } finally {
      setLoading(false)
      console.log('[USER] Create flow finished')
    }
  }

  return (
    <section>
      <h2>Create New User</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">Email</label>
          <br />
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="password">Password</label>
          <br />
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="fullName">Full Name</label>
          <br />
          <input
            id="fullName"
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
        </div>

        <button type="submit" disabled={loading}>
          {loading ? 'Creating...' : 'Create User'}
        </button>
      </form>
      {message && <p>{message}</p>}
    </section>
  )
}
