/**
 * Fonctions de validation côté frontend.
 *
 * DOUBLE VALIDATION :
 *   Les mêmes règles existent côté backend (Jakarta Validation).
 *   La validation frontend améliore l'UX (feedback immédiat sans appel réseau).
 *   La validation backend est la seule source de vérité en matière de sécurité
 *   (le client peut toujours être contourné — ne jamais faire confiance au client).
 *
 * Chaque fonction retourne :
 *   - null  → champ valide
 *   - string → message d'erreur à afficher
 */

import { PASSWORD_MIN_LENGTH, PASSWORD_REGEX, USERNAME_REGEX } from './constants'

/** Valide que le champ n'est pas vide. */
export const validateRequired = (value, fieldName = 'Ce champ') => {
  if (!value || value.trim() === '') {
    return `${fieldName} est obligatoire`
  }
  return null
}

/** Valide le format d'une adresse email. */
export const validateEmail = (email) => {
  const required = validateRequired(email, 'L\'email')
  if (required) return required

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) {
    return 'Format d\'email invalide (exemple: utilisateur@domaine.com)'
  }
  return null
}

export const validatePhone = (phone) => {
  const required = validateRequired(phone, "Le numéro de téléphone");
  if (required) return required;

  // Convertit en chaîne pour gérer les numéros transmis en type Number
  const digits = String(phone).replace(/\D/g, "");

  // Format accepté :
  //   - National : 9 chiffres commençant par 2, 3 ou 6 (ex: 612345678, 233445566)
  //   - International : 237 + 9 chiffres commençant par 2, 3, 6 (total 12)
  //   - International avec 00 : 00237 + 9 chiffres (total 14)
  const phoneRegex = /^(00)?237[236]\d{8}$|^[236]\d{8}$/;

  if (!phoneRegex.test(digits)) {
    return "Numéro camerounais invalide (ex: 6 55 85 26 35, +237 6 88 77 45 26, 2 33 44 55 66)";
  }

  return null;
};

/** Valide le nom d'utilisateur. */
export const validateUsername = (userName) => {
  const required = validateRequired(userName, 'Le nom d\'utilisateur')
  if (required) return required

  if (!USERNAME_REGEX.test(userName)) {
    return 'Le nom d\'utilisateur doit contenir 3 à 50 caractères (lettres, chiffres, underscore uniquement)'
  }
  return null
}

/** Valide la complexité du mot de passe. */
export const validatePassword = (password) => {
  const required = validateRequired(password, 'Le mot de passe')
  if (required) return required

  if (password.length < PASSWORD_MIN_LENGTH) {
    return `Le mot de passe doit contenir au moins ${PASSWORD_MIN_LENGTH} caractères`
  }

  if (!PASSWORD_REGEX.test(password)) {
    return 'Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial (@$!%*?&)'
  }
  return null
}

/** Valide la correspondance du mot de passe et de sa confirmation. */
export const validateConfirmPassword = (password, confirmPassword) => {
  const required = validateRequired(confirmPassword, 'La confirmation du mot de passe')
  if (required) return required

  if (password !== confirmPassword) {
    return 'Les mots de passe ne correspondent pas'
  }
  return null
}

/** Valide un champ prénom ou nom. */
export const validateName = (name, fieldName = 'Ce champ') => {
  const required = validateRequired(name, fieldName)
  if (required) return required

  if (name.trim().length > 100) {
    return `${fieldName} ne peut pas dépasser 100 caractères`
  }
  return null
}

/**
 * Valide le formulaire d'inscription complet.
 * Retourne un objet { champ: messageErreur } ou {} si tout est valide.
 */
export const validateRegisterForm = (formData) => {
  const errors = {}

  const usernameError = validateUsername(formData.userName)
  if (usernameError) errors.userName = usernameError

  const emailError = validateEmail(formData.email)
  if (emailError) errors.email = emailError

  const phoneError = validatePhone(formData.phone)
  if (phoneError) errors.phone = phoneError

  const firstNameError = validateName(formData.firstName, 'Le prénom')
  if (firstNameError) errors.firstName = firstNameError

  const lastNameError = validateName(formData.lastName, 'Le nom de famille')
  if (lastNameError) errors.lastName = lastNameError

  const passwordError = validatePassword(formData.password)
  if (passwordError) errors.password = passwordError

  const confirmError = validateConfirmPassword(formData.password, formData.confirmPassword)
  if (confirmError) errors.confirmPassword = confirmError

  return errors
}

/**
 * Valide le formulaire de connexion complet.
 */
export const validateLoginForm = (formData) => {
  const errors = {}

  const identifierError = validateRequired(formData.identifier, 'L\'identifiant')
  if (identifierError) errors.identifier = identifierError

  const passwordError = validateRequired(formData.password, 'Le mot de passe')
  if (passwordError) errors.password = passwordError

  return errors
}
