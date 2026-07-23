/**
 * Déclenche le téléchargement d'un blob (ex: PDF) reçu depuis l'API.
 *
 * @param {Blob} blob      - Contenu binaire du fichier (ex: réponse axios avec responseType: 'blob')
 * @param {string} filename - Nom du fichier proposé au téléchargement
 */
export const triggerBlobDownload = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

/**
 * Ouvre un blob (ex: PDF) reçu depuis l'API dans un nouvel onglet du navigateur,
 * au lieu de forcer son téléchargement.
 *
 * @param {Blob} blob - Contenu binaire du fichier (ex: réponse axios avec responseType: 'blob')
 */
export const openBlobInNewTab = (blob) => {
  const url = window.URL.createObjectURL(blob)
  const newTab = window.open(url, '_blank', 'noopener,noreferrer')

  // Si le popup est bloqué par le navigateur, on retombe sur un lien classique
  // (l'utilisateur devra alors cliquer, mais ça reste une ouverture, pas un download forcé).
  if (!newTab) {
    const link = document.createElement('a')
    link.href = url
    link.target = '_blank'
    link.rel = 'noopener noreferrer'
    document.body.appendChild(link)
    link.click()
    link.remove()
  }

  // On libère l'URL objet un peu plus tard, le temps que l'onglet ait fini de charger le PDF.
  setTimeout(() => window.URL.revokeObjectURL(url), 60_000)
}
