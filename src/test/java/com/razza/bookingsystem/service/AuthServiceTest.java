package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.AvailabilityDto;
import com.razza.bookingsystem.dto.PerformanceDto;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserAlreadyExistsException;
import com.razza.bookingsystem.mapper.AvailabilityMapper;
import com.razza.bookingsystem.mapper.PerformanceMapper;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LOCAL_DATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 *
 * Setup:
 * No Spring context is started. Mockito isolates the service from its dependencies:
 * {@link UserRepository}, {@link VenueRepository}, {@link UserMapper},
 * {@link PerformanceMapper}, {@link AvailabilityMapper}, {@link PasswordEncoder},
 * {@link JwtService} and {@link AuthenticationManager}.
 * A venue, a guest user and a performer with their DTOs are created before each test.
 *
 * Scenarios:
 * Signup is called for a guest, for a performer, with an email already registered in the venue,
 * with a venue that does not exist, and with the same email in two different venues.
 * Login is called with valid credentials and for a user whose status is CANCELLED.
 *
 * Expected outcome:
 * A valid signup returns the user DTO, encodes the password and assigns the requested role
 * and the resolved venue before the user is persisted.
 * A performer signup also builds the availability and the performances.
 * A duplicate email in the same venue or an unknown venue throws an exception and nothing is saved.
 * Email uniqueness is scoped per venue, not globally.
 * A valid login returns the JWT produced by the JwtService.
 * A login for a cancelled user throws BadCredentialsException and no token is generated.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PerformanceMapper performanceMapper;
    @Mock private AvailabilityMapper availabilityMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private VenueRepository venueRepository;


    @InjectMocks
    private AuthService authService;

    private Venue venue;
    private User user, performer;
    private UserDto userDto, performerDto;
    private PerformanceDto performanceDto;
    private Performance performance;
    private Availability availability;
    private AvailabilityDto availabilityDto;

    /**
     * Sets up shared fixtures used across multiple test cases.
     * Creates a venue, a guest user belonging to that venue and the corresponding UserDto.
     * Also creates a performance, a performer with the corresponding UserDto,
     * and an availability.
     */
    @BeforeEach
    void setUp() {
        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("myVenue")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .name("user")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .email("user@example.com")
                .password("hashedPassword")
                .role(Role.GUEST)
                .venue(venue)
                .status(Status.CONFIRMED)
                .performances(null)
                .availability(null)
                .build();

        userDto = UserDto.builder()
                .id(user.getId())
                .name("user")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .email(user.getEmail())
                .role(Role.GUEST)
                .venue(venue)
                .availability(null)
                .performances(null)
                .build();

        performance = Performance.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("blues")
                .description("blues session")
                .duration(120)
                .performanceType(PerformanceType.LIVE_MUSIC)
                .build();

        performanceDto = PerformanceDto.builder()
                .name("blues")
                .description("blues session")
                .duration(120)
                .performanceType(PerformanceType.LIVE_MUSIC)
                .build();

        performer = User.builder()
                .id(UUID.randomUUID())
                .name("performer")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .email("performer@example.com")
                .password("hashedPassword")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .performances(Set.of(performance))
                .availability(availability)
                .build();

        performerDto = UserDto.builder()
                .id(performer.getId())
                .name("performer")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .email(performer.getEmail())
                .role(Role.PERFORMER)
                .venue(venue)
                .availability(availabilityDto)
                .performances(Set.of(performanceDto))
                .build();

        availability = Availability.builder()
                .id(UUID.randomUUID())
                .user(user)
                .monday(true)
                .tuesday(true)
                .wednesday(false)
                .thursday(false)
                .friday(false)
                .saturday(false)
                .sunday(false)
                .build();
    }


    /**
     * Verifies that a valid signup request by a guest returns a DTO that is not null,
     * with the correct email and role.
     * Also verifies that the plain text password is encoded, the assigned role is GUEST,
     * the assigned venue is the resolved one, all before the user is persisted.
     * Uses an ArgumentCaptor to inspect the User object actually passed to the repository save call.
     */
    @Transactional
    @Test
    void signup_success_returnsUserDto() {
        when(venueRepository.findByName("myVenue")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue("user@example.com", venue)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "myVenue", null, null);

        assertThat(result).isNotNull();                                     // these assertions are made anyways to prove that the right data flows out and that the mapping works correctly
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getRole()).isEqualTo(Role.GUEST);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class); // user class is specified two times since the second one is for runtime
        verify(userRepository).save(captor.capture());                     // here it's verified that when the user is saved into repository and DB, the password is already hashed, using the result of stubbing eventRepository.save(event) would not work since the result of that is the initialized user, exactly the same user that has been initialized inside setUp(), while what we want is the actual object passed to save(), the one built by the service
        assertThat(captor.getValue().getPassword()).isEqualTo("hashedPassword");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.GUEST);
        assertThat(captor.getValue().getVenue()).isEqualTo(venue);
    }

    /**
     * Verifies that a valid signup request by a performer returns a DTO that is not null,
     * with the correct email and role.
     * Also verifies that the plain text password is encoded, the assigned role is PERFORMER,
     * the availability matches the requested days and the performances are attached to the user,
     * all before the user is persisted.
     * Uses an ArgumentCaptor to inspect the User object actually passed to the repository save call.
     */
    @Transactional
    @Test
    void signup_success_returnsPerformerDto() {
        when(venueRepository.findByName("myVenue")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue("performer@example.com", venue)).thenReturn(Optional.empty());
        when(performanceMapper.toEntity(performanceDto)).thenReturn(performance);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(availabilityMapper.toEntity(any())).thenReturn(availability);
        when(userRepository.save(any(User.class))).thenReturn(performer);
        when(userMapper.toDto(performer)).thenReturn(performerDto);

        UserDto result = authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"),
                "performer@example.com", "plainPassword", Role.PERFORMER, "myVenue","monday, tuesday", Set.of(performanceDto)
        );

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("performer@example.com");
        assertThat(result.getRole()).isEqualTo(Role.PERFORMER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashedPassword");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.PERFORMER);
        assertThat(Objects.requireNonNull(captor.getValue().getAvailability()).getMonday()).isTrue();
        assertThat(captor.getValue().getAvailability().getWednesday()).isFalse();
        assertThat(captor.getValue().getPerformances()).hasSize(1);
        assertThat(captor.getValue().getPerformances().iterator().next().getName().equals("blues"));
    }

    /**
     * Verifies that a {@link UserAlreadyExistsException} is thrown, and no
     * save is attempted, when the email address is already registered within
     * the same venue. The exception message must contain the duplicate email.
     */
    @Transactional
    @Test
    void signup_throwsUserAlreadyExistsException_whenEmailAlreadyRegisteredInVenue() {
        when(venueRepository.findByName("myVenue")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue("user@example.com", venue))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "myVenue", null, null))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("user@example.com");

        verify(userRepository, never()).save(any());
    }

    /**
     * Verifies that a {@link ResourceNotFoundException} is thrown, and no
     * save is attempted, when the supplied venue name does not match any
     * existing venue. The exception message must reference "venue".
     */
    @Transactional
    @Test
    void signup_throwsResourceNotFoundException_whenVenueDoesNotExist() {
        when(venueRepository.findByName("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "unknown", null, null))
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
        when(venueRepository.findByName("myVenue")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue("user@example.com", venue)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);
        authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "myVenue", null, null);

        Venue otherVenue = Venue.builder().id(UUID.randomUUID()).name("other").build();
        when(venueRepository.findByName("other")).thenReturn(Optional.of(otherVenue));
        when(userRepository.findByEmailAndVenue("user@example.com", otherVenue))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        User otherUser = User.builder()
                .id(UUID.randomUUID()).name("otherUser").email("user@example.com").birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .password("hashedPassword").role(Role.GUEST).venue(otherVenue).status(Status.CONFIRMED).performances(null).availability(null).build();
        UserDto otherDto = UserDto.builder()
                .id(UUID.randomUUID()).name("otherUser").email("user@example.com").birthDate(OffsetDateTime.parse("2000-01-01T00:00:00Z"))
                .role(Role.GUEST).venue(otherVenue).availability(null).performances(null).build();
        when(userRepository.save(any())).thenReturn(otherUser);
        when(userMapper.toDto(otherUser)).thenReturn(otherDto);

        authService.signup("user", OffsetDateTime.parse("2000-01-01T12:00:00+02:00"), "user@example.com", "plainPassword", Role.GUEST, "myVenue", null, null);
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

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        Authentication auth = mock(Authentication.class); //inline mock instead of field level one because Authentication object is only needed in this test

        when(venueRepository.findByName("myVenue")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue("user@example.com",venue)).thenReturn(Optional.of(user));
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(captor.capture()))
                .thenReturn(auth);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-value");

        String token = authService.login("user@example.com", "plainPassword", "myVenue");

        assertThat(token).isEqualTo("jwt-token-value");
        assertThat(captor.getValue().getPrincipal()).isEqualTo("user@example.com|myVenue");
        assertThat(captor.getValue().getCredentials()).isEqualTo("plainPassword");
    }

    /**
     * Verifies that a user whose status is CANCELLED cannot log in.
     * A {@link BadCredentialsException} with the message "invalid credentials" is thrown,
     * and no JWT is generated.
     */
    @Transactional
    @Test
    void login_fails_cancelled_user() {
        user.setStatus(Status.CANCELLED);
        when(venueRepository.findByName("myVenue")).thenReturn(Optional.of(venue));
        when(userRepository.findByEmailAndVenue("user@example.com",venue)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.login("user@example.com","wrongPassword", venue.getName()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("invalid credentials");
        verify(jwtService, never()).generateToken(any());
    }
}