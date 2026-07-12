package com.onmom.family.dto;

import java.util.List;

public record FamilyMessageListResponse(
        List<FamilyMessageResponse> content,
        CursorPageResponse page
) {
}
