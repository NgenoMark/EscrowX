package com.example.escbackend.user.service;

import com.example.escbackend.auth.service.OtpDeliveryService;
import com.example.escbackend.auth.service.OtpService;
import com.example.escbackend.audit.repository.AuditLogRepository;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.constants.UserStatus;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.notification.repository.InAppNotificationRepository;
import com.example.escbackend.notification.repository.NotificationDeliveryLogRepository;
import com.example.escbackend.notification.service.PushNotificationService;
import com.example.escbackend.user.dto.AdminVerificationConfirmRequest;
import com.example.escbackend.user.dto.CreateMarketplaceUserRequest;
import com.example.escbackend.user.dto.UserDetailsResponse;
import com.example.escbackend.user.dto.UserRoleStatusUpdateResponse;
import com.example.escbackend.user.entity.ProfileEntity;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.ProfileRepository;
import com.example.escbackend.user.repository.RiderProfileRepository;
import com.example.escbackend.user.repository.UserBlacklistRepository;
import com.example.escbackend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAdminOnboardingTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private RiderProfileRepository riderProfileRepository;
    @Mock
    private UserMapperService mapper;
    @Mock
    private UserBlacklistRepository blacklistRepo;
    @Mock
    private AdminAuthorizationService authz;
    @Mock
    private AuditLogRepository auditRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OtpService otpService;
    @Mock
    private OtpDeliveryService otpDeliveryService;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private InAppNotificationRepository inAppNotificationRepository;
    @Mock
    private NotificationDeliveryLogRepository notificationDeliveryLogRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createBuyer_happyPath_setsPendingVerification() {
        UUID actorId = UUID.randomUUID();
        UUID createdUserId = UUID.randomUUID();
        CreateMarketplaceUserRequest request = buildMarketplaceRequest("buyer@example.com", "0700000001");

        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.existsByEmail("buyer@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0700000001")).thenReturn(false);

        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(createdUserId);
            return saved;
        });
        when(profileRepository.save(any(ProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDetails(any(UserEntity.class), any(ProfileEntity.class))).thenReturn(UserDetailsResponse.builder().build());

        userService.createMarketplaceUserByAdmin(actorId, request, UserRole.BUYER);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.BUYER);
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        verify(authz).requireAdminOrSuperAdmin(actorId);
    }

    @Test
    void createSeller_happyPath_setsPendingVerification() {
        UUID actorId = UUID.randomUUID();
        UUID createdUserId = UUID.randomUUID();
        CreateMarketplaceUserRequest request = buildMarketplaceRequest("seller@example.com", "0700000002");

        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.existsByEmail("seller@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0700000002")).thenReturn(false);

        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(createdUserId);
            return saved;
        });
        when(profileRepository.save(any(ProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDetails(any(UserEntity.class), any(ProfileEntity.class))).thenReturn(UserDetailsResponse.builder().build());

        userService.createMarketplaceUserByAdmin(actorId, request, UserRole.SELLER);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.SELLER);
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        verify(authz).requireAdminOrSuperAdmin(actorId);
    }

    @Test
    void createMarketplaceUser_rejectsUnauthorizedActor() {
        UUID actorId = UUID.randomUUID();
        CreateMarketplaceUserRequest request = buildMarketplaceRequest("x@example.com", "0700000003");

        doThrow(new ApiException(HttpStatus.FORBIDDEN, "Forbidden"))
            .when(authz).requireAdminOrSuperAdmin(actorId);

        assertThatThrownBy(() -> userService.createMarketplaceUserByAdmin(actorId, request, UserRole.BUYER))
            .isInstanceOf(ApiException.class)
            .extracting(ex -> ((ApiException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void sendVerificationOtp_happyPath() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserEntity target = marketplaceUser(targetId, UserRole.BUYER, UserStatus.PENDING_VERIFICATION, "buyer@example.com");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(otpService.generate("buyer@example.com", "ADMIN_USER_VERIFY")).thenReturn("123456");

        UserRoleStatusUpdateResponse response = userService.sendVerificationOtpForMarketplaceUser(actorId, targetId);

        assertThat(response.getUserId()).isEqualTo(targetId);
        assertThat(response.getOldValue()).isEqualTo(UserStatus.PENDING_VERIFICATION.name());
        assertThat(response.getNewValue()).isEqualTo(UserStatus.PENDING_VERIFICATION.name());
        verify(authz).requireAdminOrSuperAdmin(actorId);
        verify(otpService).generate("buyer@example.com", "ADMIN_USER_VERIFY");
        verify(otpDeliveryService).sendRegistrationOtp("buyer@example.com", "123456");
    }

    @Test
    void sendVerificationOtp_rejectsNonMarketplaceRole() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserEntity target = marketplaceUser(targetId, UserRole.RIDER, UserStatus.PENDING_VERIFICATION, "rider@example.com");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.sendVerificationOtpForMarketplaceUser(actorId, targetId))
            .isInstanceOf(ApiException.class)
            .extracting(ex -> ((ApiException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(otpService, never()).generate(any(), any());
        verify(otpDeliveryService, never()).sendRegistrationOtp(any(), any());
    }

    @Test
    void sendVerificationOtp_rejectsInvalidState() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserEntity target = marketplaceUser(targetId, UserRole.SELLER, UserStatus.ACTIVE, "seller@example.com");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.sendVerificationOtpForMarketplaceUser(actorId, targetId))
            .isInstanceOf(ApiException.class)
            .extracting(ex -> ((ApiException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(otpService, never()).generate(any(), any());
        verify(otpDeliveryService, never()).sendRegistrationOtp(any(), any());
    }

    @Test
    void verifyOtp_success_activatesUserAndSendsEmail() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserEntity target = marketplaceUser(targetId, UserRole.SELLER, UserStatus.PENDING_VERIFICATION, "seller@example.com");
        AdminVerificationConfirmRequest request = new AdminVerificationConfirmRequest();
        request.setOtp("654321");
        request.setReason("Manual check complete");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRoleStatusUpdateResponse response = userService.verifyMarketplaceUserByOtp(actorId, targetId, request);

        assertThat(response.getOldValue()).isEqualTo(UserStatus.PENDING_VERIFICATION.name());
        assertThat(response.getNewValue()).isEqualTo(UserStatus.ACTIVE.name());
        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(otpService).verify("seller@example.com", "ADMIN_USER_VERIFY", "654321");
        verify(userRepository).save(target);
        verify(otpDeliveryService).sendMarketplaceVerificationSuccessEmail("seller@example.com", UserRole.SELLER.name());
    }

    @Test
    void verifyOtp_wrongOtp_keepsPendingAndDoesNotSendEmail() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserEntity target = marketplaceUser(targetId, UserRole.BUYER, UserStatus.PENDING_VERIFICATION, "buyer@example.com");
        AdminVerificationConfirmRequest request = new AdminVerificationConfirmRequest();
        request.setOtp("000000");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST, "Invalid OTP"))
            .when(otpService).verify("buyer@example.com", "ADMIN_USER_VERIFY", "000000");

        assertThatThrownBy(() -> userService.verifyMarketplaceUserByOtp(actorId, targetId, request))
            .isInstanceOf(ApiException.class)
            .extracting(ex -> ((ApiException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(target.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(otpDeliveryService, never()).sendMarketplaceVerificationSuccessEmail(any(), any());
    }

    @Test
    void verifyOtp_rejectsInvalidState() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserEntity target = marketplaceUser(targetId, UserRole.BUYER, UserStatus.ACTIVE, "buyer@example.com");
        AdminVerificationConfirmRequest request = new AdminVerificationConfirmRequest();
        request.setOtp("111111");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.verifyMarketplaceUserByOtp(actorId, targetId, request))
            .isInstanceOf(ApiException.class)
            .extracting(ex -> ((ApiException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(otpService, never()).verify(any(), any(), any());
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(otpDeliveryService, never()).sendMarketplaceVerificationSuccessEmail(any(), any());
    }

    private CreateMarketplaceUserRequest buildMarketplaceRequest(String email, String phone) {
        CreateMarketplaceUserRequest request = new CreateMarketplaceUserRequest();
        request.setEmail(email);
        request.setPhone(phone);
        request.setPassword("pass123");
        request.setDisplayName("Display Name");
        request.setBusinessName("Biz Name");
        request.setAddress("Address");
        request.setAvatarUrl("https://example.com/avatar.png");
        return request;
    }

    private UserEntity marketplaceUser(UUID id, UserRole role, UserStatus status, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRole(role);
        user.setStatus(status);
        user.setEmail(email);
        return user;
    }
}
