package com.template.auth.service;

import com.template.auth.model.dto.ArticleCreateRequest;
import com.template.auth.model.dto.ArticleDto;
import com.template.auth.model.dto.ArticleUpdateRequest;
import com.template.auth.model.dto.ProductCreateRequest;
import com.template.auth.model.entity.Article;

import java.util.List;

public interface ArticleService {

    ArticleDto create(ArticleCreateRequest request, Long orderId);

    ArticleDto getArticleById(Long id);

    List<ArticleDto> getAllArticles();

    ArticleDto updateArticle(Long id, ArticleUpdateRequest request);

    void deleteArticle(Long id);

    Article findEntityById(Long id);

    Article findByName(String name);

}
