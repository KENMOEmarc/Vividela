package com.template.vivid.model.requests;

import com.template.vivid.model.dto.ArticleDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    @NotNull
    private Long clientId;

    @NotNull
    private LocalDate depositDate;

    private LocalDate expectedDeliveryDate;

    private String note;

    @NotNull
    private List<ArticleDto> articles;
}

