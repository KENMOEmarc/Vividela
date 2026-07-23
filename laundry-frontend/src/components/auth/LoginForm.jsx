import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { toast } from 'react-toastify'
import { useAuth } from '../../context/AuthContext'
import { validateLoginForm } from '../../utils/validators'
import { ROUTES } from '../../utils/constants'
import { LogIn, User, Lock, Eye, EyeOff, Loader2 } from 'lucide-react'

const INPUT_BASE = 'w-full pr-4 py-2.5 border rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 disabled:bg-slate-50 disabled:cursor-not-allowed'

const LoginForm = () => {
  const [formData, setFormData]         = useState({ identifier: '', password: '' })
  const [fieldErrors, setFieldErrors]   = useState({})
  const [showPassword, setShowPassword] = useState(false)

  const { login, isLoading, error, clearError } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const inputRef = useRef(null)

  useEffect(() => { inputRef.current?.focus() }, [])

  useEffect(() => {
    if (error) { toast.error(error); clearError() }
  }, [error, clearError])

  useEffect(() => {
    if (location.state?.from) {
      toast.info('Veuillez vous connecter pour accéder à cette page.')
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const from = location.state?.from?.pathname || ROUTES.DASHBOARD

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
    if (fieldErrors[name]) setFieldErrors(prev => ({ ...prev, [name]: null }))
    if (error) clearError()
  }

  const handleBlur = (e) => {
    const { name } = e.target
    const allErrors = validateLoginForm(formData)
    if (allErrors[name]) setFieldErrors(prev => ({ ...prev, [name]: allErrors[name] }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errors = validateLoginForm(formData)
    if (Object.keys(errors).length > 0) { setFieldErrors(errors); return }
    const result = await login(formData)
    if (result.success) navigate(from, { replace: true })
  }

  const fieldClass = (name) =>
    `${INPUT_BASE} pl-10 ${
      !formData[name]       ? 'border-slate-300 focus:border-blue-500 focus:ring-blue-100' :
      fieldErrors[name]     ? 'border-red-400 focus:border-red-400 focus:ring-red-100 bg-red-50' :
                              'border-green-400 focus:border-green-400 focus:ring-green-100'
    }`

  return (
    <div className="bg-white rounded-2xl shadow-lg border border-slate-100 animate-fade-in-up">
      <div className="p-8">

        {/* En-tête */}
        <div className="text-center mb-7">
          <div className="inline-flex items-center justify-center w-14 h-14 bg-blue-100 rounded-2xl mb-4">
            <LogIn className="text-blue-600" size={26} />
          </div>
          <h2 className="text-2xl font-bold text-slate-900">Connexion</h2>
          <p className="text-slate-500 text-sm mt-1">Accédez à votre compte</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">

          {/* Identifiant */}
          <div>
            <label htmlFor="identifier" className="block text-sm font-semibold text-slate-700 mb-1.5">
              Email ou nom d'utilisateur
            </label>
            <div className="relative">
              <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="identifier"
                ref={inputRef}
                type="text"
                name="identifier"
                className={fieldClass('identifier')}
                value={formData.identifier}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="jean@exemple.com ou jean_dupont"
                autoComplete="username"
                disabled={isLoading}
              />
            </div>
            {fieldErrors.identifier && (
              <p className="mt-1 text-sm text-red-500">{fieldErrors.identifier}</p>
            )}
          </div>

          {/* Mot de passe */}
          <div>
            <label htmlFor="password" className="block text-sm font-semibold text-slate-700 mb-1.5">
              Mot de passe
            </label>
            <div className="relative">
              <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                name="password"
                className={`${fieldClass('password')} pr-10`}
                value={formData.password}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="••••••••"
                autoComplete="current-password"
                disabled={isLoading}
              />
              <button
                type="button"
                onClick={() => setShowPassword(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
                tabIndex="-1"
                aria-label={showPassword ? 'Masquer' : 'Afficher'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {fieldErrors.password && (
              <p className="mt-1 text-sm text-red-500">{fieldErrors.password}</p>
            )}
          </div>

          {/* Bouton */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 mt-2"
          >
            {isLoading ? (
              <><Loader2 size={17} className="animate-spin" /> Connexion en cours...</>
            ) : (
              <><LogIn size={17} /> Se connecter</>
            )}
          </button>
        </form>

        <div className="text-center mt-5 text-sm">
          <span className="text-slate-500">Pas encore de compte ? </span>
          <Link to={ROUTES.REGISTER} className="text-blue-600 font-semibold hover:underline">
            Créer un compte
          </Link>
        </div>
      </div>
    </div>
  )
}

export default LoginForm
