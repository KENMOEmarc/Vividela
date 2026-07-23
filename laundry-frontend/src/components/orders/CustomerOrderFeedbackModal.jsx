import { useState, useEffect } from 'react'
import { X, Star, MessageSquareHeart, Loader2, CheckCircle2 } from 'lucide-react'
import { toast } from 'react-toastify'
import { getOrderFeedback, submitOrderFeedback } from '../../api/feedbackApi'

const SENTIMENT_LABELS = {
  POSITIVE: 'Positif',
  NEUTRAL: 'Neutre',
  NEGATIVE: 'Négatif',
}

const SENTIMENT_BADGE = {
  POSITIVE: 'bg-green-50 text-green-600 ring-green-100',
  NEUTRAL: 'bg-slate-50 text-slate-600 ring-slate-100',
  NEGATIVE: 'bg-red-50 text-red-600 ring-red-100',
}

/**
 * Modale "Donner mon avis" affichée depuis la carte de commande côté client,
 * une fois la commande livrée (voir CustomerOrderCard). Charge d'abord l'état
 * actuel du formulaire (GET /orders/{id}/feedback) : si un avis a déjà été
 * soumis, affiche un simple récapitulatif ; sinon affiche le formulaire
 * (note en étoiles + commentaire libre) et le soumet (POST), ce qui déclenche
 * côté backend l'analyse IA (Gemini) puis la notification du manager (et de
 * l'admin si l'avis est jugé négatif).
 */
const CustomerOrderFeedbackModal = ({ order, onClose, onSubmitted }) => {
  const [loading, setLoading] = useState(true)
  const [feedback, setFeedback] = useState(null)
  const [rating, setRating] = useState(0)
  const [hoverRating, setHoverRating] = useState(0)
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let active = true
    setLoading(true)
    getOrderFeedback(order.id)
      .then((res) => { if (active) setFeedback(res.data?.data ?? null) })
      .catch(() => { if (active) setFeedback(null) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [order.id])

  const handleSubmit = async () => {
    if (rating < 1) {
      toast.error('Merci de sélectionner une note avant de valider')
      return
    }
    setSubmitting(true)
    try {
      const res = await submitOrderFeedback(order.id, { rating, comment: comment.trim() || undefined })
      const saved = res.data?.data
      setFeedback(saved)
      toast.success('Merci pour votre avis !')
      onSubmitted?.(order.id)
    } catch (err) {
      toast.error(err?.response?.data?.message || "Impossible d'envoyer votre avis")
    } finally {
      setSubmitting(false)
    }
  }

  const alreadySubmitted = !!feedback?.submitted

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md max-h-[85vh] flex flex-col">
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-slate-100 flex-shrink-0">
          <div>
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <MessageSquareHeart size={15} className="text-pink-500" />
              Donner mon avis
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">Commande #{order.id}</p>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg">
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 px-6 py-5">
          {loading ? (
            <div className="flex justify-center py-10">
              <div className="w-6 h-6 border-2 border-pink-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : alreadySubmitted ? (
            <div className="space-y-4 text-center py-4">
              <CheckCircle2 size={32} className="text-green-500 mx-auto" />
              <div>
                <p className="text-sm font-semibold text-slate-800">Vous avez déjà donné votre avis</p>
                <p className="text-xs text-slate-500 mt-1">Merci pour votre retour, il a bien été transmis au pressing.</p>
              </div>
              <div className="flex items-center justify-center gap-1">
                {[1, 2, 3, 4, 5].map((n) => (
                  <Star
                    key={n}
                    size={18}
                    className={n <= (feedback.rating ?? 0) ? 'text-amber-400 fill-amber-400' : 'text-slate-200'}
                  />
                ))}
              </div>
              {feedback.comment && (
                <p className="text-xs text-slate-600 bg-slate-50 rounded-xl border border-slate-100 p-3 text-left">
                  {feedback.comment}
                </p>
              )}
              {feedback.sentiment && (
                <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${SENTIMENT_BADGE[feedback.sentiment] || 'bg-slate-50 text-slate-600 ring-slate-100'}`}>
                  Ressenti détecté · {SENTIMENT_LABELS[feedback.sentiment] || feedback.sentiment}
                </span>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              <p className="text-xs text-slate-500">
                Votre commande a été livrée. Votre avis nous aide à améliorer nos services au quotidien.
              </p>

              <div>
                <p className="text-xs font-semibold text-slate-700 mb-2">Votre note</p>
                <div className="flex items-center gap-1.5">
                  {[1, 2, 3, 4, 5].map((n) => (
                    <button
                      key={n}
                      type="button"
                      onClick={() => setRating(n)}
                      onMouseEnter={() => setHoverRating(n)}
                      onMouseLeave={() => setHoverRating(0)}
                      className="p-0.5"
                      aria-label={`${n} étoile${n > 1 ? 's' : ''}`}
                    >
                      <Star
                        size={26}
                        className={
                          n <= (hoverRating || rating)
                            ? 'text-amber-400 fill-amber-400'
                            : 'text-slate-200'
                        }
                      />
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-700 mb-2 block">
                  Commentaire <span className="text-slate-400 font-normal">(facultatif)</span>
                </label>
                <textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  maxLength={2000}
                  rows={4}
                  placeholder="Qu'avez-vous pensé de notre service ?"
                  className="w-full text-sm rounded-xl border border-slate-200 px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-pink-200 focus:border-pink-300 resize-none"
                />
              </div>
            </div>
          )}
        </div>

        <div className="px-6 pb-5 pt-1 flex-shrink-0 flex gap-2.5">
          {alreadySubmitted ? (
            <button
              onClick={onClose}
              className="w-full py-2.5 border border-slate-200 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-50 transition-colors"
            >
              Fermer
            </button>
          ) : (
            <>
              <button
                onClick={onClose}
                className="flex-1 py-2.5 border border-slate-200 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-50 transition-colors"
              >
                Annuler
              </button>
              <button
                onClick={handleSubmit}
                disabled={submitting || loading}
                className="flex-1 inline-flex items-center justify-center gap-2 py-2.5 rounded-xl text-sm font-semibold text-white bg-pink-500 hover:bg-pink-600 transition-colors disabled:opacity-50"
              >
                {submitting && <Loader2 size={15} className="animate-spin" />}
                Envoyer mon avis
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default CustomerOrderFeedbackModal
