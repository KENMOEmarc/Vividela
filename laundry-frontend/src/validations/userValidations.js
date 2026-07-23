import { USERNAME_REGEX } from '../utils/constants'

export const validateUserField = (key, value, { mode = 'create' } = {}) => {
  switch (key) {
    case 'firstName':
    case 'lastName':
      if (!value?.trim()) return 'Ce champ est requis'
      if (value.trim().length < 2) return 'Minimum 2 caractères'
      return null

    case 'userName':
      if (!value?.trim()) return "Le nom d'utilisateur est requis"
      if (!USERNAME_REGEX.test(value.trim())) return '3-50 caractères : lettres, chiffres ou _'
      return null

    case 'email':
      if (!value?.trim()) return "L'email est requis"
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())) return 'Format email invalide'
      return null

    case 'phone':
      if (!value?.trim()) return "Le numéro de téléphone est requis";

      const digits = String(value).replace(/\D/g, ''); // ne garder que les chiffres

      if (!/^(00)?237[236]\d{8}$|^[236]\d{8}$/.test(digits)) {
        return 'Format téléphone invalide (exemple: 6 12 34 56 78)';
      }

      return null;

    case 'password':
      if (mode === 'edit' && !value) return null
      if (!value) return 'Le mot de passe est requis'
      if (value.length < 8) return 'Minimum 8 caractères'
      return null

    case 'role':
      if (!value) return 'Le rôle est requis'
      return null

    default:
      return null
  }
}

export const validateUserForm = (data, mode = 'create') => {
  const keys = ['firstName', 'lastName', 'userName', 'email', 'phone', 'role']
  if (mode === 'create' || data.password) keys.push('password')

  return keys.reduce((errors, key) => {
    const error = validateUserField(key, data[key], { mode })
    if (error) errors[key] = error
    return errors
  }, {})
}
