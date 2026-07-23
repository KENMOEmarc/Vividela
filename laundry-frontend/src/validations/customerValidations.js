export const validateCustomerField = (key, value) => {
  switch (key) {
    case 'firstName':
      if (!value?.trim()) return 'Le prénom est requis.'
      if (value.trim().length < 2) return 'Minimum 2 caractères.'
      return null

    case 'lastName':
      if (!value?.trim()) return 'Le nom est requis.'
      if (value.trim().length < 2) return 'Minimum 2 caractères.'
      return null

    case 'email':
      if (!value?.trim()) return 'L\'email est requis.'
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())) {
        return 'Email invalide.'
      }
      return null

    case 'phone':
      if (value && !/^[\d+\-\s()]+$/.test(value.trim())) {
        return 'Numéro de téléphone invalide.'
      }
      return null

    default:
      return null
  }
}

export const validateCustomerForm = (data) => {
  return ['firstName', 'lastName', 'email'].reduce((errors, key) => {
    const error = validateCustomerField(key, data[key])
    if (error) errors[key] = error
    return errors
  }, {})
}
