import { Link } from 'react-router-dom'
import { ROUTES } from '../utils/constants'
import {
  Shirt, Lock, Globe2, ArrowRight, PlayCircle,
  ShoppingCart, Bell, Users, ScrollText,
  AlertTriangle, RefreshCw, ScanBarcode, LogIn, Sparkles,
  Timer, Star, CheckCircle2, Ticket, Plus, Search,
  LayoutGrid, UserCog, Boxes, Receipt, Clock3,
} from 'lucide-react'

const TrustBadge = ({ icon: Icon, label }) => (
  <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-blue-400/30 bg-white/5 text-blue-100 text-xs font-medium backdrop-blur-sm">
    <Icon size={13} />
    {label}
  </div>
)

const StatCard = ({ value, label }) => (
  <div className="text-center">
    <div className="text-2xl sm:text-3xl font-bold text-slate-900">{value}</div>
    <div className="text-slate-500 text-sm mt-1">{label}</div>
  </div>
)

const FEATURES = [
  {
    icon: ShoppingCart,
    color: 'bg-blue-50 text-blue-600',
    title: 'Gestion des commandes',
    desc: "Créez, filtrez et suivez chaque commande par statut (en attente, en traitement, prêt, livré) et par délai.",
  },
  {
    icon: Ticket,
    color: 'bg-purple-50 text-purple-600',
    title: 'Tickets & reçus',
    desc: "Générez un ticket de dépôt et un reçu pour chaque commande, imprimables ou envoyés directement au client.",
  },
  {
    icon: Boxes,
    color: 'bg-emerald-50 text-emerald-600',
    title: 'Gestion du stock',
    desc: "Suivez vos produits lot par lot avec seuils d'alerte, pour ne jamais être pris de court par une rupture.",
  },
  {
    icon: ScrollText,
    color: 'bg-amber-50 text-amber-600',
    title: 'Tarifs des services',
    desc: "Définissez un tarif par catégorie de vêtement et par service — lavage, repassage, nettoyage à sec, teinture.",
  },
  {
    icon: UserCog,
    color: 'bg-rose-50 text-rose-600',
    title: 'Clients & équipe',
    desc: "Gérez vos clients et votre personnel avec des rôles Admin, Manager et Employé aux permissions dédiées.",
  },
  {
    icon: Bell,
    color: 'bg-slate-100 text-slate-600',
    title: 'Notifications',
    desc: "Email, SMS et notifications in-app envoyées automatiquement à chaque étape : dépôt, prêt, paiement.",
  },
]

const STEPS = [
  {
    n: '01',
    icon: LogIn,
    color: 'text-blue-600 bg-blue-50',
    title: 'Créez votre espace',
    desc: 'Ajoutez vos employés, configurez vos rôles et permissions selon votre organisation.',
  },
  {
    n: '02',
    icon: ScanBarcode,
    color: 'text-amber-600 bg-amber-50',
    title: 'Paramétrez',
    desc: 'Définissez vos tarifs de services, vos catégories de vêtements et vos niveaux de stock.',
  },
  {
    n: '03',
    icon: Sparkles,
    color: 'text-emerald-600 bg-emerald-50',
    title: 'Pilotez',
    desc: 'Gérez commandes, tickets et stocks en temps réel depuis un tableau de bord unique.',
  },
]

const ORDER_STATUS_STYLES = {
  'Prêt':          'bg-emerald-100 text-emerald-700',
  'En traitement': 'bg-purple-100 text-purple-700',
  'En attente':    'bg-amber-100 text-amber-700',
  'Annulé':        'bg-red-100 text-red-600',
}

const PAYMENT_STYLES = {
  'Payé':      'bg-emerald-50 text-emerald-700 border border-emerald-200',
  'En attente':'bg-amber-50 text-amber-700 border border-amber-200',
  'Remboursé': 'bg-slate-100 text-slate-500 border border-slate-200',
}

