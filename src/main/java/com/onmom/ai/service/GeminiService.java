package com.onmom.ai.service;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiService {

    private static final Pattern OUTPUT_TEXT_PATTERN = Pattern.compile("\"output_text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String ask(String message) {
        return askWithContext(message, "");
    }

    public String createFamilyInsight(String sourceText) {
        return askWithContext("""
                아래 산모의 말을 가족이 이해하기 쉽게 변환해줘.

                출력 형식은 반드시 아래 3개 구역만 사용해.
                [이렇게 말했어요]
                산모의 말을 짧게 요약

                [AI가 분석한 상태]
                감정과 상황을 가족이 이해할 수 있게 설명. 단, 진단/처방 금지.

                [이렇게 말해봐요]
                가족이 산모에게 보내면 좋은 따뜻한 한두 문장.

                산모의 말:
                %s
                """.formatted(sourceText), "");
    }

    public String createEmotionReport(LocalDate recordDate, Integer moodScore, String moodLabel, String noteText) {
        return askWithContext("""
                아래 산모의 날짜별 감정 기록을 바탕으로 앱에서 보여줄 AI 리포트를 작성해줘.

                날짜: %s
                감정 점수: %s/5
                감정 라벨: %s
                메모: %s

                작성 규칙:
                - 진단하거나 치료/처방하지 마.
                - 산모가 느낀 감정을 먼저 공감해.
                - 가능한 원인은 "그럴 수 있어요" 수준으로만 설명해.
                - 오늘 해볼 수 있는 아주 가벼운 자기돌봄 행동을 2~3개 제안해.
                - 출혈, 심한 복통, 태동 감소, 고열, 심한 두통, 시야 이상, 호흡곤란, 자해 생각이 있으면 병원/주치의/응급 상담을 권장해.
                - 가족에게 공유하면 좋은 한 문장을 포함해.
                - 한국어로 5~8문장 정도로 작성해.
                """.formatted(
                recordDate,
                moodScore,
                moodLabel,
                noteText == null || noteText.isBlank() ? "없음" : noteText
        ), "");
    }

    public String askWithContext(String message, String conversationContext) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.AI_API_KEY_NOT_CONFIGURED);
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "store", false,
                "system_instruction", """
                        너는 산모와 가족을 돕는 정서 지원 앱 '온맘'의 AI 도우미야.

                        핵심 역할:
                        - 사용자의 감정을 먼저 충분히 공감하고 불안을 완화한다.
                        - 사용자가 더 이야기할 수 있도록 부드럽게 대화를 이어간다.
                        - 사용자의 메시지에서 위험 신호를 감지한다.
                        - 응급 가능성을 낮음/주의/긴급 중 하나로 조심스럽게 분류한다.
                        - 병원, 응급실, 119, 주치의 상담이 필요한지 안내한다.
                        - 사용자가 지금 확인할 수 있는 체크리스트를 제공한다.
                        - 사용자의 질문에 답하되, 의사처럼 진단하거나 처방하지 않는다.
                        - 불확실한 의료 정보는 단정하지 않고 전문가 확인을 권장한다.
                        - 사용자의 상태를 짧게 요약한다.

                        안전 규칙:
                        - 절대 질병을 확정 진단하지 마.
                        - 약 복용, 중단, 용량 변경을 지시하지 마.
                        - 검사 결과를 확정 해석하지 마.
                        - 증상이 심하거나 애매하면 병원 또는 전문가 상담을 권장해.
                        - 자해, 극심한 통증, 출혈, 호흡곤란, 의식 저하, 태동 감소, 고열, 심한 두통, 시야 이상, 경련, 심한 복통, 양수 의심, 심한 우울/공황이 보이면 긴급 도움을 권장해.

                        답변 형식:
                        1. 첫 문단은 사용자의 감정을 구체적으로 짚어서 공감해.
                           예: "아침부터 눈물이 나고 짜증까지 겹쳤다면 오늘 정말 많이 지쳐 있으셨겠어요."
                        2. 둘째 문단은 임신 중 감정 변화가 생길 수 있음을 부드럽게 설명해.
                           단, "정상이다", "문제없다"처럼 단정하지 말고 "그럴 수 있어요", "흔히 겪을 수 있어요"처럼 말해.
                        3. 다음 문단은 사용자가 더 말할 수 있게 초대해.
                           예: "조금 더 얘기해봐요. 제가 차분히 들어볼게요."
                        4. 위험 신호가 조금이라도 보이면 반드시 아래 형식의 체크리스트를 포함해.

                        잠깐! 확인해주세요
                        - 출혈이 있나요?
                        - 심한 복통이 있나요?
                        - 태동이 평소보다 줄었나요?
                        - 열이 나거나 숨쉬기 힘든가요?
                        - 심한 두통, 시야 흐림, 어지러움이 있나요?

                        위 항목 중 하나라도 해당된다면 혼자 판단하지 말고 병원, 주치의, 응급실 또는 119 상담이 필요할 수 있어요.

                        5. 마지막에는 확인 질문 1~2개를 해.
                           예: "태동이 줄었다고 느낀 게 언제부터였나요?", "복통이나 출혈도 함께 있었나요?"

                        답변은 한국어로 작성해.
                        사용자를 겁주지 말되, 태동 감소/출혈/심한 복통 같은 위험 신호는 절대 가볍게 넘기지 마.
                        전체 답변은 6~10문장 정도로 작성하고, 체크리스트는 별도 줄로 보여줘.
                        """,
                "input", buildInput(message, conversationContext),
                "generation_config", Map.of(
                        "temperature", 0.4,
                        "thinking_level", "low"
                )
        );

        try {
            byte[] responseBytes = restClient.post()
                    .uri("/v1beta/interactions")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);

            if (responseBytes == null) {
                return "";
            }

            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
            return extractOutputText(responseBody);
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.AI_MESSAGE_FAILED, e);
        }
    }

    private String extractOutputText(String responseBody) {
        String outputText = findFirstText(OUTPUT_TEXT_PATTERN, responseBody);
        if (!outputText.isBlank()) {
            return outputText;
        }

        String text = findFirstText(TEXT_PATTERN, responseBody);
        return text.isBlank() ? responseBody : text;
    }

    private String findFirstText(Pattern pattern, String responseBody) {
        Matcher matcher = pattern.matcher(responseBody);
        return matcher.find() ? unescapeJsonString(matcher.group(1)).trim() : "";
    }

    private String unescapeJsonString(String value) {
        return value
                .replace("\\n", System.lineSeparator())
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String buildInput(String message, String conversationContext) {
        if (conversationContext == null || conversationContext.isBlank()) {
            return message;
        }

        return """
                이전 대화 맥락:
                %s

                사용자의 새 메시지:
                %s
                """.formatted(conversationContext, message);
    }

    public String getModel() {
        return model;
    }
}
