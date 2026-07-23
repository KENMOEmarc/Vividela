import { AtSign, Mail, Phone, Lock, ShieldCheck } from 'lucide-react'
import { ROLES, ROLE_LABELS } from '../utils/constants'

// ─── Avatar ──────────────────────────────────────────────────────────────────
const AVATAR_PALETTE = [
  'from-blue-400 to-blue-600',
  'from-violet-400 to-violet-600',
  'from-emerald-400 to-emerald-600',
  'from-orange-400 to-orange-600',
  'from-rose-400 to-rose-600',
  'from-cyan-400 to-cyan-600',
  'from-amber-400 to-amber-600',
  'from-indigo-400 to-indigo-600',
]

export const getUserInitials = (user) =>
  [user?.firstName?.[0], user?.lastName?.[0]].filter(Boolean).join('').toUpperCase() || '?'

export const getUserAvatarGradient = (firstName = '') =>
  AVATAR_PALETTE[(firstName?.charCodeAt(0) ?? 0) % AVATAR_PALETTE.length]

// ─── Badge rôle ──────────────────────────────────────────────────────────────
export const USER_ROLE_BADGE = {
  [ROLES.ADMIN]:    'bg-purple-50 text-purple-600 ring-purple-100',
  [ROLES.MANAGER]:  'bg-amber-50 text-amber-600 ring-amber-100',
  [ROLES.EMPLOYEE]: 'bg-blue-50 text-blue-600 ring-blue-100',
  [ROLES.CUSTOMER]: 'bg-emerald-50 text-emerald-600 ring-emerald-100',
}

