import { CLOTHING_TYPES, SERVICE_TYPES } from '../utils/constants'

export const validateServicePriceField = (key, value, mode = 'create') => {
  switch (key) {
    case 'clothingType':
      if (mode === 'edit') return null // non modifiable après création
      if (!value) return 'Le type de vêtement est requis.'
      if (!Object.values(CLOTHING_TYPES).includes(value)) return 'Type de vêtement invalide.'
      return null

    case 'service':
      if (mode === 'edit') return null // non modifiable après création
      if (!value) return 'Le type de service est requis.'
      if (!Object.values(SERVICE_TYPES).includes(value)) return 'Type de service invalide.'
      return null

    case 'price':
      if (value === '' || value === null || value === undefined) {
        return 'Le prix est requis.'
      }
      if (isNaN(Number(value))) {
        return 'Veuillez entrer un nombre valide.'
      }
      if (Number(value) <= 0) {
        return 'Le prix doit être supérieur à 0.'
      }
      return null

    default:
      return null
  }
}

export const validateServicePriceForm = (data, mode = 'create') => {
  const keys = mode === 'edit'
    ? ['price']
    : ['clothingType', 'service', 'price']

  return keys.reduce((errors, key) => {
    const error = validateServicePriceField(key, data[key], mode)
    if (error) errors[key] = error
    return errors
  }, {})
}
