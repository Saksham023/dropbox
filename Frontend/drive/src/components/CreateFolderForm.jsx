import { useState } from 'react'
import { createFolder } from '../services/fileService'

export default function CreateFolderForm({ ownerId, parentId, onFolderCreated }) {
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const handleCreate = async (e) => {
    e.preventDefault()

    if (!ownerId || !name.trim()) {
      setMessage('Folder name is required')
      return
    }

    setLoading(true)
    setMessage('')

    try {
      const result = await createFolder({ ownerId, parentId, name: name.trim() })
      console.log('[FOLDER] Create success:', result)
      setName('')
      setMessage('Folder created')
      onFolderCreated()
    } catch (error) {
      console.error('[FOLDER] Create failed:', error)
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section>
      <h2>Create Folder</h2>
      <form onSubmit={handleCreate}>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Folder name"
        />
        <button type="submit" disabled={loading}>{loading ? 'Creating...' : 'Create Folder'}</button>
      </form>
      {message && <p>{message}</p>}
    </section>
  )
}
