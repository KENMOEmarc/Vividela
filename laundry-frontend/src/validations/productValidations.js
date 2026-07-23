import { MEASUREMENT_UNITS } from '../utils/constants'

export const validateProductField = (key, value) => {
  switch (key) {
    case 'name':
      if (!value?.trim()) return 'Le nom du produit est requis.'
      if (value.trim().length < 2) return 'Minimum 2 caractères.'
      return null

    case 'thresholdValue':
      if (value === '' || value === null || value === undefined) {
        return 'La valeur seuil est requise.'
      }
      if (isNaN(Number(value))) {
        return 'Veuillez entrer un nombre valide.'
      }
      if (Number(value) < 0) {
        return 'Veuillez entrer un nombre positif.'
      }
      return null

    case 'measurementUnit':
      if (!value) return "L'unité de mesure est requise."
      if (!Object.values(MEASUREMENT_UNITS).includes(value)) {
        return 'Unité invalide.'
      }
      return null

    default:
      return null
  }
}

export const validateProductForm = (data) => {
  return ['name', 'thresholdValue', 'measurementUnit'].reduce((errors, key) => {
    const error = validateProductField(key, data[key])
    if (error) errors[key] = error
    return errors
  }, {})
}
