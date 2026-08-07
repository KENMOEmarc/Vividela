package com.vivid.model.mapper;

import com.vivid.model.payloads.responses.AuthResponse;
import com.vivid.model.dto.UserDto;
import com.vivid.model.entity.User;
import com.vivid.model.enums.RoleType;

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
                .id(issuer.id())
                .userName(issuer.userName())
                .email(issuer.email())
                .phone(issuer.phone())
                .firstName(issuer.firstName())
                .lastName(issuer.lastName())
                .role(issuer.role() != null ? issuer.role() : null)
                .createdAt(issuer.createdAt())
                .updatedAt(issuer.updatedAt())
                .build();
    }
}
