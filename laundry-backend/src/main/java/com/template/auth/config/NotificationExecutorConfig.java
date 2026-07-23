package com.template.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool de threads dédié à l'envoi asynchrone des notifications (IN_APP,
 * email) déclenchées lorsqu'une commande change de statut, ainsi qu'à
 * l'analyse IA (Gemini) et aux notifications déclenchées par les avis
 * clients (voir GeminiReviewAnalysisService et FeedbackAnalysisCoordinator).
 *
 * Chaque notification est envoyée via {@code CompletableFuture.runAsync(...,
 * notificationExecutor)}. Le fait d'utiliser un pool dédié (plutôt que le
 * ForkJoinPool.commonPool() par défaut de CompletableFuture) évite qu'un pic
 * de notifications ne vienne saturer un pool partagé avec d'autres tâches
 * asynchrones de l'application, et permet de nommer/monitorer ces threads
 * indépendamment.
 *
 * La requête HTTP qui déclenche le changement de statut (ex :
 * OrderServiceImpl#updateOrder / #recalculateStatus) n'attend jamais la fin
 * de l'envoi effectif de la notification pour renvoyer sa réponse : l'action
 * métier (sauvegarde de la commande) est déjà terminée et committée avant
 * que le thread de notification ne soit déclenché.
 *
 * Dimensionnement : {@code CORE_POOL_SIZE = MAX_POOL_SIZE = 24} (≥ 20 threads
 * demandé). Le pool combine désormais deux familles de tâches, toutes deux
 * dominées par de l'attente réseau (I/O bound) plutôt que par du calcul CPU,
 * ce qui justifie un pool plus large que le nombre de cœurs disponibles :
 *   - notifications IN_APP (écriture DB) / email / SMS,
 *   - appels au modèle Gemini (analyse de sentiment d'un avis client).
 * Une file d'attente bornée ({@link LinkedBlockingQueue}) protège contre un
 * emballement mémoire en cas de pic ; au-delà, {@code CallerRunsPolicy}
 * applique une pression de retour (exécution sur le thread appelant) plutôt
 * que de perdre silencieusement des notifications.
 */
@Configuration
public class NotificationExecutorConfig {

    private static final int CORE_POOL_SIZE = 24;
    private static final int MAX_POOL_SIZE = 24;
    private static final int QUEUE_CAPACITY = 500;
    private static final long KEEP_ALIVE_SECONDS = 60L;

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "notif-async-" + counter.getAndIncrement());
                // Threads démons : ne doivent jamais empêcher l'arrêt de l'application.
                thread.setDaemon(true);
                return thread;
            }
        };

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
        // Permet aux 24 threads du cœur d'expirer après une période
        // d'inactivité (KEEP_ALIVE_SECONDS) plutôt que de rester en
        // permanence, tout en gardant la capacité disponible immédiatement
        // lors des pics.
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
