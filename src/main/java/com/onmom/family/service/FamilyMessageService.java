package com.onmom.family.service;

import com.onmom.family.domain.FamilyMessage;
import com.onmom.family.dto.CursorPageResponse;
import com.onmom.family.dto.FamilyMessageListResponse;
import com.onmom.family.dto.FamilyMessageResponse;
import com.onmom.family.repository.FamilyMessageQueryRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserStatus;
import com.onmom.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyMessageService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserRepository userRepository;
    private final FamilyMessageQueryRepository familyMessageQueryRepository;
    private final FamilyMessageCursorCodec cursorCodec;

    public FamilyMessageService(
            UserRepository userRepository,
            FamilyMessageQueryRepository familyMessageQueryRepository,
            FamilyMessageCursorCodec cursorCodec
    ) {
        this.userRepository = userRepository;
        this.familyMessageQueryRepository = familyMessageQueryRepository;
        this.cursorCodec = cursorCodec;
    }

    @Transactional(readOnly = true)
    public FamilyMessageListResponse findReceived(Long currentUserId, String cursor, Integer size) {
        validateActiveUser(currentUserId);

        int pageSize = normalizeSize(size);
        FamilyMessageCursor decodedCursor = cursorCodec.decode(cursor);
        List<FamilyMessage> messages = familyMessageQueryRepository.findReceivedMessages(
                currentUserId,
                decodedCursor == null ? null : decodedCursor.createdAt(),
                decodedCursor == null ? null : decodedCursor.id(),
                pageSize + 1
        );

        boolean hasNext = messages.size() > pageSize;
        List<FamilyMessage> pageMessages = hasNext ? messages.subList(0, pageSize) : messages;
        String nextCursor = hasNext ? createNextCursor(pageMessages) : null;

        List<FamilyMessageResponse> content = pageMessages.stream()
                .map(FamilyMessageResponse::from)
                .toList();

        return new FamilyMessageListResponse(
                content,
                new CursorPageResponse(nextCursor, pageSize, hasNext)
        );
    }

    private void validateActiveUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String createNextCursor(List<FamilyMessage> messages) {
        FamilyMessage lastMessage = messages.get(messages.size() - 1);
        return cursorCodec.encode(lastMessage.getCreatedAt(), lastMessage.getId());
    }
}
