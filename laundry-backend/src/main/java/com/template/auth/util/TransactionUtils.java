package com.template.auth.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Petit utilitaire partagé pour différer une action (typiquement le
 * déclenchement d'un traitement asynchrone comme l'envoi de notifications)
 * jusqu'après le commit de la transaction Spring actuellement active.
 * <p>
 * Sans cela, une action lancée sur un autre thread (ex : via
 * {@code CompletableFuture.runAsync(...)}) peut s'exécuter — et donc envoyer
 * un email/SMS réel, ou lire une ligne pas encore visible en base — AVANT
 * que la transaction d'origine ne soit committée, voire alors que celle-ci
 * finit par échouer (rollback), ce qui enverrait une notification pour une
 * action qui n'a en réalité jamais eu lieu.
 * <p>
 * Utilisé par tous les déclencheurs de notification (voir
 * {@code NotificationService}) et par {@code FeedbackAnalysisCoordinator}.
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * Exécute {@code action} une fois la transaction en cours committée, ou
     * immédiatement si aucune transaction Spring n'est active (ex : appel
     * direct hors contexte Spring, tests unitaires sans contexte
     * transactionnel).
     * <p>
     * Si la transaction en cours est finalement annulée (rollback),
     * {@code action} n'est jamais exécutée.
     */
    public static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
