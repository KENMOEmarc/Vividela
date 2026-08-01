package com.template.vivid.service;

import com.template.vivid.model.dto.ArticleCreateRequest;
import com.template.vivid.model.dto.ArticleDto;
import com.template.vivid.model.dto.ArticleUpdateRequest;
import com.template.vivid.model.entity.Article;

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
