import { useEffect, useState } from 'react'
import {
  abortUpload,
  clearPendingUpload,
  getPendingUpload,
  resumeUpload,
  uploadFileInChunks,
} from '../services/fileService'

export default function FileUpload({ ownerId, parentId, onFileUploaded }) {
  const [selectedFile, setSelectedFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [progress, setProgress] = useState(null)
  const [pendingUpload, setPendingUpload] = useState(null)

  useEffect(() => {
    const pending = getPendingUpload()
    if (pending?.ownerId === ownerId) {
      setPendingUpload(pending)
    }
  }, [ownerId])

  const handleUpload = async () => {
    console.log('[UPLOAD] Button clicked')

    if (!ownerId || !selectedFile) {
      const msg = 'Login and file are required'
      console.log('[UPLOAD] Validation failed:', { ownerId, hasFile: Boolean(selectedFile) })
      setMessage(msg)
      return
    }

    setLoading(true)
    setMessage('')

    try {
      console.log('[UPLOAD] Starting upload for file:', {
        name: selectedFile.name,
        size: selectedFile.size,
        type: selectedFile.type,
      })

      const result = await uploadFileInChunks({
        ownerId,
        parentId,
        file: selectedFile,
        onProgress: setProgress,
      })
      console.log('[UPLOAD] Service result:', result)

      setMessage('Upload successful')
      setPendingUpload(null)
      setSelectedFile(null)
      document.getElementById('file-input').value = ''
      onFileUploaded()
      console.log('[UPLOAD] Upload success and UI updated')
    } catch (error) {
      console.error('[UPLOAD] Upload failed:', error)
      setMessage(error.message)
      setPendingUpload(getPendingUpload())
    } finally {
      setLoading(false)
      console.log('[UPLOAD] Upload flow finished')
    }
  }

  const handleResume = async () => {
    if (!ownerId || !selectedFile || !pendingUpload) {
      setMessage('Select the original file to resume')
      return
    }

    setLoading(true)
    setMessage('')

    try {
      const result = await resumeUpload({
        ownerId,
        file: selectedFile,
        uploadState: pendingUpload,
        onProgress: setProgress,
      })
      console.log('[UPLOAD] Resume result:', result)

      setMessage('Upload resumed and completed')
      setPendingUpload(null)
      setSelectedFile(null)
      document.getElementById('file-input').value = ''
      onFileUploaded()
    } catch (error) {
      console.error('[UPLOAD] Resume failed:', error)
      setMessage(error.message)
      setPendingUpload(getPendingUpload())
    } finally {
      setLoading(false)
    }
  }

  const handleCancelPendingUpload = async () => {
    if (!pendingUpload) return

    setLoading(true)
    setMessage('')

    try {
      await abortUpload({
        ownerId,
        fileId: pendingUpload.fileId,
        uploadSessionId: pendingUpload.uploadSessionId,
      })
      clearPendingUpload()
      setPendingUpload(null)
      setProgress(null)
      setMessage('Pending upload cancelled')
      onFileUploaded()
    } catch (error) {
      console.error('[UPLOAD] Cancel failed:', error)
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  const canResume = pendingUpload && selectedFile

  return (
    <section>
      <h2>Upload File</h2>
      <div>
        <input
          id="file-input"
          type="file"
          onChange={(e) => {
            const file = e.target.files?.[0] || null
            setSelectedFile(file)
            console.log('[UPLOAD] File selected:', file)
          }}
        />
      </div>
      <button type="button" onClick={handleUpload} disabled={loading}>
        {loading ? 'Uploading...' : 'Upload'}
      </button>
      {pendingUpload && (
        <div>
          <p>Resumable upload: {pendingUpload.name}</p>
          <button type="button" onClick={handleResume} disabled={loading || !canResume}>
            Resume
          </button>
          <button type="button" onClick={handleCancelPendingUpload} disabled={loading}>
            Cancel pending upload
          </button>
        </div>
      )}
      {progress && (
        <p>
          {progress.status}: {progress.uploadedChunks}/{progress.totalChunks} chunks ({progress.percentage}%)
        </p>
      )}
      {message && <p>{message}</p>}
    </section>
  )
}
