export const validateOrderItemField = (key, value) => {
  switch (key) {
    case 'articleId':
      if (!value) return 'L\'article est requis.'
      return null

    case 'quantity':
      if (value === '' || value === null || value === undefined) {
        return 'La quantité est requise.'
      }
      if (isNaN(Number(value))) {
        return 'Veuillez entrer un nombre valide.'
      }
      if (Number(value) < 1) {
        return 'La quantité doit être au moins 1.'
      }
      return null

    case 'unitPrice':
      if (value === '' || value === null || value === undefined) {
        return 'Le prix est requis.'
      }
      if (isNaN(Number(value))) {
        return 'Veuillez entrer un nombre valide.'
      }
      if (Number(value) < 0) {
        return 'Le prix doit être positif.'
      }
      return null

    case 'discount':
      if (value && (isNaN(Number(value)) || Number(value) < 0 || Number(value) > 100)) {
        return 'La réduction doit être entre 0 et 100.'
      }
      return null

    default:
      return null
  }
}

export const validateOrderItemForm = (data) => {
  return ['articleId', 'quantity', 'unitPrice'].reduce((errors, key) => {
    const error = validateOrderItemField(key, data[key])
    if (error) errors[key] = error
    return errors
  }, {})
}
