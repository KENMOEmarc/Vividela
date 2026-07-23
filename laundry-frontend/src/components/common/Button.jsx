const VARIANTS = {
  primary:   'bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white shadow-sm shadow-blue-200',
  secondary: 'border border-slate-200 text-slate-600 hover:bg-slate-50',
  danger:    'bg-red-600 hover:bg-red-700 active:bg-red-800 text-white shadow-sm shadow-red-200',
  ghost:     'text-slate-500 hover:text-slate-700 hover:bg-slate-50 border border-transparent',
  success:   'bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm shadow-emerald-200',
}

const SIZES = {
  sm: 'px-3 py-1.5 text-xs gap-1.5 rounded-lg',
  md: 'px-3.5 py-2 text-sm gap-1.5 rounded-xl',
  lg: 'px-5 py-2.5 text-sm gap-2 rounded-xl',
}

const Button = ({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  icon: Icon = null,
  iconPosition = 'left',
  className = '',
  disabled = false,
  type = 'button',
  onClick,
  ...props
}) => {
  const isDisabled = disabled || loading

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={isDisabled}
      className={[
        'inline-flex items-center justify-center font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-blue-100',
        VARIANTS[variant] ?? VARIANTS.primary,
        SIZES[size] ?? SIZES.md,
        isDisabled ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer',
        className,
      ].join(' ')}
      {...props}
    >
      {loading ? (
        <>
          <span className="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin flex-shrink-0" />
          {children}
        </>
      ) : (
        <>
          {Icon && iconPosition === 'left' && <Icon size={14} className="flex-shrink-0" />}
          {children}
          {Icon && iconPosition === 'right' && <Icon size={14} className="flex-shrink-0" />}
        </>
      )}
    </button>
  )
}

export default Button
