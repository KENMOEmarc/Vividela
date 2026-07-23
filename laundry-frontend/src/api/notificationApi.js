import apiClient from './authApi'

/** Récupère les notifications de l'utilisateur connecté (les plus récentes en premier). */
export const getMyNotifications = () => apiClient.get('/notifications')

/** Marque une notification comme lue. */
export const markNotificationAsRead = (notificationId) =>
  apiClient.patch(`/notifications/${notificationId}/read`)
