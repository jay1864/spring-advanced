package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
class AuthServiceTest {

    public final static SignupRequest TEST_SIGNUP_REQUEST = new SignupRequest("test@example.com", "12345678!@A", "USER");
    public final static SigninRequest TEST_SIGNIN_REQUEST = new SigninRequest("nonexistent@example.com", "password123");
    public final static User TEST_USER = new User("test@example.com", "encodedPassword", UserRole.USER);


    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Nested
    class SignupTest{

        @Test
        void 이메일_중복시_예외_발생() {
            // given
            SignupRequest signupRequest = TEST_SIGNUP_REQUEST;
            given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(true);

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    authService.signup(signupRequest));

            // then
            assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());
        }

        @Test
        void 회원가입_성공() {
            // given
            SignupRequest signupRequest = TEST_SIGNUP_REQUEST;
            User user = TEST_USER;
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(user);
            given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn("testToken");

            // when
            SignupResponse signupResponse = authService.signup(signupRequest);

            // then
            assertNotNull(signupResponse);
            assertEquals("testToken", signupResponse.getBearerToken());
        }

    }

    @Nested
    class SigninTest{

        @Test
        void 로그인한_이메일이_존재하지_않을_때_예외_발생() {
            // given
            SigninRequest signinRequest = TEST_SIGNIN_REQUEST;
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                authService.signin(signinRequest);
            });

            // then
            assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
        }

        @Test
        void 비밀번호가_일치하지_않을_때_예외_발생() {
            // given
            SigninRequest signinRequest = new SigninRequest("test@example.com", "wrongPassword");
            User user = TEST_USER;

            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // when
            AuthException exception = assertThrows(AuthException.class, () -> {
                authService.signin(signinRequest);
            });

            // then
            assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        }

        @Test
        void 로그인_성공() {
            // given
            SigninRequest signinRequest = TEST_SIGNIN_REQUEST;
            User user = TEST_USER;
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn("testToken");

            // when
            SigninResponse signinResponse = authService.signin(signinRequest);

            // then
            assertNotNull(signinResponse);
            assertEquals("testToken", signinResponse.getBearerToken());
        }

    }

}