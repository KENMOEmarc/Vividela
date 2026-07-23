/**
 * Composant d'alerte réutilisable.
 *
 * Affiche les messages de succès, d'erreur, d'information ou d'avertissement
 * avec l'icône et la couleur Bootstrap appropriées.
 *
 * PROPS :
 *   type    : 'success' | 'danger' | 'warning' | 'info'  (défaut: 'danger')
 *   message : string ou string[] — un ou plusieurs messages à afficher
 *   onClose : fonction de fermeture (optionnel → bouton × affiché si fourni)
 *
 * BOOTSTRAP :
 *   alert alert-{type}    → couleur et style de l'alerte
 *   alert-dismissible     → ajoute le bouton de fermeture
 *   d-flex align-items-start → icône alignée avec le texte
 */

const ICONS = {
  success: 'bi-check-circle-fill',
  danger:  'bi-exclamation-triangle-fill',
  warning: 'bi-exclamation-circle-fill',
  info:    'bi-info-circle-fill'
}

const Alert = ({ type = 'danger', message, onClose }) => {
  if (!message) return null

  const icon     = ICONS[type] || ICONS.danger
  const messages = Array.isArray(message) ? message : [message]

  return (
    <div
      className={`alert alert-${type} ${onClose ? 'alert-dismissible' : ''} d-flex align-items-start fade show`}
      role="alert"
    >
      <i className={`bi ${icon} me-2 mt-1 flex-shrink-0`}></i>

      <div className="flex-grow-1">
        {messages.length === 1 ? (
          <span>{messages[0]}</span>
        ) : (
          <ul className="mb-0 ps-3">
            {messages.map((msg, idx) => (
              <li key={idx}>{msg}</li>
            ))}
          </ul>
        )}
      </div>

      {onClose && (
        <button
          type="button"
          className="btn-close ms-2"
          aria-label="Fermer"
          onClick={onClose}
        />
      )}
    </div>
  )
}

export default Alert
