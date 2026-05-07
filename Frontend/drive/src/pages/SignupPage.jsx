import { Link } from 'react-router-dom'
import CreateUserForm from '../components/CreateUserForm'

export default function SignupPage() {
  return (
    <main>
      <h1>Create Account</h1>
      <CreateUserForm />
      <p>
        Already have an account? <Link to="/login">Back to login</Link>
      </p>
    </main>
  )
}
