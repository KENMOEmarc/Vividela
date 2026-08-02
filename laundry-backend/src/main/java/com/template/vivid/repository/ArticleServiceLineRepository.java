package com.template.vivid.repository;

import com.template.vivid.model.entity.ArticleServiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ArticleServiceLineRepository extends JpaRepository<ArticleServiceLine, Long> {

    List<ArticleServiceLine> findByArticleId(Long articleId);

    /**
     * Charge en une seule requête les services de plusieurs articles à la
     * fois. Utilisée par OrderServiceImpl.toDto pour éviter une requête
     * findByArticleId par article (N+1) lors du mappage d'une liste de
     * commandes avec includeArticles=true.
     */
    List<ArticleServiceLine> findByArticleIdIn(List<Long> articleIds);

    /**
     * Supprime tous les services actuellement appliqués à un article, avant
     * réinsertion de la nouvelle liste (voir ArticleServiceImpl#updateArticle).
     * <p>
     * CORRECTION : l'ancienne version était une méthode dérivée
     * ({@code deleteByArticleId}), que Spring Data traduit en un
     * "SELECT puis entityManager.remove() par entité" — ces suppressions ne
     * sont alors que MISES EN ATTENTE dans le contexte de persistance et ne
     * s'exécutent en base qu'au flush final de la transaction. Or, à ce
     * flush, Hibernate exécute par défaut les INSERT avant les DELETE : si un
     * même ServiceType était conservé d'une modification à l'autre (ex :
     * WASH gardé, IRON remplacé par DRY_CLEAN), le nouvel INSERT sur
     * (article_id, service='WASH') percutait alors la contrainte
     * UNIQUE(article_id, service) puisque l'ancienne ligne WASH n'avait pas
     * encore été supprimée en base — rendant toute modification des services
     * impossible dès qu'au moins un service était conservé.
     * <p>
     * En passant à une requête de suppression en masse (@Modifying,
     * flushAutomatically = true), le DELETE s'exécute immédiatement en base
     * dès l'appel de cette méthode, garantissant que les anciennes lignes ont
     * bien disparu avant que attachServices() n'insère les nouvelles.
     * (Pas de clearAutomatically ici : ArticleServiceImpl#updateArticle
     * continue d'utiliser l'entité Article — et son association LAZY vers
     * Order — juste après cet appel ; vider tout le contexte de persistance
     * la détacherait et risquerait une LazyInitializationException plus loin
     * dans la méthode.)
     * Voir revue de code — "Impossible de modifier les services d'un article".
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM ArticleServiceLine asv WHERE asv.article.id = :articleId")
    void deleteByArticleId(@Param("articleId") Long articleId);

    /**
     * Somme des prix appliqués de tous les services de tous les articles
     * rattachés à une commande. Utilisée pour recalculer Order.totalAmount.
     */
    @Query("SELECT COALESCE(SUM(asv.appliedPrice), 0) FROM ArticleServiceLine asv WHERE asv.article.order.id = :orderId")
    BigDecimal sumAppliedPriceByOrderId(@Param("orderId") Long orderId);

    /**
     * AJOUT : vérifie si un tarif (couple type de vêtement / service) a déjà
     * été appliqué à au moins un article existant, utilisée pour avertir
     * avant suppression d'un ServicePrice encore référencé. Voir revue de
     * code, règle manquante n°1 (section Tarifs des services).
     */
    @Query("SELECT COUNT(asv) > 0 FROM ArticleServiceLine asv WHERE asv.article.clothingType = :clothingType AND asv.service = :service")
    boolean existsByClothingTypeAndService(@Param("clothingType") com.template.vivid.model.enums.ClothingType clothingType,
                                           @Param("service") com.template.vivid.model.enums.ServiceType service);
}

