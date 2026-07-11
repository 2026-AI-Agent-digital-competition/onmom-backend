package com.onmom.emotion.service;

import com.onmom.ai.domain.AiReport;
import com.onmom.ai.repository.AiReportRepository;
import com.onmom.ai.service.GeminiService;
import com.onmom.emotion.domain.EmotionRecord;
import com.onmom.emotion.dto.CreateEmotionRecordRequest;
import com.onmom.emotion.dto.EmotionAiReportResponse;
import com.onmom.emotion.dto.EmotionCalendarDayResponse;
import com.onmom.emotion.dto.EmotionCalendarResponse;
import com.onmom.emotion.dto.EmotionRecordResponse;
import com.onmom.emotion.repository.EmotionRecordRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.domain.PregnancyStatus;
import com.onmom.pregnancy.repository.PregnancyRepository;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserStatus;
import com.onmom.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmotionRecordService {

    private static final String EMOTION_DAILY_REPORT = "EMOTION_DAILY_REPORT";

    private final EmotionRecordRepository emotionRecordRepository;
    private final AiReportRepository aiReportRepository;
    private final UserRepository userRepository;
    private final PregnancyRepository pregnancyRepository;
    private final GeminiService geminiService;

    public EmotionRecordService(
            EmotionRecordRepository emotionRecordRepository,
            AiReportRepository aiReportRepository,
            UserRepository userRepository,
            PregnancyRepository pregnancyRepository,
            GeminiService geminiService
    ) {
        this.emotionRecordRepository = emotionRecordRepository;
        this.aiReportRepository = aiReportRepository;
        this.userRepository = userRepository;
        this.pregnancyRepository = pregnancyRepository;
        this.geminiService = geminiService;
    }

    @Transactional
    public EmotionRecordResponse createOrUpdate(Long currentUserId, CreateEmotionRecordRequest request) {
        validateActiveUser(currentUserId);
        validateMotherAccess(currentUserId, request.pregnancyId());

        EmotionRecord record = emotionRecordRepository
                .findByPregnancyIdAndRecordDate(request.pregnancyId(), request.recordDate())
                .map(existing -> {
                    existing.update(
                            currentUserId,
                            request.moodScore(),
                            request.moodLabel(),
                            request.noteText(),
                            request.source()
                    );
                    return existing;
                })
                .orElseGet(() -> new EmotionRecord(
                        request.pregnancyId(),
                        currentUserId,
                        request.recordDate(),
                        request.moodScore(),
                        request.moodLabel(),
                        request.noteText(),
                        request.source()
                ));

        EmotionRecord savedRecord = emotionRecordRepository.save(record);
        AiReport aiReport = createOrUpdateAiReport(savedRecord);

        return EmotionRecordResponse.from(savedRecord, aiReport.getId());
    }

    @Transactional(readOnly = true)
    public EmotionRecordResponse findDaily(Long currentUserId, Long pregnancyId, LocalDate recordDate) {
        validateActiveUser(currentUserId);
        validateMotherAccess(currentUserId, pregnancyId);

        EmotionRecord record = emotionRecordRepository
                .findByPregnancyIdAndRecordDate(pregnancyId, recordDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMOTION_RECORD_NOT_FOUND));

        return EmotionRecordResponse.from(record, findEmotionAiReportId(record));
    }

    @Transactional(readOnly = true)
    public EmotionCalendarResponse findCalendar(Long currentUserId, Long pregnancyId, int year, int month) {
        validateActiveUser(currentUserId);
        validateMotherAccess(currentUserId, pregnancyId);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<EmotionCalendarDayResponse> days = emotionRecordRepository
                .findByPregnancyIdAndRecordDateBetweenOrderByRecordDateAsc(pregnancyId, startDate, endDate)
                .stream()
                .map(record -> EmotionCalendarDayResponse.of(record, findEmotionAiReportId(record)))
                .toList();

        return new EmotionCalendarResponse(pregnancyId, year, month, days);
    }

    @Transactional
    public EmotionAiReportResponse findAiReport(Long currentUserId, Long emotionRecordId) {
        validateActiveUser(currentUserId);

        EmotionRecord record = emotionRecordRepository.findById(emotionRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMOTION_RECORD_NOT_FOUND));
        validateMotherAccess(currentUserId, record.getPregnancyId());

        AiReport aiReport = aiReportRepository
                .findTopByEmotionRecordIdAndReportTypeOrderByGeneratedAtDesc(record.getId(), EMOTION_DAILY_REPORT)
                .orElseGet(() -> createOrUpdateAiReport(record));

        return EmotionAiReportResponse.from(aiReport);
    }

    private void validateActiveUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateMotherAccess(Long currentUserId, Long pregnancyId) {
        Pregnancy pregnancy = pregnancyRepository.findById(pregnancyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND));

        if (pregnancy.getStatus() != PregnancyStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND);
        }
        if (!pregnancy.getMotherUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PREGNANCY_ACCESS_DENIED);
        }
    }

    private AiReport createOrUpdateAiReport(EmotionRecord record) {
        String content = geminiService.createEmotionReport(
                record.getRecordDate(),
                record.getMoodScore(),
                record.getMoodLabel(),
                record.getNoteText()
        );
        String title = "%s 감정 리포트".formatted(record.getMoodLabel());

        return aiReportRepository
                .findTopByEmotionRecordIdAndReportTypeOrderByGeneratedAtDesc(record.getId(), EMOTION_DAILY_REPORT)
                .map(existingReport -> {
                    existingReport.update(title, content, geminiService.getModel());
                    return existingReport;
                })
                .orElseGet(() -> aiReportRepository.save(new AiReport(
                        record.getPregnancyId(),
                        record.getId(),
                        EMOTION_DAILY_REPORT,
                        title,
                        content,
                        geminiService.getModel()
                )));
    }

    private Long findEmotionAiReportId(EmotionRecord record) {
        return aiReportRepository
                .findTopByEmotionRecordIdAndReportTypeOrderByGeneratedAtDesc(record.getId(), EMOTION_DAILY_REPORT)
                .map(AiReport::getId)
                .orElse(null);
    }
}