const MOCK_ORDERS = [
  { initials: 'LO', avatar: 'from-pink-500 to-rose-500',  name: 'Luc Owona',   num: '#1', status: 'Prêt',          price: '3 800 FCFA', articles: 2, payment: 'Payé' },
  { initials: 'ME', avatar: 'from-red-400 to-rose-500',   name: 'Marie Essimi', num: '#2', status: 'En traitement', price: '4 000 FCFA', articles: 2, payment: 'En attente' },
  { initials: 'BK', avatar: 'from-purple-500 to-indigo-500', name: 'Bertrand Kenmoe', num: '#10', status: 'Prêt', price: '3 000 FCFA', articles: 1, payment: 'Payé' },
  { initials: 'SN', avatar: 'from-fuchsia-500 to-pink-500', name: 'Sophie Ngo', num: '#12', status: 'Prêt',        price: '5 700 FCFA', articles: 2, payment: 'Payé' },
]

const SIDEBAR_ICONS = [LayoutGrid, UserCog, Users, Shirt, Boxes, ScrollText, Ticket, ShoppingCart]

const MiniSidebar = () => (
  <div className="hidden sm:flex flex-col items-center gap-3 bg-slate-950 py-4 px-2.5 w-12">
    <div className="w-7 h-7 rounded-lg bg-blue-600 flex items-center justify-center mb-2">
      <Shirt size={13} className="text-white" />
    </div>
    {SIDEBAR_ICONS.map((Icon, i) => (
      <div
        key={i}
        className={`w-7 h-7 rounded-lg flex items-center justify-center ${
          i === 7 ? 'bg-blue-600 text-white' : 'text-slate-500'
        }`}
      >
        <Icon size={14} />
      </div>
    ))}
  </div>
)

