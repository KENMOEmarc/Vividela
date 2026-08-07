package com.vivid.model.entity;

import com.vivid.model.enums.ServiceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "article_services")
public class ArticleServiceLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "service", nullable = false)
    private ServiceType service;

    @NotNull
    @Column(name = "applied_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal appliedPrice;


    public static Object getArticleId(Object o) {
        return ((ArticleServiceLine) o).getArticle().getId();
    }
}

