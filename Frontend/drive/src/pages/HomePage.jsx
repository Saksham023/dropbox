import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import FileUpload from '../components/FileUpload'
import FileList from '../components/FileList'
import CreateFolderForm from '../components/CreateFolderForm'
import { listFiles } from '../services/fileService'

export default function HomePage() {
  const navigate = useNavigate()
  const user = useMemo(() => {
    const raw = localStorage.getItem('currentUser')
    return raw ? JSON.parse(raw) : null
  }, [])

  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [folderPath, setFolderPath] = useState([])

  const currentParentId = folderPath.length > 0 ? folderPath[folderPath.length - 1].id : null

  const loadFiles = async () => {
    if (!user?.id) return

    setLoading(true)
    setMessage('')

    try {
      const dbFiles = await listFiles({ ownerId: user.id, parentId: currentParentId })
      setFiles(dbFiles)
    } catch (error) {
      console.error('[FILES] Load failed:', error)
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadFiles()
  }, [user?.id, currentParentId])

  const handleLogout = () => {
    localStorage.removeItem('currentUser')
    navigate('/login')
  }

  const handleOpenFolder = (folder) => {
    setFolderPath((prev) => [...prev, { id: folder.id, name: folder.name }])
  }

  const handleGoRoot = () => {
    setFolderPath([])
  }

  const handleGoBack = () => {
    setFolderPath((prev) => prev.slice(0, -1))
  }

  return (
    <main>
      <h1>Home</h1>
      <p>Logged in as: {user?.fullName}</p>
      <button type="button" onClick={handleLogout}>Logout</button>

      <p>
        Location: /{folderPath.map((f) => f.name).join('/')}
      </p>
      <button type="button" onClick={handleGoRoot}>Root</button>
      <button type="button" onClick={handleGoBack} disabled={folderPath.length === 0}>Back</button>

      <CreateFolderForm ownerId={user?.id} parentId={currentParentId} onFolderCreated={loadFiles} />
      <FileUpload ownerId={user?.id} parentId={currentParentId} onFileUploaded={loadFiles} />

      {loading ? <p>Loading files...</p> : <FileList files={files} onOpenFolder={handleOpenFolder} />}
      {message && <p>{message}</p>}
    </main>
  )
}
