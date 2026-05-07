import { useState } from 'react'
import { uploadFile } from '../services/uploadService'

export default function FileUpload({ ownerId, parentId, onFileUploaded }) {
  const [selectedFile, setSelectedFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

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

      const result = await uploadFile({ ownerId, parentId, file: selectedFile })
      console.log('[UPLOAD] Service result:', result)

      const localDownloadUrl = URL.createObjectURL(selectedFile)
      console.log('[UPLOAD] Local download URL created:', localDownloadUrl)

      onFileUploaded({
        id: result.fileId,
        name: selectedFile.name,
        size: selectedFile.size,
        mimeType: selectedFile.type || 'application/octet-stream',
        uploadedAt: new Date().toISOString(),
        objectKey: result.objectKey,
        localDownloadUrl,
      })

      setMessage('Upload successful')
      setSelectedFile(null)
      document.getElementById('file-input').value = ''
      console.log('[UPLOAD] Upload success and UI updated')
    } catch (error) {
      console.error('[UPLOAD] Upload failed:', error)
      setMessage(error.message)
    } finally {
      setLoading(false)
      console.log('[UPLOAD] Upload flow finished')
    }
  }

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
      {message && <p>{message}</p>}
    </section>
  )
}
