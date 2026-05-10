package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Status;
import com.razza.bookingsystem.domain.Tenant;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserDeleteException;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 *
 * Covers the makeAdmin and deleteUser operations, verifying correct behavior
 * for happy paths, tenant-scoping enforcement, and expected exception cases.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private Tenant tenant;
    private User user;
    private UUID userId;

    /**
     * Sets up a default tenant and user before each test.
     * The user belongs to the tenant and starts with the USER role.
     */
    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("acme")
                .build();

        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("user@example.com")
                .password("hashedPassword")
                .role(Role.USER)
                .tenant(tenant)
                .build();
    }


    /**
     * Given a valid user in the tenant, makeAdmin should return a UserDto
     * reflecting the updated ADMIN role and the original email address.
     */
    @Transactional
    @Test
    void makeAdmin_success_returnsUpdatedUserDto() {
        UserDto adminDto = UserDto.builder()
                .id(userId)
                .email("user@example.com")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByIdAndTenantId(userId, tenant.getId()))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(adminDto);

        UserDto result = userService.makeAdmin(userId, tenant);

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getEmail()).isEqualTo("user@example.com");
    }

    /**
     * makeAdmin should mutate the user entity's role to ADMIN before saving.
     */
    @Transactional
    @Test
    void makeAdmin_updatesUserRoleToAdmin() {
        when(userRepository.findByIdAndTenantId(userId, tenant.getId()))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any())).thenReturn(new UserDto());

        userService.makeAdmin(userId, tenant);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    /**
     * makeAdmin should throw ResourceNotFoundException when no user with the
     * given ID exists within the tenant, and must not attempt to save anything.
     */
    @Transactional
    @Test
    void makeAdmin_throwsResourceNotFoundException_whenUserNotFoundInTenant() {
        when(userRepository.findByIdAndTenantId(userId, tenant.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.makeAdmin(userId, tenant))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(userRepository, never()).save(any());
    }

    /**
     * makeAdmin should throw ResourceNotFoundException when the user exists but
     * belongs to a different tenant, enforcing tenant isolation.
     */
    @Transactional
    @Test
    void makeAdmin_userInDifferentTenant_throwsResourceNotFoundException() {
        Tenant otherTenant = Tenant.builder().id(UUID.randomUUID()).name("other").build();
        when(userRepository.findByIdAndTenantId(userId, otherTenant.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.makeAdmin(userId, otherTenant))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * deleteUser should successfully delete a user who has no confirmed bookings.
     */
    @Transactional
    @Test
    void deleteUser_success_deletesUserWithNoActiveBookings() {
        when(userRepository.findByIdAndTenantId(userId, tenant.getId()))
                .thenReturn(Optional.of(user));
        when(bookingRepository.countByUserIdAndStatus(userId, Status.CONFIRMED))
                .thenReturn(0);

        userService.deleteUser(userId, tenant);

        verify(userRepository).delete(user);
    }

    /**
     * deleteUser should throw ResourceNotFoundException when no user with the
     * given ID exists within the tenant, and must not attempt to delete anything.
     */
    @Transactional
    @Test
    void deleteUser_throwsResourceNotFoundException_whenUserNotFoundInTenant() {
        when(userRepository.findByIdAndTenantId(userId, tenant.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(userId, tenant))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(userRepository, never()).delete(any());
    }

    /**
     * deleteUser should throw UserDeleteException when the user has one or more
     * confirmed bookings, and the exception message should include the booking count.
     */
    @Transactional
    @Test
    void deleteUser_throwsUserDeleteException_whenUserHasConfirmedBookings() {
        when(userRepository.findByIdAndTenantId(userId, tenant.getId()))
                .thenReturn(Optional.of(user));
        when(bookingRepository.countByUserIdAndStatus(userId, Status.CONFIRMED))
                .thenReturn(3);

        assertThatThrownBy(() -> userService.deleteUser(userId, tenant))
                .isInstanceOf(UserDeleteException.class)
                .hasMessageContaining("3");

        verify(userRepository, never()).delete(any());
    }


    /**
     * deleteUser should only check for CONFIRMED bookings; canceled or other
     * statuses must not influence the deletion.
     */
    @Transactional
    @Test
    void deleteUser_checksOnlyConfirmedBookings() {
        when(userRepository.findByIdAndTenantId(userId, tenant.getId()))
                .thenReturn(Optional.of(user));
        when(bookingRepository.countByUserIdAndStatus(userId, Status.CONFIRMED))
                .thenReturn(0);

        userService.deleteUser(userId, tenant);

        verify(bookingRepository).countByUserIdAndStatus(userId, Status.CONFIRMED);
        verify(bookingRepository, never()).countByUserIdAndStatus(userId, Status.CANCELLED);
    }

    /**
     * deleteUser should throw ResourceNotFoundException when the user exists but
     * belongs to a different tenant, enforcing tenant isolation.
     */
    @Transactional
    @Test
    void deleteUser_userInDifferentTenant_throwsResourceNotFoundException() {
        Tenant otherTenant = Tenant.builder().id(UUID.randomUUID()).name("other").build();
        when(userRepository.findByIdAndTenantId(userId, otherTenant.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(userId, otherTenant))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }
}