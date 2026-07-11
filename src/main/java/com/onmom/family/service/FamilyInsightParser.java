package com.onmom.family.service;

import org.springframework.stereotype.Component;

@Component
class FamilyInsightParser {

    FamilyInsightDraft parse(String aiResponse, String fallbackSourceText) {
        String sourceSummary = extractSection(aiResponse, "이렇게 말했어요", "AI가 분석한 상태");
        String aiInterpretation = extractSection(aiResponse, "AI가 분석한 상태", "이렇게 말해봐요");
        String suggestedMessage = extractSection(aiResponse, "이렇게 말해봐요", null);

        if (sourceSummary.isBlank()) {
            sourceSummary = fallbackSourceText;
        }
        if (aiInterpretation.isBlank()) {
            aiInterpretation = aiResponse == null || aiResponse.isBlank()
                    ? "산모가 정서적 어려움을 표현했습니다."
                    : aiResponse;
        }
        if (suggestedMessage.isBlank()) {
            suggestedMessage = "많이 힘들었겠다. 내가 곁에서 같이 들어줄게.";
        }

        return new FamilyInsightDraft(sourceSummary.trim(), aiInterpretation.trim(), suggestedMessage.trim());
    }

    private String extractSection(String text, String startTitle, String endTitle) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String startMarker = "[" + startTitle + "]";
        int start = text.indexOf(startMarker);
        if (start < 0) {
            return "";
        }
        start += startMarker.length();

        int end = text.length();
        if (endTitle != null) {
            String endMarker = "[" + endTitle + "]";
            int endIndex = text.indexOf(endMarker, start);
            if (endIndex >= 0) {
                end = endIndex;
            }
        }

        return text.substring(start, end).trim();
    }
}
