import { useState, useMemo } from 'react'
import {
  Search,
  ChevronUp, ChevronDown, ChevronsUpDown,
  ChevronLeft, ChevronRight,
} from 'lucide-react'
import ActionButton from './ActionButton'

const PAGE_SIZE = 10

const DataTable = ({
  columns = [],
  data = [],
  loading = false,
  searchFields = [],
  searchPlaceholder = 'Rechercher…',
  keyField = 'id',
  onEdit = null,
  onDelete = null,
  extraActions = null,
  emptyMessage = 'Aucune donnée trouvée.',
  // Autorisation d'édition au niveau de la ligne (ex: un Manager/Employé ne
  // doit pas pouvoir modifier une ligne "ADMIN"). Si non fournie, l'édition
  // suit uniquement onEdit (comportement inchangé pour les autres tables).
  canEditRow = null,
}) => {
  const [search,    setSearch]    = useState('')
  const [sortField, setSortField] = useState(null)
  const [sortDir,   setSortDir]   = useState('asc')
  const [page,      setPage]      = useState(1)

  const handleSort = (field) => {
    if (sortField === field) setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    else { setSortField(field); setSortDir('asc') }
    setPage(1)
  }

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim()
    if (!q || !searchFields.length) return data
    return data.filter(item =>
      searchFields.some(f => String(item[f] ?? '').toLowerCase().includes(q))
    )
  }, [data, search, searchFields])

  const sorted = useMemo(() => {
    if (!sortField) return filtered
    return [...filtered].sort((a, b) => {
      const va = a[sortField] ?? ''
      const vb = b[sortField] ?? ''
      const cmp = typeof va === 'string'
        ? va.localeCompare(vb, 'fr', { sensitivity: 'base' })
        : va < vb ? -1 : va > vb ? 1 : 0
      return sortDir === 'asc' ? cmp : -cmp
    })
  }, [filtered, sortField, sortDir])

  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE))
  const paginated  = sorted.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const hasActions = onEdit || onDelete || extraActions

  const SortIcon = ({ field }) => {
    if (sortField !== field) return <ChevronsUpDown size={12} className="text-slate-300 flex-shrink-0" />
    return sortDir === 'asc'
      ? <ChevronUp size={12} className="text-blue-500 flex-shrink-0" />
      : <ChevronDown size={12} className="text-blue-500 flex-shrink-0" />
  }

  return (
    <div className="flex flex-col">

      {/* ── Barre de recherche ─────────────────────────────────────── */}
      <div className="px-5 py-3 border-b border-slate-50">
        <div className="relative w-full sm:max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          <input
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1) }}
            placeholder={searchPlaceholder}
            className="w-full pl-8 pr-3 py-2 text-sm border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400 bg-slate-50 focus:bg-white transition-colors placeholder:text-slate-300"
          />
        </div>
        <p className="text-[11px] text-slate-400 mt-1.5">
          {loading ? '…' : (
            <>
              <span className="font-semibold text-slate-600">{filtered.length}</span>
              {' '}{filtered.length !== 1 ? 'résultats' : 'résultat'}
              {search && <span> pour « {search} »</span>}
            </>
          )}
        </p>
      </div>

      {/* ── Table ──────────────────────────────────────────────────── */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm min-w-[640px]">
          <thead className="bg-slate-50/80 border-b border-slate-100">
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  onClick={() => col.sortable && handleSort(col.key)}
                  className={[
                    'px-4 py-3 text-left text-[10px] font-bold text-slate-400 uppercase tracking-wider select-none',
                    col.sortable ? 'cursor-pointer hover:text-slate-600 hover:bg-slate-100/80 transition-colors' : '',
                    col.className ?? '',
                  ].join(' ')}
                >
                  <div className="flex items-center gap-1">
                    {col.label}
                    {col.sortable && <SortIcon field={col.key} />}
                  </div>
                </th>
              ))}
              {hasActions && (
                <th className="px-4 py-3 text-right text-[10px] font-bold text-slate-400 uppercase tracking-wider w-20">
                  Actions
                </th>
              )}
            </tr>
          </thead>

          <tbody className="divide-y divide-slate-50">
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">
                  {columns.map((col) => (
                    <td key={col.key} className={`px-4 py-3.5 ${col.className ?? ''}`}>
                      <div className="h-4 bg-slate-100 rounded w-3/4" />
                    </td>
                  ))}
                  {hasActions && <td className="px-4 py-3.5 w-20" />}
                </tr>
              ))
            ) : paginated.length === 0 ? (
              <tr>
                <td colSpan={columns.length + (hasActions ? 1 : 0)} className="px-4 py-14 text-center">
                  <Search size={28} className="text-slate-200 mx-auto mb-2.5" />
                  <p className="text-sm font-medium text-slate-400">
                    {search ? `Aucun résultat pour « ${search} »` : emptyMessage}
                  </p>
                  {search && (
                    <button
                      onClick={() => setSearch('')}
                      className="mt-2 text-xs text-blue-500 hover:underline"
                    >
                      Effacer la recherche
                    </button>
                  )}
                </td>
              </tr>
            ) : (
              paginated.map((item, i) => (
                <tr
                  key={item[keyField] ?? i}
                  className="hover:bg-slate-50/60 transition-colors group"
                >
                  {columns.map((col) => (
                    <td key={col.key} className={`px-4 py-3.5 ${col.className ?? ''}`}>
                      {col.render
                        ? col.render(item, i)
                        : <span className="text-sm text-slate-700">{String(item[col.key] ?? '')}</span>
                      }
                    </td>
                  ))}
                  {hasActions && (
                    <td className="px-4 py-3.5 w-20">
                      <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        {typeof extraActions === 'function' && extraActions(item)}
                        {onEdit && (!canEditRow || canEditRow(item)) && (
                          <ActionButton variant="edit" label="Modifier" onClick={() => onEdit(item)} />
                        )}
                        {onDelete && (
                          <ActionButton variant="delete" label="Supprimer" onClick={() => onDelete(item)} />
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ── Pagination ─────────────────────────────────────────────── */}
      {!loading && sorted.length > PAGE_SIZE && (
        <div className="flex items-center justify-between px-5 py-3 border-t border-slate-50 bg-slate-50/40">
          <span className="text-[11px] text-slate-400">
            Page <span className="font-semibold text-slate-600">{page}</span> sur {totalPages}
            <span className="mx-2 text-slate-300">·</span>
            {sorted.length} résultats
          </span>
          <div className="flex items-center gap-1">
            <PageBtn
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={page === 1}
              icon={ChevronLeft}
            />
            {buildPageList(page, totalPages).map((p, idx) =>
              p === '…' ? (
                <span key={`e-${idx}`} className="w-7 text-center text-slate-400 text-xs">…</span>
              ) : (
                <button
                  key={p}
                  onClick={() => setPage(p)}
                  className={`w-7 h-7 rounded-lg text-xs font-semibold transition-colors ${
                    page === p
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'text-slate-500 hover:bg-white border border-slate-200 hover:border-slate-300'
                  }`}
                >
                  {p}
                </button>
              )
            )}
            <PageBtn
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
              icon={ChevronRight}
            />
          </div>
        </div>
      )}
    </div>
  )
}

const PageBtn = ({ onClick, disabled, icon: Icon }) => (
  <button
    onClick={onClick}
    disabled={disabled}
    className="p-1.5 rounded-lg border border-slate-200 text-slate-500 hover:bg-white hover:border-slate-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
  >
    <Icon size={14} />
  </button>
)

const buildPageList = (current, total) =>
  Array.from({ length: total }, (_, i) => i + 1)
    .filter(p => p === 1 || p === total || Math.abs(p - current) <= 1)
    .reduce((acc, p, idx, arr) => {
      if (idx > 0 && arr[idx - 1] !== p - 1) acc.push('…')
      acc.push(p)
      return acc
    }, [])

export default DataTable
