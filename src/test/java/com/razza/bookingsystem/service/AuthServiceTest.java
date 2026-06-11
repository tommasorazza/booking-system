package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.Role;
import com.razza.bookingsystem.domain.Venue;
import com.razza.bookingsystem.domain.User;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserAlreadyExistsException;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.VenueRepository;
import com.razza.bookingsystem.repository.UserRepository;
import com.razza.bookingsystem.security.CustomUserDetails;
import com.razza.bookingsystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 *
 * Uses Mockito to isolate the service from its dependencies:
 * {@link UserRepository}, {@link UserMapper}, {@link PasswordEncoder},
 * {@link JwtService}, {@link AuthenticationManager},
 * {@link CustomUserDetailsService}, and {@link VenueRepository}.
 *
 * Test groups:
 * signup - registration, validation, venue scoping, and password encoding
 * login  - authentication, token generation, and credential handling
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private VenueRepository venueRepository;

    @InjectMocks
    private AuthService authService;

    private Venue venue;
    private User user;
    private UserDto userDto;

    /**
     * Sets up shared fixtures used across multiple test cases.
     * Creates a venue, a user belonging to that venue, and the
     * corresponding UserDto.
     */
    @BeforeEach
    void setUp() {
        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("acme")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("hashedPassword")
                .role(Role.GUEST)
                .venue(venue)
                .build();

        userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(Role.GUEST)
                .build();
    }


    /**
     * Verifies that a valid signup request returns a non-null DTO with the
     * correct email and role.
     */
    @Transactional
    @Test
    void signup_success_returnsUserDto() {
        when(venueRepository.findByName("acme")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue("user@example.com", venue)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "acme", null, null);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getRole()).isEqualTo(Role.GUEST);
    }

    /**
     * Verifies that the plain-text password is encoded before the user is
     * persisted. Uses an ArgumentCaptor to inspect the User object actually
     * passed to the repository save call.
     */
    @Transactional
    @Test
    void signup_encodesPasswordBeforeSaving() {
        when(venueRepository.findByName("acme")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue(any(), any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "acme", null, null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class); // user class is specified two times since the second one is for runtime
        verify(userRepository).save(captor.capture());                     // here it's verified that when the user is saved into repository and DB, the password is already hashed, using the result of stubbing eventRepository.save(event) would not work since the result of that is the initialized user, while what we want is the actual object passed to save()
        assertThat(captor.getValue().getPassword()).isEqualTo("hashedPassword");
    }

    /**
     * Verifies that the service always assigns {@link Role#GUEST} to a newly
     * registered user, regardless of any external input. Uses an
     * ArgumentCaptor to inspect the User object passed to the repository.
     */
    @Transactional
    @Test
    void signup_assignsUserRoleAutomatically() {
        when(venueRepository.findByName("acme")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue(any(), any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "acme", null, null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.GUEST);
    }

    /**
     * Verifies that the resolved venue is assigned to the user before
     * persisting. Uses an ArgumentCaptor to inspect the User object passed
     * to the repository.
     */
    @Transactional
    @Test
    void signup_assignsVenueToUser() {
        when(venueRepository.findByName("acme")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue(any(), any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "acme", null, null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getVenue()).isEqualTo(venue);
    }

    /**
     * Verifies that a {@link UserAlreadyExistsException} is thrown — and no
     * save is attempted — when the email address is already registered within
     * the same venue. The exception message must contain the duplicate email.
     */
    @Transactional
    @Test
    void signup_throwsUserAlreadyExistsException_whenEmailAlreadyRegisteredInVenue() {
        when(venueRepository.findByName("acme")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue("user@example.com", venue))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "acme", null, null))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("user@example.com");

        verify(userRepository, never()).save(any());
    }

    /**
     * Verifies that a {@link ResourceNotFoundException} is thrown — and no
     * save is attempted — when the supplied venue name does not match any
     * existing venue. The exception message must reference "venue".
     */
    @Transactional
    @Test
    void signup_throwsResourceNotFoundException_whenVenueDoesNotExist() {
        when(venueRepository.findByName("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "acme", null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("venue");

        verify(userRepository, never()).save(any());
    }

    /**
     * Verifies that the same email address can be registered in two different
     * venues without triggering a {@link UserAlreadyExistsException}.
     * Email uniqueness is scoped per venue, not globally.
     */
    @Transactional
    @Test
    void signup_sameEmail_differentVenue_doesNotThrow() {
        Venue otherVenue = Venue.builder().id(UUID.randomUUID()).name("other").build();
        when(venueRepository.findByName("other")).thenReturn(Optional.of(otherVenue));
        when(userRepository.findByEmailAndVenue("user@example.com", otherVenue))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        User otherUser = User.builder()
                .id(UUID.randomUUID()).email("user@example.com")
                .password("hashedPassword").role(Role.GUEST).venue(otherVenue).build();
        UserDto otherDto = UserDto.builder()
                .id(otherUser.getId()).email("user@example.com").role(Role.GUEST).build();
        when(userRepository.save(any())).thenReturn(otherUser);
        when(userMapper.toDto(otherUser)).thenReturn(otherDto);

        UserDto result = authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "other", null, null);

        assertThat(result.getEmail()).isEqualTo("user@example.com");
    }

    /**
     * Verifies that a successful authentication returns the JWT token
     * produced by {@link JwtService}.
     */
    @Transactional
    @Test
    void login_success_returnsJwtToken() {
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(), user.getName(), user.getBirthDate(), user.getEmail(), user.getPassword(),
                venue, user.getAvailability(),
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-value");

        String token = authService.login("user@example.com", "plainPassword", "acme");

        assertThat(token).isEqualTo("jwt-token-value");
    }

    /**
     * Verifies that the service builds a venue-scoped username in the format
     * "email|venueName" when constructing the authentication token passed to
     * the {@link AuthenticationManager}. This ensures credentials are always
     * resolved within the correct venue context.
     */
    @Transactional
    @Test
    void login_buildsVenueScopedUsernameForAuthentication() {
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(), user.getName(), user.getBirthDate(), user.getEmail(), user.getPassword(),
                venue, user.getAvailability(),
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        when(authenticationManager.authenticate(captor.capture())).thenReturn(auth);
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.login("user@example.com", "plainPassword", "acme");

        assertThat(captor.getValue().getPrincipal()).isEqualTo("user@example.com|acme");
        assertThat(captor.getValue().getCredentials()).isEqualTo("plainPassword");
    }

    /**
     * Verifies that a {@link BadCredentialsException} propagates to the caller
     * when the {@link AuthenticationManager} rejects the credentials, and that
     * token generation is never attempted in that case.
     */
    @Transactional
    @Test
    void login_throwsBadCredentialsException_whenPasswordIsWrong() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login("user@example.com", "wrongPassword", "acme"))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    /**
     * Verifies that token generation is delegated to {@link JwtService} with
     * the {@link CustomUserDetails} principal extracted from the authenticated
     * token, and that the returned token value is passed through to the caller.
     */
    @Transactional
    @Test
    void login_delegatesTokenGenerationToJwtService() {
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(), user.getName(), user.getBirthDate(), user.getEmail(), user.getPassword(),
                venue, user.getAvailability(),
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(userDetails)).thenReturn("generated-token");

        String token = authService.login("user@example.com", "plainPassword", "acme");

        verify(jwtService).generateToken(userDetails);
        assertThat(token).isEqualTo("generated-token");
    }
}