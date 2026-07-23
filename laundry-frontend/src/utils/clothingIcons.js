import { CLOTHING_TYPES } from './constants'

// Retourne un emoji adapté au type de vêtement. Les emoji sont utilisés pour
// garantir la compatibilité sans dépendre de noms d'icônes SVG spécifiques.
export const getClothingEmoji = (clothingType) => {
  switch (clothingType) {
    case CLOTHING_TYPES.SHIRT:
    case CLOTHING_TYPES.T_SHIRT:
      return '👕'
    case CLOTHING_TYPES.PANTS:
    case CLOTHING_TYPES.JEANS:
      return '👖'
    case CLOTHING_TYPES.SKIRT:
    case CLOTHING_TYPES.DRESS:
      return '👗'
    case CLOTHING_TYPES.JACKET:
    case CLOTHING_TYPES.COAT:
      return '🧥'
    case CLOTHING_TYPES.SUIT:
      return '🤵'
    case CLOTHING_TYPES.JACKET:
      return '🧥'
    case CLOTHING_TYPES.SWEATER:
      return '🧥'
    case CLOTHING_TYPES.BLOUSE:
      return '👚'
    case CLOTHING_TYPES.UNDERWEAR:
      return '🩲'
    case CLOTHING_TYPES.SOCKS:
      return '🧦'
    case CLOTHING_TYPES.VEST:
      return '🦺'
    case CLOTHING_TYPES.SHORTS:
      return '🩳'
    case CLOTHING_TYPES.SCARF:
      return '🧣'
    case CLOTHING_TYPES.GLOVES:
      return '🧤'
    case CLOTHING_TYPES.BELT:
      return '🪢'
    default:
      return '👚'
  }
}