const LandingPage = () => {
  return (
    <div className="bg-white">

      {/* ── HERO ─────────────────────────────────────────────────────── */}
      <section className="relative overflow-hidden bg-gradient-to-br from-blue-950 via-blue-900 to-blue-800">
        <div className="absolute -top-24 -right-24 w-96 h-96 bg-blue-500/20 rounded-full blur-3xl" />
        <div className="absolute -bottom-32 -left-24 w-96 h-96 bg-indigo-500/20 rounded-full blur-3xl" />

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-28">
          <div className="max-w-3xl mx-auto text-center">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-800/60 border border-blue-600/40 text-blue-200 text-xs font-semibold tracking-wide uppercase mb-6">
              <Sparkles size={12} />
              Commencer
            </span>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-white leading-tight">
              Gérez votre pressing<br className="hidden sm:block" /> avec Vividela.
            </h1>

            <p className="mt-6 text-blue-200 text-base sm:text-lg max-w-2xl mx-auto">
              Commandes, tickets, stocks, tarifs et notifications clients — tout centralisé
              dans une seule plateforme, pensée pour les métiers du pressing.
            </p>

            <div className="mt-9 flex flex-col sm:flex-row items-center justify-center gap-3">
              <Link
                to={ROUTES.REGISTER}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 bg-white text-blue-800 font-semibold rounded-xl hover:bg-blue-50 transition-colors shadow-lg shadow-black/10"
              >
                Créer un compte
                <ArrowRight size={17} />
              </Link>
              <Link
                to={ROUTES.LOGIN}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 bg-white/10 text-white font-semibold rounded-xl border border-white/20 hover:bg-white/20 transition-colors backdrop-blur-sm"
              >
                <PlayCircle size={17} />
                Se connecter
              </Link>
            </div>

            <div className="mt-8 flex flex-wrap items-center justify-center gap-2.5">
              <TrustBadge icon={Lock} label="Connexion chiffrée" />
              <TrustBadge icon={Shirt} label="Accès sécurisé" />
              <TrustBadge icon={Globe2} label="Web · Mobile · Desktop" />
            </div>
          </div>

          {/* Mockup — écran "Gestion des commandes" */}
          <div className="mt-16 max-w-4xl mx-auto">
            <div className="rounded-2xl border border-white/10 shadow-2xl shadow-black/30 overflow-hidden">
              <div className="flex items-center gap-1.5 px-4 py-2.5 bg-slate-950 border-b border-white/10">
                <span className="w-2.5 h-2.5 rounded-full bg-red-400/70" />
                <span className="w-2.5 h-2.5 rounded-full bg-amber-400/70" />
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400/70" />
                <span className="ml-3 text-[11px] text-slate-400 font-mono">app.vividela.io/dashboard/orders</span>
              </div>

              <div className="flex bg-white">
                <MiniSidebar />

                <div className="flex-1 p-4 sm:p-5">
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      <ShoppingCart size={15} className="text-blue-600" />
                      <span className="font-semibold text-slate-800 text-sm">Gestion des commandes</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="hidden sm:flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-50 border border-slate-200 text-slate-400 text-xs">
                        <Search size={12} /> Rechercher…
                      </div>
                      <div className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-blue-600 text-white text-xs font-medium">
                        <Plus size={12} /> Ajouter
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-1.5 mb-4">
                    {['En attente', 'En traitement', 'Prêt', 'Livré'].map((s) => (
                      <span key={s} className="px-2.5 py-1 rounded-md border border-slate-200 text-slate-500 text-[11px] font-medium">
                        {s}
                      </span>
                    ))}
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    {MOCK_ORDERS.map((o) => (
                      <div key={o.num} className="rounded-xl border border-slate-100 p-3">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <span className={`w-7 h-7 rounded-full bg-gradient-to-br ${o.avatar} flex items-center justify-center text-white text-[10px] font-semibold`}>
                              {o.initials}
                            </span>
                            <div>
                              <div className="text-xs font-semibold text-slate-800 leading-tight">{o.name}</div>
                              <div className="text-[10px] text-slate-400">{o.num}</div>
                            </div>
                          </div>
                          <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${ORDER_STATUS_STYLES[o.status]}`}>
                            {o.status}
                          </span>
                        </div>
                        <div className="flex items-center justify-between text-[11px] text-slate-500 mb-2">
                          <span className="font-semibold text-slate-700">{o.price}</span>
                          <span>{o.articles} article{o.articles > 1 ? 's' : ''}</span>
                        </div>
                        <span className={`inline-block px-2 py-0.5 rounded-md text-[10px] font-medium ${PAYMENT_STYLES[o.payment]}`}>
                          {o.payment}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── STATS ────────────────────────────────────────────────────── */}
      <section className="border-b border-slate-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 grid grid-cols-2 sm:grid-cols-4 gap-8">
          <StatCard value="1 200+" label="Tickets traités" />
          <StatCard value="350+" label="Clients suivis" />
          <StatCard value="< 1s" label="Temps de réponse" />
          <StatCard value="99.9%" label="Disponibilité" />
        </div>
      </section>

      {/* ── FEATURES GRID ───────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="text-center max-w-2xl mx-auto mb-14">
          <span className="inline-block px-3 py-1 rounded-full bg-blue-50 text-blue-600 text-xs font-semibold uppercase tracking-wide mb-4">
            Modules intégrés
          </span>
          <h2 className="text-3xl sm:text-4xl font-bold text-slate-900">Tout ce dont vous avez besoin</h2>
          <p className="mt-3 text-slate-500">
            De la prise en charge des commandes au suivi financier, en passant par les stocks et
            les notifications clients — tout est centralisé dans Vividela.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {FEATURES.map(({ icon: Icon, color, title, desc }) => (
            <div
              key={title}
              className="rounded-2xl border border-slate-100 p-6 hover:shadow-lg hover:border-slate-200 transition-all"
            >
              <div className={`w-11 h-11 rounded-xl flex items-center justify-center mb-4 ${color}`}>
                <Icon size={20} />
              </div>
              <h3 className="font-semibold text-slate-900 text-lg mb-1.5">{title}</h3>
              <p className="text-slate-500 text-sm leading-relaxed">{desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── STOCK SHOWCASE ──────────────────────────────────────────── */}
      <section className="bg-slate-50 py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid lg:grid-cols-2 gap-14 items-center">
          <div>
            <span className="inline-block px-3 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-semibold uppercase tracking-wide mb-4">
              Gestion du stock
            </span>
            <h2 className="text-3xl font-bold text-slate-900 leading-tight">
              Ne soyez plus jamais pris de court
            </h2>
            <p className="mt-4 text-slate-500">
              Suivez vos produits lot par lot avec précision. Recevez des alertes automatiques
              dès qu'un seuil critique est atteint et gardez une vue claire sur vos stocks.
            </p>
            <div className="mt-6 space-y-3">
              {[
                { icon: AlertTriangle, text: 'Alertes automatiques par seuil défini' },
                { icon: RefreshCw, text: 'Suivi des lots et des expirations' },
                { icon: ScanBarcode, text: 'Mise à jour rapide, par produit ou en lot' },
              ].map(({ icon: Icon, text }) => (
                <div key={text} className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-white border border-slate-200 flex items-center justify-center flex-shrink-0">
                    <Icon size={15} className="text-blue-600" />
                  </div>
                  <span className="text-slate-700 text-sm">{text}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-2xl bg-white border border-slate-200 shadow-lg overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100">
              <span className="flex items-center gap-2 font-semibold text-slate-800 text-sm">
                <Boxes size={16} className="text-blue-600" />
                Gestion du stock
              </span>
            </div>

            <div className="grid grid-cols-3 gap-2.5 p-4">
              <div className="rounded-xl bg-slate-50 border border-slate-100 p-3">
                <div className="text-[11px] text-slate-400 mb-1">Produits</div>
                <div className="text-lg font-bold text-slate-800">6</div>
              </div>
              <div className="rounded-xl bg-emerald-50 border border-emerald-100 p-3">
                <div className="text-[11px] text-emerald-600 mb-1">Stocks OK</div>
                <div className="text-lg font-bold text-emerald-700">5</div>
              </div>
              <div className="rounded-xl bg-red-50 border border-red-100 p-3">
                <div className="text-[11px] text-red-500 mb-1">Alertes stock</div>
                <div className="text-lg font-bold text-red-600">1</div>
              </div>
            </div>

            <div className="px-5 pb-5">
              <div className="flex items-center gap-2 text-[11px] font-medium mb-2">
                <span className="px-2.5 py-1 rounded-md bg-slate-100 text-slate-600">Tous (6)</span>
                <span className="px-2.5 py-1 rounded-md bg-red-500 text-white">Alertes (1)</span>
              </div>
              <div className="rounded-lg border border-red-100 bg-red-50/60 px-3 py-2.5 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertTriangle size={13} className="text-red-500" />
                  <div>
                    <div className="text-xs font-semibold text-slate-800">Sachet plastique</div>
                    <div className="text-[10px] text-slate-400">Seuil alerte : 200.00 pièce(s)</div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm font-bold text-red-600">176.00</div>
                  <span className="text-[10px] font-semibold text-red-600">Alerte</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── TARIFS SHOWCASE ─────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="grid lg:grid-cols-2 gap-14 items-center">

          <div className="order-2 lg:order-1 rounded-2xl bg-white border border-slate-200 shadow-lg overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100">
              <span className="flex items-center gap-2 font-semibold text-slate-800 text-sm">
                <ScrollText size={16} className="text-blue-600" />
                Tarifs par catégorie
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2.5 p-4">
              {[
                { icon: Shirt, color: 'bg-pink-50 text-pink-600', name: 'Chemise', range: '300 – 1 800 FCFA' },
                { icon: Shirt, color: 'bg-blue-50 text-blue-600', name: 'Pantalon', range: '500 – 2 500 FCFA' },
                { icon: Shirt, color: 'bg-purple-50 text-purple-600', name: 'Robe', range: '700 – 3 000 FCFA' },
                { icon: Shirt, color: 'bg-teal-50 text-teal-600', name: 'Costume', range: '1 200 – 6 000 FCFA' },
              ].map((c) => (
                <div key={c.name} className="rounded-xl border border-slate-100 p-3">
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center mb-2 ${c.color}`}>
                    <c.icon size={14} />
                  </div>
                  <div className="text-xs font-semibold text-slate-800">{c.name}</div>
                  <div className="text-[10px] text-slate-400 mb-1">6 services</div>
                  <div className="text-[11px] font-medium text-slate-600">{c.range}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="order-1 lg:order-2">
            <span className="inline-block px-3 py-1 rounded-full bg-amber-50 text-amber-600 text-xs font-semibold uppercase tracking-wide mb-4">
              Tarifs des services
            </span>
            <h2 className="text-3xl font-bold text-slate-900 leading-tight">
              Un tarif pour chaque vêtement, chaque service
            </h2>
            <p className="mt-4 text-slate-500">
              Configurez vos prix par catégorie de vêtement — chemise, pantalon, robe, costume —
              et par type de service : lavage, repassage, nettoyage à sec, teinture, retouche.
            </p>
            <div className="mt-6 space-y-3">
              {[
                { icon: Receipt, text: 'Prix affiché automatiquement sur chaque ticket' },
                { icon: Clock3, text: 'Historique des tarifs et mise à jour instantanée' },
                { icon: CheckCircle2, text: 'Statut Actif / Inactif par service' },
              ].map(({ icon: Icon, text }) => (
                <div key={text} className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-slate-50 border border-slate-200 flex items-center justify-center flex-shrink-0">
                    <Icon size={15} className="text-blue-600" />
                  </div>
                  <span className="text-slate-700 text-sm">{text}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ── STEPS ────────────────────────────────────────────────────── */}
      <section className="bg-slate-50 py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-xl mx-auto mb-14">
            <span className="inline-block px-3 py-1 rounded-full bg-blue-50 text-blue-600 text-xs font-semibold uppercase tracking-wide mb-4">
              Démarrage rapide
            </span>
            <h2 className="text-3xl sm:text-4xl font-bold text-slate-900">Opérationnel en 3 étapes</h2>
          </div>

          <div className="grid sm:grid-cols-3 gap-6">
            {STEPS.map(({ n, icon: Icon, color, title, desc }) => (
              <div key={n} className="rounded-2xl bg-white border border-slate-100 p-6 relative overflow-hidden">
                <span className="absolute top-4 right-5 text-4xl font-bold text-slate-100 select-none">{n}</span>
                <div className={`w-11 h-11 rounded-xl flex items-center justify-center mb-5 ${color}`}>
                  <Icon size={20} />
                </div>
                <h3 className="font-semibold text-slate-900 text-lg mb-2">{title}</h3>
                <p className="text-slate-500 text-sm leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA BANNER ───────────────────────────────────────────────── */}
      <section className="bg-gradient-to-br from-blue-900 to-blue-700 relative overflow-hidden">
        <div className="absolute -top-16 -left-16 w-72 h-72 bg-blue-500/20 rounded-full blur-3xl" />
        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
          <div className="inline-flex items-center justify-center w-14 h-14 bg-white/10 rounded-2xl mb-5 border border-white/20">
            <Timer className="text-white" size={26} />
          </div>
          <h2 className="text-3xl font-bold text-white">Prêt à moderniser votre pressing ?</h2>
          <p className="mt-3 text-blue-200">
            Rejoignez Vividela et pilotez commandes, stocks et finances depuis un seul endroit.
          </p>
          <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
            <Link
              to={ROUTES.REGISTER}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 bg-white text-blue-800 font-semibold rounded-xl hover:bg-blue-50 transition-colors"
            >
              Créer un compte gratuitement
              <ArrowRight size={17} />
            </Link>
            <Link
              to={ROUTES.LOGIN}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 text-white font-semibold rounded-xl border border-white/30 hover:bg-white/10 transition-colors"
            >
              J'ai déjà un compte
            </Link>
          </div>
          <div className="mt-7 flex items-center justify-center gap-1.5 text-blue-200 text-sm">
            <Star size={14} className="fill-amber-300 text-amber-300" />
            <Star size={14} className="fill-amber-300 text-amber-300" />
            <Star size={14} className="fill-amber-300 text-amber-300" />
            <Star size={14} className="fill-amber-300 text-amber-300" />
            <Star size={14} className="fill-amber-300 text-amber-300" />
            <span className="ml-2 flex items-center gap-1">
              <CheckCircle2 size={14} />
              Déjà adopté par des équipes pressing
            </span>
          </div>
        </div>
      </section>
    </div>
  )
}

export default LandingPage
