package com.template.vivid.model.mapper;

import com.template.vivid.model.payloads.responses.AuthResponse;
import com.template.vivid.model.dto.UserDto;
import com.template.vivid.model.entity.User;
import com.template.vivid.model.enums.RoleType;

/**
 * Mapper centralisant la conversion User (entity) ↔ UserDto / AuthResponse.UserInfo.
 */
public final class UserMapper {

    private UserMapper() {
        // Classe utilitaire : pas d'instanciation
    }

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return UserDto.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? RoleType.valueOf(user.getRole().toString()) : null)
                .enabled(Boolean.TRUE.equals(user.getIsActive()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static AuthResponse.UserInfo toUserInfo(User user) {
        if (user == null) {
            return null;
        }
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? String.valueOf(user.getRole()) : null)
                .build();
    }

    public static User toUser(UserDto issuer) {
        if (issuer == null) {
            return null;
        }
        return User.builder()
                .id(issuer.getId())
                .userName(issuer.getUserName())
                .email(issuer.getEmail())
                .phone(issuer.getPhone())
                .firstName(issuer.getFirstName())
                .lastName(issuer.getLastName())
                .role(issuer.getRole() != null ? issuer.getRole() : null)
                .createdAt(issuer.getCreatedAt())
                .updatedAt(issuer.getUpdatedAt())
                .build();
    }
}
