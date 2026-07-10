package com.onmom.chat.service;

import com.onmom.ai.domain.AiReport;
import com.onmom.ai.repository.AiReportRepository;
import com.onmom.ai.service.GeminiService;
import com.onmom.chat.domain.ChatMessage;
import com.onmom.chat.domain.ChatSession;
import com.onmom.chat.domain.SenderType;
import com.onmom.chat.dto.ChatMessageResponse;
import com.onmom.chat.dto.CreateChatMessageRequest;
import com.onmom.chat.repository.ChatMessageRepository;
import com.onmom.chat.repository.ChatSessionRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.notification.domain.SafetyAlert;
import com.onmom.notification.repository.SafetyAlertRepository;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.repository.PregnancyRepository;
import com.onmom.user.domain.UserAccount;
import com.onmom.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final PregnancyRepository pregnancyRepository;
    private final SafetyAlertRepository safetyAlertRepository;
    private final AiReportRepository aiReportRepository;
    private final GeminiService geminiService;
    private final SafetySignalDetector safetySignalDetector;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            UserAccountRepository userAccountRepository,
            PregnancyRepository pregnancyRepository,
            SafetyAlertRepository safetyAlertRepository,
            AiReportRepository aiReportRepository,
            GeminiService geminiService,
            SafetySignalDetector safetySignalDetector
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userAccountRepository = userAccountRepository;
        this.pregnancyRepository = pregnancyRepository;
        this.safetyAlertRepository = safetyAlertRepository;
        this.aiReportRepository = aiReportRepository;
        this.geminiService = geminiService;
        this.safetySignalDetector = safetySignalDetector;
    }

    @Transactional
    public ChatMessageResponse createMessage(CreateChatMessageRequest request) {
        validateUser(request.userId());
        Pregnancy pregnancy = validatePregnancyAccess(request.pregnancyId(), request.userId());
        ChatSession session = resolveSession(request, pregnancy);

        String conversationContext = buildConversationContext(session.getId());
        List<SafetySignal> safetySignals = safetySignalDetector.detect(request.message());
        String riskLevel = safetySignalDetector.riskLevel(safetySignals);

        ChatMessage userMessage = chatMessageRepository.save(new ChatMessage(
                session.getId(),
                SenderType.USER,
                request.message(),
                userMetadata(riskLevel, safetySignals)
        ));

        String answer = geminiService.askWithContext(request.message(), conversationContext);
        ChatMessage aiMessage = chatMessageRepository.save(new ChatMessage(
                session.getId(),
                SenderType.AI,
                answer,
                aiMetadata(riskLevel, userMessage.getId())
        ));

        List<Long> safetyAlertIds = saveSafetyAlerts(request.pregnancyId(), userMessage.getId(), safetySignals);
        AiReport aiReport = aiReportRepository.save(new AiReport(
                request.pregnancyId(),
                "CHAT_SUMMARY",
                "AI 채팅 상태 요약",
                buildReportContent(request.message(), answer, riskLevel, safetySignals),
                geminiService.getModel()
        ));

        return new ChatMessageResponse(
                session.getId(),
                userMessage.getId(),
                aiMessage.getId(),
                answer,
                riskLevel,
                safetyAlertIds,
                aiReport.getId()
        );
    }

    private void validateUser(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Pregnancy validatePregnancyAccess(Long pregnancyId, Long userId) {
        Pregnancy pregnancy = pregnancyRepository.findById(pregnancyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND));

        if (!"ACTIVE".equals(pregnancy.getStatus())) {
            throw new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND);
        }

        if (!pregnancy.getMotherUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PREGNANCY_ACCESS_DENIED);
        }

        return pregnancy;
    }

    private ChatSession resolveSession(CreateChatMessageRequest request, Pregnancy pregnancy) {
        if (request.chatSessionId() == null) {
            return chatSessionRepository.save(new ChatSession(pregnancy.getId(), request.userId()));
        }

        return chatSessionRepository
                .findByIdAndPregnancyIdAndUserId(request.chatSessionId(), pregnancy.getId(), request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND));
    }

    private String buildConversationContext(Long sessionId) {
        List<ChatMessage> recentMessages = new ArrayList<>(
                chatMessageRepository.findTop12BySessionIdOrderByCreatedAtDesc(sessionId)
        );
        recentMessages.sort(Comparator.comparing(ChatMessage::getCreatedAt));

        StringBuilder builder = new StringBuilder();
        for (ChatMessage message : recentMessages) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(message.getSenderType() == SenderType.USER ? "사용자: " : "온맘 AI: ");
            builder.append(message.getContent());
        }
        return builder.toString();
    }

    private List<Long> saveSafetyAlerts(Long pregnancyId, Long sourceMessageId, List<SafetySignal> safetySignals) {
        List<Long> alertIds = new ArrayList<>();
        for (SafetySignal signal : safetySignals) {
            SafetyAlert alert = safetyAlertRepository.save(new SafetyAlert(
                    pregnancyId,
                    sourceMessageId,
                    signal.alertType(),
                    signal.severity(),
                    signal.title(),
                    signal.description(),
                    signal.recommendation()
            ));
            alertIds.add(alert.getId());
        }
        return alertIds;
    }

    private String buildReportContent(String userMessage, String answer, String riskLevel, List<SafetySignal> safetySignals) {
        StringBuilder builder = new StringBuilder();
        builder.append("위험도: ").append(riskLevel).append(System.lineSeparator());
        builder.append("사용자 메시지: ").append(userMessage).append(System.lineSeparator());
        builder.append("AI 응답: ").append(answer).append(System.lineSeparator());

        if (!safetySignals.isEmpty()) {
            builder.append("감지된 위험 신호: ");
            builder.append(safetySignals.stream().map(SafetySignal::alertType).toList());
        }

        return builder.toString();
    }

    private String userMetadata(String riskLevel, List<SafetySignal> safetySignals) {
        return """
                {"riskLevel":"%s","safetySignals":"%s"}
                """.formatted(jsonEscape(riskLevel), jsonEscape(safetySignals.stream().map(SafetySignal::alertType).toList().toString())).trim();
    }

    private String aiMetadata(String riskLevel, Long sourceMessageId) {
        return """
                {"riskLevel":"%s","sourceMessageId":%d}
                """.formatted(jsonEscape(riskLevel), sourceMessageId).trim();
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(System.lineSeparator(), "\\n");
    }
}
