package com.onmom.chat.service;

import com.onmom.ai.domain.AiReport;
import com.onmom.ai.repository.AiReportRepository;
import com.onmom.ai.service.GeminiService;
import com.onmom.chat.domain.ChatMessage;
import com.onmom.chat.domain.ChatSession;
import com.onmom.chat.domain.SenderType;
import com.onmom.chat.dto.ChatCursorPageResponse;
import com.onmom.chat.dto.ChatMessageItemResponse;
import com.onmom.chat.dto.ChatMessageListResponse;
import com.onmom.chat.dto.ChatMessageResponse;
import com.onmom.chat.dto.CreateChatMessageRequest;
import com.onmom.chat.repository.ChatMessageQueryRepository;
import com.onmom.chat.repository.ChatMessageRepository;
import com.onmom.chat.repository.ChatSessionRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.notification.domain.SafetyAlert;
import com.onmom.notification.repository.SafetyAlertRepository;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.domain.PregnancyStatus;
import com.onmom.pregnancy.repository.PregnancyRepository;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserStatus;
import com.onmom.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ChatService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageQueryRepository chatMessageQueryRepository;
    private final UserRepository userRepository;
    private final PregnancyRepository pregnancyRepository;
    private final SafetyAlertRepository safetyAlertRepository;
    private final AiReportRepository aiReportRepository;
    private final GeminiService geminiService;
    private final SafetySignalDetector safetySignalDetector;
    private final ChatMessageCursorCodec cursorCodec;
    private final TransactionTemplate transactionTemplate;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            ChatMessageQueryRepository chatMessageQueryRepository,
            UserRepository userRepository,
            PregnancyRepository pregnancyRepository,
            SafetyAlertRepository safetyAlertRepository,
            AiReportRepository aiReportRepository,
            GeminiService geminiService,
            SafetySignalDetector safetySignalDetector,
            ChatMessageCursorCodec cursorCodec,
            TransactionTemplate transactionTemplate
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageQueryRepository = chatMessageQueryRepository;
        this.userRepository = userRepository;
        this.pregnancyRepository = pregnancyRepository;
        this.safetyAlertRepository = safetyAlertRepository;
        this.aiReportRepository = aiReportRepository;
        this.geminiService = geminiService;
        this.safetySignalDetector = safetySignalDetector;
        this.cursorCodec = cursorCodec;
        this.transactionTemplate = transactionTemplate;
    }

    public ChatMessageResponse createMessage(Long currentUserId, CreateChatMessageRequest request) {
        ChatMessagePreparation preparation = Objects.requireNonNull(transactionTemplate.execute(status ->
                prepareUserMessage(currentUserId, request)
        ));

        String answer = geminiService.askWithContext(request.message(), preparation.conversationContext());

        ChatAiPersistence aiPersistence = Objects.requireNonNull(transactionTemplate.execute(status ->
                saveAiResponse(preparation, answer)
        ));

        return new ChatMessageResponse(
                preparation.sessionId(),
                preparation.userMessageId(),
                aiPersistence.aiMessageId(),
                answer,
                preparation.riskLevel(),
                preparation.safetyAlertIds(),
                aiPersistence.aiReportId()
        );
    }

    private ChatMessagePreparation prepareUserMessage(Long currentUserId, CreateChatMessageRequest request) {
        validateUser(currentUserId);
        Pregnancy pregnancy = validatePregnancyAccess(request.pregnancyId(), currentUserId);
        ChatSession session = resolveSession(request, pregnancy, currentUserId);

        String conversationContext = buildConversationContext(session.getId());
        List<SafetySignal> safetySignals = safetySignalDetector.detect(request.message());
        String riskLevel = safetySignalDetector.riskLevel(safetySignals);

        ChatMessage userMessage = chatMessageRepository.save(new ChatMessage(
                session.getId(),
                SenderType.USER,
                request.message(),
                userMetadata(riskLevel, safetySignals)
        ));

        List<Long> safetyAlertIds = saveSafetyAlerts(request.pregnancyId(), userMessage.getId(), safetySignals);

        return new ChatMessagePreparation(
                pregnancy.getId(),
                session.getId(),
                userMessage.getId(),
                request.message(),
                conversationContext,
                riskLevel,
                safetySignals.stream().map(SafetySignal::alertType).toList(),
                safetyAlertIds
        );
    }

    private ChatAiPersistence saveAiResponse(ChatMessagePreparation preparation, String answer) {
        ChatMessage aiMessage = chatMessageRepository.save(new ChatMessage(
                preparation.sessionId(),
                SenderType.AI,
                answer,
                aiMetadata(preparation.riskLevel(), preparation.userMessageId())
        ));

        AiReport aiReport = aiReportRepository.save(new AiReport(
                preparation.pregnancyId(),
                "CHAT_SUMMARY",
                "AI 채팅 상태 요약",
                buildReportContent(preparation.userMessage(), answer, preparation.riskLevel(), preparation.safetySignalTypes()),
                geminiService.getModel()
        ));

        return new ChatAiPersistence(aiMessage.getId(), aiReport.getId());
    }

    @Transactional(readOnly = true)
    public ChatMessageListResponse findMessages(
            Long currentUserId,
            Long chatSessionId,
            String cursor,
            Integer size
    ) {
        validateUser(currentUserId);
        ChatSession session = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND));

        if (!session.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }

        int pageSize = normalizeSize(size);
        ChatMessageCursor decodedCursor = cursorCodec.decode(cursor);
        List<ChatMessage> messages = chatMessageQueryRepository.findBySessionId(
                session.getId(),
                decodedCursor == null ? null : decodedCursor.createdAt(),
                decodedCursor == null ? null : decodedCursor.id(),
                pageSize + 1
        );

        boolean hasNext = messages.size() > pageSize;
        List<ChatMessage> pageMessages = hasNext ? messages.subList(0, pageSize) : messages;
        String nextCursor = hasNext ? createNextCursor(pageMessages, cursorCodec) : null;

        List<ChatMessageItemResponse> content = pageMessages.stream()
                .map(ChatMessageItemResponse::from)
                .toList();

        return new ChatMessageListResponse(
                session.getId(),
                content,
                new ChatCursorPageResponse(nextCursor, pageSize, hasNext)
        );
    }

    private void validateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Pregnancy validatePregnancyAccess(Long pregnancyId, Long userId) {
        Pregnancy pregnancy = pregnancyRepository.findById(pregnancyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND));

        if (pregnancy.getStatus() != PregnancyStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND);
        }

        if (!pregnancy.getMotherUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PREGNANCY_ACCESS_DENIED);
        }

        return pregnancy;
    }

    private ChatSession resolveSession(CreateChatMessageRequest request, Pregnancy pregnancy, Long currentUserId) {
        if (request.chatSessionId() == null) {
            return chatSessionRepository.save(new ChatSession(pregnancy.getId(), currentUserId));
        }

        return chatSessionRepository
                .findByIdAndPregnancyIdAndUserId(request.chatSessionId(), pregnancy.getId(), currentUserId)
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

    private String buildReportContent(String userMessage, String answer, String riskLevel, List<String> safetySignalTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append("위험도: ").append(riskLevel).append(System.lineSeparator());
        builder.append("사용자 메시지: ").append(userMessage).append(System.lineSeparator());
        builder.append("AI 응답: ").append(answer).append(System.lineSeparator());

        if (!safetySignalTypes.isEmpty()) {
            builder.append("감지된 위험 신호: ");
            builder.append(safetySignalTypes);
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

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String createNextCursor(List<ChatMessage> messages, ChatMessageCursorCodec cursorCodec) {
        ChatMessage lastMessage = messages.get(messages.size() - 1);
        return cursorCodec.encode(lastMessage.getCreatedAt(), lastMessage.getId());
    }

    private record ChatMessagePreparation(
            Long pregnancyId,
            Long sessionId,
            Long userMessageId,
            String userMessage,
            String conversationContext,
            String riskLevel,
            List<String> safetySignalTypes,
            List<Long> safetyAlertIds
    ) {
    }

    private record ChatAiPersistence(
            Long aiMessageId,
            Long aiReportId
    ) {
    }
}
