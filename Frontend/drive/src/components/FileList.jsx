function formatSize(size) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

export default function FileList({ files, onOpenFolder }) {
  return (
    <section>
      <h2>Files</h2>
      {files.length === 0 ? (
        <p>No files found.</p>
      ) : (
        <ul>
          {files.map((file) => (
            <li key={file.id}>
              <strong>{file.name}</strong> [{file.type}] ({formatSize(file.size)}) - status: {file.status}{' '}
              {file.type === 'FOLDER' ? (
                <button type="button" onClick={() => onOpenFolder(file)}>Open</button>
              ) : file.downloadUrl ? (
                <a href={file.downloadUrl} target="_blank" rel="noreferrer">Download</a>
              ) : (
                <span>(download endpoint pending)</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