// ─── Colonnes du tableau ──────────────────────────────────────────────────────
export const USER_TABLE_COLUMNS = [
  {
    key: 'id',
    label: 'ID',
    sortable: true,
    className: 'w-16',
    render: (user) => (
      <span className="font-mono text-[11px] text-slate-400">#{user.id}</span>
    ),
  },
  {
    key: 'firstName',
    label: 'Utilisateur',
    sortable: true,
    render: (user) => (
      <div className="flex items-center gap-2.5">
        <div
          className={`w-9 h-9 rounded-full bg-gradient-to-br ${getUserAvatarGradient(user.firstName)} text-white font-bold text-[11px] flex items-center justify-center flex-shrink-0 shadow-sm`}
        >
          {getUserInitials(user)}
        </div>
        <div className="min-w-0">
          <p className="font-semibold text-slate-800 leading-tight truncate">
            {user.firstName} {user.lastName}
          </p>
          <p className="text-[10px] text-slate-400 lg:hidden truncate">{user.email}</p>
        </div>
      </div>
    ),
  },
  {
    key: 'userName',
    label: 'Username',
    sortable: true,
    className: 'hidden md:table-cell',
    render: (user) => (
      <span className="font-mono text-[11px] text-slate-500 bg-slate-50 px-2 py-0.5 rounded-md">
        {user.userName}
      </span>
    ),
  },
  {
    key: 'email',
    label: 'Email',
    sortable: true,
    className: 'hidden lg:table-cell',
    render: (user) => (
      <span className="text-[12px] text-slate-500">{user.email}</span>
    ),
  },
  {
    key: 'phone',
    label: 'Téléphone',
    sortable: true,
    className: 'hidden lg:table-cell',
    render: (user) => (
      <span className="text-[12px] text-slate-500">{user.phone}</span>
    ),
  },
  {
    key: 'role',
    label: 'Rôle',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (user) => {
      const cls = USER_ROLE_BADGE[user.role] ?? 'bg-slate-100 text-slate-500 ring-slate-200'
      return (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${cls}`}>
          {ROLE_LABELS[user.role] ?? user.role ?? '—'}
        </span>
      )
    },
  },
  {
    key: 'enabled',
    label: 'Statut',
    sortable: true,
    render: (user) =>
      user.enabled ? (
        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-600 ring-1 ring-emerald-100">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
          Actif
        </span>
      ) : (
        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-500">
          <span className="w-1.5 h-1.5 rounded-full bg-slate-400" />
          Inactif
        </span>
      ),
  },
  {
    key: 'createdAt',
    label: 'Créé le',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (user) => {
      if (!user.createdAt) return <span className="text-slate-400 text-[11px]">—</span>
      return (
        <span className="text-[11px] text-slate-400">
          {new Date(user.createdAt).toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
          })}
        </span>
      )
    },
  },
]

// ─── Champs du formulaire ─────────────────────────────────────────────────────
// RÈGLE MÉTIER : seul un ADMIN peut changer le mot de passe d'un compte
// existant (voir UserServiceImpl#update côté backend, qui rejette toute
// tentative venant d'un MANAGER/EMPLOYEE). En édition, le champ n'est donc
// affiché que si l'utilisateur connecté est ADMIN. En création, il reste
// affiché pour ADMIN et MANAGER (seuls rôles autorisés à créer un compte),
// puisqu'un mot de passe initial est requis pour tout nouveau compte.
export const getUserFormFields = (currentUserRole, mode = 'create') => [
  {
    key: 'firstName',
    label: 'Prénom',
    type: 'text',
    required: true,
    placeholder: 'Jean',
    group: 'name',
  },
  {
    key: 'lastName',
    label: 'Nom',
    type: 'text',
    required: true,
    placeholder: 'Dupont',
    group: 'name',
  },
  {
    key: 'userName',
    label: "Nom d'utilisateur",
    type: 'text',
    required: true,
    placeholder: 'jean_dupont',
    prefixIcon: AtSign,
  },
  {
    key: 'email',
    label: 'Adresse email',
    type: 'email',
    required: true,
    placeholder: 'jean@exemple.com',
    prefixIcon: Mail,
  },
  {
    key: 'phone',
    label: 'Numéro de téléphone',
    type: 'text',
    required: true,
    placeholder: '6 12 34 56 78',
    prefixIcon: Phone,
  },
  ...(mode === 'create' || currentUserRole === ROLES.ADMIN
    ? [{
        key: 'password',
        label: 'Mot de passe',
        type: 'password',
        required: mode === 'create',
        placeholder: '••••••••',
        hint: mode === 'edit' ? "Laisser vide pour conserver l'actuel" : undefined,
        prefixIcon: Lock,
      }]
    : []),
  {
    key: 'role',
    label: 'Rôle',
    type: 'select',
    required: true,
    prefixIcon: ShieldCheck,
    defaultValue: ROLES.CUSTOMER,
    // Un utilisateur ne peut attribuer qu'un rôle inférieur ou égal au sien :
    // ADMIN → ADMIN/MANAGER/EMPLOYEE/CUSTOMER
    // MANAGER → MANAGER/EMPLOYEE/CUSTOMER (jamais ADMIN)
    // EMPLOYEE → EMPLOYEE/CUSTOMER (jamais ADMIN ni MANAGER)
    options: [
      ...(currentUserRole === ROLES.ADMIN
        ? [{ value: ROLES.ADMIN, label: ROLE_LABELS.ADMIN }]
        : []),
      ...(currentUserRole === ROLES.ADMIN || currentUserRole === ROLES.MANAGER
        ? [{ value: ROLES.MANAGER, label: ROLE_LABELS.MANAGER }]
        : []),
      { value: ROLES.EMPLOYEE, label: ROLE_LABELS.EMPLOYEE },
      { value: ROLES.CUSTOMER, label: ROLE_LABELS.CUSTOMER },
    ],
  },
  {
    key: 'enabled',
    label: 'Compte actif',
    type: 'toggle',
    required: false,
    hint: "L'utilisateur peut se connecter",
    defaultValue: true,
  },
]

// ─── Aperçu suppression ───────────────────────────────────────────────────────
export const renderUserDeletePreview = (user) => (
  <div className="flex items-center gap-3 p-3.5 bg-slate-50 border border-slate-100 rounded-xl">
    <div
      className={`w-10 h-10 rounded-full bg-gradient-to-br ${getUserAvatarGradient(user?.firstName)} text-white font-bold text-sm flex items-center justify-center flex-shrink-0 shadow-sm`}
    >
      {getUserInitials(user)}
    </div>
    <div className="min-w-0">
      <p className="font-semibold text-slate-900 text-sm truncate">
        {user?.firstName} {user?.lastName}
      </p>
      <p className="text-[11px] text-slate-500 truncate">
        @{user?.userName || user?.username} · {user?.email}
      </p>
    </div>
  </div>
)
