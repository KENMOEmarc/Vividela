import { useState, useEffect } from 'react'
import { toast } from 'react-toastify'
import { Tag, TriangleAlert, Ruler, Loader2, PackagePlus, Triangle } from 'lucide-react'
import { MEASUREMENT_UNITS, MEASUREMENT_UNIT_LABELS } from '../../utils/constants'
import { validateProductForm } from '../../validations/productValidations'

// ─── Styles ──────────────────────────────────────────────────────────────────
const INPUT_BASE =
  'w-full pr-4 py-2.5 border rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 disabled:bg-slate-50 disabled:cursor-not-allowed'

const INITIAL_FORM = {
  name: '',
  thresholdValue: '',
  measurementUnit: '',
}

// ─── Composant ───────────────────────────────────────────────────────────────
const ProductForm = ({
  mode = 'create', // 'create' | 'edit'
  product = null,  // données existantes (mode édition)
  onSubmit,        // async (formData) => { ... }
  isSubmitting = false,
}) => {
  const [formData, setFormData] = useState(() => {
    if (mode === 'edit' && product) {
      return {
        name: product.name || '',
        thresholdValue: product.thresholdValue != null ? String(product.thresholdValue) : '',
        measurementUnit: product.measurementUnit || '',
      }
    }
    return INITIAL_FORM
  })
  const [fieldErrors, setFieldErrors] = useState({})

  // Synchroniser le formulaire si le produit change en mode édition
  useEffect(() => {
    if (mode === 'edit' && product) {
      setFormData({
        name: product.name || '',
        thresholdValue: product.thresholdValue != null ? String(product.thresholdValue) : '',
        measurementUnit: product.measurementUnit || '',
      })
      setFieldErrors({})
    }
  }, [product, mode])

  // ── Handlers ───────────────────────────────────────────────────────────
  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    // Efface l'erreur du champ modifié
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }))
    }
  }

  const handleBlur = (e) => {
    const { name } = e.target
    const errors = validateProductForm(formData)
    if (errors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: errors[name] }))
    } else if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }))
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errors = validateProductForm(formData)

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      // focus premier champ en erreur
      const firstError = Object.keys(errors)[0]
      document.getElementById(firstError)?.focus()
      return
    }

    // Préparation des données
    const payload = {
      ...formData,
      thresholdValue: parseFloat(formData.thresholdValue),
    }

    try {
      await onSubmit?.(payload)
    } catch (err) {
      toast.error(err?.response?.data?.message || err?.message || 'Une erreur est survenue.')
    }
  }

  // ── Classes dynamiques ─────────────────────────────────────────────────
  const fieldClass = (name) =>
    `${INPUT_BASE} pl-10 ${
      !formData[name]
        ? 'border-slate-300 focus:border-blue-500 focus:ring-blue-100'
        : fieldErrors[name]
          ? 'border-red-400 focus:border-red-400 focus:ring-red-100 bg-red-50'
          : 'border-green-400 focus:border-green-400 focus:ring-green-100'
    }`

  // ── Sous-composants internes ───────────────────────────────────────────
  const Label = ({ htmlFor, children, required = true }) => (
    <label htmlFor={htmlFor} className="block text-sm font-semibold text-slate-700 mb-1.5">
      {children} {required && <span className="text-red-500">*</span>}
    </label>
  )

  const Error = ({ name }) =>
    fieldErrors[name] ? (
      <p className="mt-1 text-sm text-red-500">{fieldErrors[name]}</p>
    ) : null

  // ── Rendu ──────────────────────────────────────────────────────────────
  return (
    <div className="bg-white rounded-2xl shadow-lg border border-slate-100 animate-fade-in-up">
      <div className="p-8">
        {/* En-tête */}
        <div className="text-center mb-7">
          <div className="inline-flex items-center justify-center w-14 h-14 bg-blue-100 rounded-2xl mb-4">
            <PackagePlus className="text-blue-600" size={26} />
          </div>
          <h2 className="text-2xl font-bold text-slate-900">
            {mode === 'create' ? 'Nouveau produit' : 'Modifier le produit'}
          </h2>
          <p className="text-slate-500 text-sm mt-1">
            {mode === 'create'
              ? 'Ajoutez un produit à votre catalogue'
              : 'Modifiez les informations du produit'}
          </p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          {/* Nom */}
          <div>
            <Label htmlFor="name">Nom du produit</Label>
            <div className="relative">
              <Tag size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="name"
                type="text"
                name="name"
                className={fieldClass('name')}
                value={formData.name}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="Ex: Riz basmati"
                autoComplete="off"
                disabled={isSubmitting}
              />
            </div>
            <Error name="name" />
          </div>

          {/* Valeur seuil */}
          <div>
            <Label htmlFor="thresholdValue">Valeur seuil</Label>
            <div className="relative">
              <TriangleAlert size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="thresholdValue"
                type="number"
                name="thresholdValue"
                className={fieldClass('thresholdValue')}
                value={formData.thresholdValue}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="0.00"
                step="0.01"
                min="0"
                disabled={isSubmitting}
              />
            </div>
            <p className="mt-1 text-xs text-slate-400">Quantité minimale avant alerte de stock</p>
            <Error name="thresholdValue" />
          </div>

          {/* Unité de mesure */}
          <div>
            <Label htmlFor="measurementUnit">Unité de mesure</Label>
            <div className="relative">
              <Ruler size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <select
                id="measurementUnit"
                name="measurementUnit"
                className={`${fieldClass('measurementUnit')} appearance-none bg-white`}
                value={formData.measurementUnit}
                onChange={handleChange}
                onBlur={handleBlur}
                disabled={isSubmitting}
              >
                <option value="" disabled>
                  -- Choisissez une unité --
                </option>
                {Object.entries(MEASUREMENT_UNITS).map(([key, value]) => (
                  <option key={key} value={value}>
                    {MEASUREMENT_UNIT_LABELS[value] ?? value}
                  </option>
                ))}
              </select>
            </div>
            <Error name="measurementUnit" />
          </div>

          {/* Bouton de soumission */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 mt-2"
          >
            {isSubmitting ? (
              <>
                <Loader2 size={17} className="animate-spin" />
                {mode === 'create' ? 'Création en cours...' : 'Enregistrement...'}
              </>
            ) : (
              <>
                <PackagePlus size={17} />
                {mode === 'create' ? 'Créer le produit' : 'Enregistrer les modifications'}
              </>
            )}
          </button>
        </form>

        {/* Lien retour */}
        <div className="text-center mt-5 text-sm">
          <Link to={ROUTES.PRODUCTS} className="text-blue-600 font-semibold hover:underline">
            Retour à la liste des produits
          </Link>
        </div>
      </div>
    </div>
  )
}

export default ProductForm