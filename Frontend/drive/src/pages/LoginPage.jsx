import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { loginUser } from '../services/authService'

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    console.log('[AUTH] Login submit clicked')

    setLoading(true)
    setMessage('')

    try {
      const user = await loginUser({ email, password })
      console.log('[AUTH] Login success:', user)
      localStorage.setItem('currentUser', JSON.stringify(user))
      navigate('/home')
    } catch (error) {
      console.error('[AUTH] Login failed:', error)
      setMessage(error.message)
    } finally {
      setLoading(false)
      console.log('[AUTH] Login flow finished')
    }
  }

  return (
    <main>
      <h1>Login</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">Email</label>
          <br />
          <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
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
        <button type="submit" disabled={loading}>{loading ? 'Logging in...' : 'Login'}</button>
      </form>
      {message && <p>{message}</p>}
      <p>
        New user? <Link to="/signup">Create account</Link>
      </p>
    </main>
  )
}
