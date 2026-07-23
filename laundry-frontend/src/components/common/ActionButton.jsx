import { Pencil, Trash2, Eye, PackagePlus } from 'lucide-react'

const VARIANTS = {
  edit:    'text-slate-400 hover:text-blue-600 hover:bg-blue-50',
  delete:  'text-slate-400 hover:text-red-500 hover:bg-red-50',
  view:    'text-slate-400 hover:text-slate-600 hover:bg-slate-100',
  add:     'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50',
  default: 'text-slate-400 hover:text-slate-600 hover:bg-slate-100',
}

const DEFAULT_ICONS = {
  edit:   Pencil,
  delete: Trash2,
  view:   Eye,
  add:    PackagePlus,
}

const ActionButton = ({
  icon: Icon,
  label,
  onClick,
  variant = 'default',
  iconSize = 14,
  disabled = false,
  className = '',
  ...props
}) => {
  const ResolvedIcon = Icon ?? DEFAULT_ICONS[variant]

  return (
    <button
      type="button"
      title={label}
      onClick={onClick}
      disabled={disabled}
      className={[
        'p-1.5 rounded-lg transition-colors',
        VARIANTS[variant] ?? VARIANTS.default,
        disabled ? 'opacity-50 cursor-not-allowed' : '',
        className,
      ].join(' ')}
      {...props}
    >
      {ResolvedIcon && <ResolvedIcon size={iconSize} />}
    </button>
  )
}

export default ActionButton
