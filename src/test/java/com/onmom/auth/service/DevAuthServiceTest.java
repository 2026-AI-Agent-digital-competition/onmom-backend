package com.onmom.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onmom.auth.dto.DevAccessTokenResponse;
import com.onmom.global.auth.JwtProperties;
import com.onmom.global.auth.JwtTokenProvider;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import com.onmom.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DevAuthServiceTest {

    private UserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private DevAuthService service;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        jwtTokenProvider = org.mockito.Mockito.mock(JwtTokenProvider.class);
        jwtProperties = org.mockito.Mockito.mock(JwtProperties.class);
        service = new DevAuthService(userRepository, jwtTokenProvider, jwtProperties);
    }

    @Test
    void demoLoginCreatesMotherAndReturnsAccessTokenWhenDatabaseIsEmpty() {
        User savedUser = user(1L, "온맘 데모 산모", UserRole.MOTHER);
        when(userRepository.findFirstByNicknameAndPrimaryRoleOrderByIdAsc("온맘 데모 산모", UserRole.MOTHER))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.createAccessToken(1L, UserRole.MOTHER)).thenReturn("demo-token");
        when(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L);

        DevAccessTokenResponse response = service.loginAsDemoMother();

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.role()).isEqualTo("MOTHER");
        assertThat(response.accessToken()).isEqualTo("demo-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void demoLoginReusesExistingDemoMother() {
        User existingUser = user(2L, "온맘 데모 산모", UserRole.MOTHER);
        when(userRepository.findFirstByNicknameAndPrimaryRoleOrderByIdAsc("온맘 데모 산모", UserRole.MOTHER))
                .thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createAccessToken(2L, UserRole.MOTHER)).thenReturn("existing-token");
        when(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L);

        DevAccessTokenResponse response = service.loginAsDemoMother();

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.accessToken()).isEqualTo("existing-token");
    }

    private User user(Long id, String nickname, UserRole role) {
        User user = User.create(nickname, null, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
