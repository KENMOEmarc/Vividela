package com.vivid.service;

import com.vivid.model.payloads.requests.ArticleCreateRequest;
import com.vivid.model.dto.ArticleDto;
import com.vivid.model.payloads.requests.ArticleUpdateRequest;
import com.vivid.model.entity.Article;

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
