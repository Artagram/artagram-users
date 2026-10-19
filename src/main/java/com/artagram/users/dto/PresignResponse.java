package com.artagram.users.dto;

public record PresignResponse(
        String uploadUrl,
        String publicUrl,
        String key
) {
}
