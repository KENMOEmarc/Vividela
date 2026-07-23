import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { useAuth } from '../../context/AuthContext'
import { validateRegisterForm } from '../../utils/validators'
import { ROUTES } from '../../utils/constants'
import { UserPlus, AtSign, Mail, Phone, Lock, Eye, EyeOff, Loader2 } from 'lucide-react'

const INPUT_BASE = 'w-full pr-4 py-2.5 border rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 disabled:bg-slate-50 disabled:cursor-not-allowed'

const INITIAL_FORM = {
  userName: '', email: '', phone: '', firstName: '', lastName: '',
  password: '', confirmPassword: ''
}

const RegisterForm = () => {
  const [formData, setFormData]         = useState(INITIAL_FORM)
  const [fieldErrors, setFieldErrors]   = useState({})
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm,  setShowConfirm]  = useState(false)

  const { register, isLoading, error, clearError } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (error) { toast.error(error); clearError() }
  }, [error, clearError])

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
    if (fieldErrors[name]) setFieldErrors(prev => ({ ...prev, [name]: null }))
    if (error) clearError()
  }

   const handleBlur = (e) => {
    const { name } = e.target
    const allErrors = validateRegisterForm(formData)

    const confirmPasswordError =
      name === 'confirmPassword' &&
      formData.confirmPassword &&
      formData.password !== formData.confirmPassword
        ? 'Les mots de passe ne correspondent pas'
        : null

    const error = allErrors[name] || confirmPasswordError

    if (error) {
      setFieldErrors(prev => ({ ...prev, [name]: error }))
    } else if (fieldErrors[name]) {
      setFieldErrors(prev => ({ ...prev, [name]: null }))
    }
  }

   const handleSubmit = async (e) => {
    e.preventDefault()
    const errors = validateRegisterForm(formData)

    // Validate password match
    if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Les mots de passe ne correspondent pas'
    }

    if (Object.keys(errors).length > 0) { 
      setFieldErrors(errors)
      return 
    }

    const result = await register(formData)
    if (result.success) navigate(ROUTES.DASHBOARD, { replace: true })
  }


  const fieldClass = (name) =>
    `${INPUT_BASE} pl-10 ${
      !formData[name]       ? 'border-slate-300 focus:border-blue-500 focus:ring-blue-100' :
      fieldErrors[name]     ? 'border-red-400 focus:border-red-400 focus:ring-red-100 bg-red-50' :
                              'border-green-400 focus:border-green-400 focus:ring-green-100'
    }`

  const simpleFieldClass = (name) =>
    `${INPUT_BASE} px-3 ${
      !formData[name]       ? 'border-slate-300 focus:border-blue-500 focus:ring-blue-100' :
      fieldErrors[name]     ? 'border-red-400 focus:border-red-400 focus:ring-red-100 bg-red-50' :
                              'border-green-400 focus:border-green-400 focus:ring-green-100'
    }`

  const Label = ({ htmlFor, children }) => (
    <label htmlFor={htmlFor} className="block text-sm font-semibold text-slate-700 mb-1.5">
      {children} <span className="text-red-500">*</span>
    </label>
  )

  const Error = ({ name }) => fieldErrors[name]
    ? <p className="mt-1 text-sm text-red-500">{fieldErrors[name]}</p>
    : null

  return (
    <div className="bg-white rounded-2xl shadow-lg border border-slate-100 animate-fade-in-up">
      <div className="p-8">

        {/* En-tête */}
        <div className="text-center mb-7">
          <div className="inline-flex items-center justify-center w-14 h-14 bg-blue-100 rounded-2xl mb-4">
            <UserPlus className="text-blue-600" size={26} />
          </div>
          <h2 className="text-2xl font-bold text-slate-900">Créer un compte</h2>
          <p className="text-slate-500 text-sm mt-1">Rejoignez-nous dès aujourd'hui</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">

          {/* Prénom / Nom */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="firstName">Prénom</Label>
              <input
                id="firstName" type="text" name="firstName"
                className={simpleFieldClass('firstName')}
                value={formData.firstName}
                onChange={handleChange} onBlur={handleBlur}
                placeholder="Jean" autoComplete="given-name" disabled={isLoading}
              />
              <Error name="firstName" />
            </div>
            <div>
              <Label htmlFor="lastName">Nom</Label>
              <input
                id="lastName" type="text" name="lastName"
                className={simpleFieldClass('lastName')}
                value={formData.lastName}
                onChange={handleChange} onBlur={handleBlur}
                placeholder="Dupont" autoComplete="family-name" disabled={isLoading}
              />
              <Error name="lastName" />
            </div>
          </div>

          {/* Nom d'utilisateur */}
          <div>
            <Label htmlFor="userName">Nom d'utilisateur</Label>
            <div className="relative">
              <AtSign size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="userName" type="text" name="userName"
                className={fieldClass('userName')}
                value={formData.userName}
                onChange={handleChange} onBlur={handleBlur}
                placeholder="jean_dupont" autoComplete="username" disabled={isLoading}
              />
            </div>
            <p className="mt-1 text-xs text-slate-400">Lettres, chiffres et underscores (3-50 car.)</p>
            <Error name="userName" />
          </div>

          {/* Email */}
          <div>
            <Label htmlFor="email">Adresse email</Label>
            <div className="relative">
              <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="email" type="email" name="email"
                className={fieldClass('email')}
                value={formData.email}
                onChange={handleChange} onBlur={handleBlur}
                placeholder="jean@exemple.com" autoComplete="email" disabled={isLoading}
              />
            </div>
            <Error name="email" />
          </div>

          {/* Numéro de Téléphone */}
          <div>
            <Label htmlFor="phone">Numéro de téléphone</Label>
            <div className="relative">
              <Phone size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="phone" type="tel" name="phone"
                className={fieldClass('phone')}
                value={formData.phone}
                onChange={handleChange} onBlur={handleBlur}
                placeholder="6 12 34 57 89" autoComplete="tel" disabled={isLoading}
              />
            </div>
            <Error name="phone" />
          </div>

          {/* Mot de passe */}
          <div>
            <Label htmlFor="password">Mot de passe</Label>
            <div className="relative">
              <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                name="password"
                className={`${fieldClass('password')} pr-10`}
                value={formData.password}
                onChange={handleChange} onBlur={handleBlur}
                placeholder="••••••••" autoComplete="new-password" disabled={isLoading}
              />
              <button type="button" onClick={() => setShowPassword(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
                tabIndex="-1">
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            <p className="mt-1 text-xs text-slate-400">Min. 8 car. avec majuscule, chiffre et caractère spécial</p>
            <Error name="password" />
          </div>

          {/* Confirmation */}
          <div>
            <Label htmlFor="confirmPassword">Confirmer le mot de passe</Label>
            <div className="relative">
              <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="confirmPassword"
                type={showConfirm ? 'text' : 'password'}
                name="confirmPassword"
                className={`${fieldClass('confirmPassword')} pr-10`}
                value={formData.confirmPassword}
                onChange={handleChange} onBlur={handleBlur}
                placeholder="••••••••" autoComplete="new-password" disabled={isLoading}
              />
              <button type="button" onClick={() => setShowConfirm(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
                tabIndex="-1">
                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            <Error name="confirmPassword" />
          </div>

          {/* Bouton */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 mt-2"
          >
            {isLoading ? (
              <><Loader2 size={17} className="animate-spin" /> Création du compte...</>
            ) : (
              <><UserPlus size={17} /> Créer mon compte</>
            )}
          </button>
        </form>

        <div className="text-center mt-5 text-sm">
          <span className="text-slate-500">Déjà un compte ? </span>
          <Link to={ROUTES.LOGIN} className="text-blue-600 font-semibold hover:underline">
            Se connecter
          </Link>
        </div>
      </div>
    </div>
  )
}

export default RegisterForm
