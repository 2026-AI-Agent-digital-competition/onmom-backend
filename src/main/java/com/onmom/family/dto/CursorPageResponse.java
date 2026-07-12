package com.onmom.family.dto;

public record CursorPageResponse(
        String nextCursor,
        int size,
        boolean hasNext
) {
}
