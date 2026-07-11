package com.onmom.family.service;

import com.onmom.ai.service.GeminiService;
import com.onmom.emotion.domain.EmotionTranslation;
import com.onmom.emotion.repository.EmotionTranslationRepository;
import com.onmom.family.domain.FamilyConnection;
import com.onmom.family.domain.FamilyConnectionStatus;
import com.onmom.family.domain.FamilyMessage;
import com.onmom.family.dto.CreateFamilyInsightRequest;
import com.onmom.family.dto.FamilyInsightResponse;
import com.onmom.family.repository.FamilyConnectionRepository;
import com.onmom.family.repository.FamilyMessageRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.notification.domain.Notification;
import com.onmom.notification.repository.NotificationRepository;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.domain.PregnancyStatus;
import com.onmom.pregnancy.repository.PregnancyRepository;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserStatus;
import com.onmom.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FamilyInsightService {

    private final UserRepository userRepository;
    private final PregnancyRepository pregnancyRepository;
    private final FamilyConnectionRepository familyConnectionRepository;
    private final EmotionTranslationRepository emotionTranslationRepository;
    private final FamilyMessageRepository familyMessageRepository;
    private final NotificationRepository notificationRepository;
    private final GeminiService geminiService;
    private final FamilyInsightParser familyInsightParser;

    public FamilyInsightService(
            UserRepository userRepository,
            PregnancyRepository pregnancyRepository,
            FamilyConnectionRepository familyConnectionRepository,
            EmotionTranslationRepository emotionTranslationRepository,
            FamilyMessageRepository familyMessageRepository,
            NotificationRepository notificationRepository,
            GeminiService geminiService,
            FamilyInsightParser familyInsightParser
    ) {
        this.userRepository = userRepository;
        this.pregnancyRepository = pregnancyRepository;
        this.familyConnectionRepository = familyConnectionRepository;
        this.emotionTranslationRepository = emotionTranslationRepository;
        this.familyMessageRepository = familyMessageRepository;
        this.notificationRepository = notificationRepository;
        this.geminiService = geminiService;
        this.familyInsightParser = familyInsightParser;
    }

    @Transactional
    public FamilyInsightResponse create(Long currentUserId, CreateFamilyInsightRequest request) {
        validateActiveUser(currentUserId);
        Pregnancy pregnancy = validateMotherAccess(currentUserId, request.pregnancyId());
        List<FamilyConnection> recipients = findRecipients(pregnancy, request.recipientUserId());

        FamilyInsightDraft draft = familyInsightParser.parse(
                geminiService.createFamilyInsight(request.sourceText()),
                request.sourceText()
        );

        EmotionTranslation translation = emotionTranslationRepository.save(new EmotionTranslation(
                pregnancy.getId(),
                currentUserId,
                request.sourceText(),
                draft.aiInterpretation(),
                draft.suggestedMessage()
        ));

        List<Long> familyMessageIds = new ArrayList<>();
        List<Long> notificationIds = new ArrayList<>();
        for (FamilyConnection recipient : recipients) {
            FamilyMessage familyMessage = familyMessageRepository.save(new FamilyMessage(
                    pregnancy.getId(),
                    currentUserId,
                    recipient.getFamilyUserId(),
                    translation.getId(),
                    buildFamilyMessageContent(draft)
            ));
            familyMessageIds.add(familyMessage.getId());

            Notification notification = notificationRepository.save(new Notification(
                    recipient.getFamilyUserId(),
                    pregnancy.getId(),
                    "온맘 AI 인사이트가 도착했어요",
                    draft.suggestedMessage(),
                    familyMessage.getId()
            ));
            notificationIds.add(notification.getId());
        }

        return new FamilyInsightResponse(
                translation.getId(),
                draft.sourceSummary(),
                translation.getAiInterpretation(),
                translation.getSuggestedMessage(),
                familyMessageIds,
                notificationIds
        );
    }

    private void validateActiveUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Pregnancy validateMotherAccess(Long currentUserId, Long pregnancyId) {
        Pregnancy pregnancy = pregnancyRepository.findById(pregnancyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND));

        if (pregnancy.getStatus() != PregnancyStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND);
        }
        if (!pregnancy.getMotherUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PREGNANCY_ACCESS_DENIED);
        }
        return pregnancy;
    }

    private List<FamilyConnection> findRecipients(Pregnancy pregnancy, Long recipientUserId) {
        List<FamilyConnection> connections = familyConnectionRepository
                .findByPregnancyIdAndMotherUserIdAndStatus(
                        pregnancy.getId(),
                        pregnancy.getMotherUserId(),
                        FamilyConnectionStatus.CONNECTED
                );

        if (recipientUserId != null) {
            connections = connections.stream()
                    .filter(connection -> connection.getFamilyUserId().equals(recipientUserId))
                    .toList();
        }

        if (connections.isEmpty()) {
            throw new BusinessException(ErrorCode.CONNECTED_FAMILY_NOT_FOUND);
        }
        return connections;
    }

    private String buildFamilyMessageContent(FamilyInsightDraft draft) {
        return """
                이렇게 말했어요
                %s

                AI가 분석한 상태
                %s

                이렇게 말해봐요
                %s
                """.formatted(draft.sourceSummary(), draft.aiInterpretation(), draft.suggestedMessage()).trim();
    }
}
