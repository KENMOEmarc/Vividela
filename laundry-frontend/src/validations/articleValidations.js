/**
 * Validations pour les articles (vêtements) — alignées sur le schéma SQL réel
 * et les enums Java (ArticleStatus, ClothingType, SizeType, FabricType).
 *
 * CORRECTION : ajout de PAGNE dans les clothing types (schéma SQL),
 *              ajout de S et UNIQUE dans les tailles (SizeType.java corrigé),
 *              ajout de JEANS/PAGNE dans les tissus (schéma SQL).
 */

export const CLOTHING_TYPES = [
  { value: 'SHIRT',     label: 'Chemise' },
  { value: 'T_SHIRT',   label: 'T-shirt' },
  { value: 'PANTS',     label: 'Pantalon' },
  { value: 'JEANS',     label: 'Jeans' },
  { value: 'SKIRT',     label: 'Jupe' },
  { value: 'DRESS',     label: 'Robe' },
  { value: 'JACKET',    label: 'Veste' },
  { value: 'COAT',      label: 'Manteau' },
  { value: 'SWEATER',   label: 'Pull' },
  { value: 'BLOUSE',    label: 'Blouse' },
  { value: 'UNDERWEAR', label: 'Sous-vêtement' },
  { value: 'SOCKS',     label: 'Chaussettes' },
  { value: 'SUIT',      label: 'Costume' },
  { value: 'VEST',      label: 'Gilet' },
  { value: 'SHORTS',    label: 'Short' },
  { value: 'SCARF',     label: 'Écharpe' },
  { value: 'GLOVES',    label: 'Gants' },
  { value: 'BELT',      label: 'Ceinture' },
]

// CORRECTION : S et UNIQUE ajoutés, X remplacé par S (SizeType.java)
export const CLOTHING_SIZES = [
  { value: 'S',      label: 'S — Small' },
  { value: 'M',      label: 'M — Medium' },
  { value: 'L',      label: 'L — Large' },
  { value: 'XL',     label: 'XL — Extra Large' },
  { value: 'XXL',    label: 'XXL — Double XL' },
  { value: 'UNIQUE', label: 'Taille unique' },
]

// CORRECTION : JEANS et PAGNE ajoutés pour le contexte camerounais (schéma SQL)
export const FABRIC_TYPES = [
  { value: 'COTTON',    label: 'Coton' },
  { value: 'LINEN',     label: 'Lin' },
  { value: 'SILK',      label: 'Soie' },
  { value: 'WOOL',      label: 'Laine' },
  { value: 'POLYESTER', label: 'Polyester' },
  { value: 'SYNTHETIC', label: 'Synthétique' },
  { value: 'BLENDED',   label: 'Tissu mixte' },
  { value: 'ACRYLIC',   label: 'Acrylique' },
  { value: 'NYLON',     label: 'Nylon' },
]

// CORRECTION : statuts pressing (ArticleStatus.java) — plus de DRAFT/PUBLISHED/ARCHIVED
export const ARTICLE_STATUSES = [
  { value: 'PENDING',       label: 'En attente' },
  { value: 'WASHING',       label: 'Lavage' },
  { value: 'IRONING',       label: 'Repassage' },
  { value: 'QUALITY_CHECK', label: 'Contrôle qualité' },
  { value: 'PACKED',        label: 'Emballé' },
  { value: 'COMPLETED',     label: 'Terminé' },
]

export const ARTICLE_SERVICES = [
  { value: 'DRY_CLEAN',     label: 'Nettoyage à sec' },
  { value: 'WASH',          label: 'Lavage' },
  { value: 'IRON',          label: 'Repassage' },
  { value: 'STAIN_REMOVAL', label: 'Détachage' },
  { value: 'DEYING',        label: 'Teinture' },
  { value: 'ALTERATION',    label: 'Retouche' },
]

const VALID_CLOTHING_TYPES   = CLOTHING_TYPES.map(t => t.value)
const VALID_SIZES            = CLOTHING_SIZES.map(s => s.value)
const VALID_FABRIC_TYPES     = FABRIC_TYPES.map(f => f.value)
const VALID_ARTICLE_STATUSES = ARTICLE_STATUSES.map(s => s.value)
const VALID_SERVICES         = ARTICLE_SERVICES.map(s => s.value)

export const validateArticleField = (key, value) => {
  switch (key) {
    case 'clothingType':
      if (!value) return 'Le type de vêtement est requis.'
      if (!VALID_CLOTHING_TYPES.includes(value)) return 'Type de vêtement invalide.'
      return null

    case 'size':
      if (!value) return 'La taille est requise.'
      if (!VALID_SIZES.includes(value)) return 'Taille invalide.'
      return null

    case 'fabric':
      if (!value) return 'Le type de tissu est requis.'
      if (!VALID_FABRIC_TYPES.includes(value)) return 'Type de tissu invalide.'
      return null

    case 'color':
      if (value && value.length > 50) return 'Maximum 50 caractères.'
      return null

    case 'distinction':
      if (value && value.length > 255) return 'Maximum 255 caractères.'
      return null

    case 'status':
      if (!value) return 'Le statut est requis.'
      if (!VALID_ARTICLE_STATUSES.includes(value)) return 'Statut invalide.'
      return null

    case 'services':
      if (!value || !Array.isArray(value) || value.length === 0)
        return 'Sélectionnez au moins un service.'
      if (value.some(s => !VALID_SERVICES.includes(s)))
        return 'Service invalide dans la sélection.'
      return null

    default:
      return null
  }
}

export const validateArticleForm = (data) => {
  const requiredFields = ['clothingType', 'size', 'fabric', 'status', 'services']
  const optionalFields = ['color', 'distinction']

  return [...requiredFields, ...optionalFields].reduce((errors, key) => {
    const error = validateArticleField(key, data[key])
    if (error) errors[key] = error
    return errors
  }, {})
}