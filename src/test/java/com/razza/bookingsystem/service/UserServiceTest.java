package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.AvailabilityDto;
import com.razza.bookingsystem.dto.UserDto;
import com.razza.bookingsystem.exception.InvalidRoleException;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.exception.UserDeleteException;
import com.razza.bookingsystem.mapper.UserMapper;
import com.razza.bookingsystem.repository.BookingRepository;
import com.razza.bookingsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Nested;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 *
 * Covers makeAdmin, deleteUser, getAvailablePerformersByDate, getPerformer and
 * modifyAvailability, verifying correct behavior for happy paths, venue scoping
 * enforcement, and expected exception cases.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private Venue venue;
    private User guest, performer, secondPerformer;
    private UUID userId, performerId;
    private UserDto performerDto, secondPerformerDto;

    /**
     * Sets up a default venue, a guest and two performers with their dtos before
     * each test. The guest starts with the GUEST role, while the performers have
     * the PERFORMER role and different weekly availability.
     */
    @BeforeEach
    void setUp() {
        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("music bar")
                .build();

        userId = UUID.randomUUID();
        performerId = UUID.randomUUID();

        guest = User.builder()
                .id(userId)
                .name("first guest")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00.000+02:00"))
                .email("guest@example.com")
                .password("hashedPassword")
                .role(Role.GUEST)
                .venue(venue)
                .status(Status.CONFIRMED)
                .performances(null)
                .availability(null)
                .build();

        performer = User.builder()
                .id(performerId)
                .name("first performer")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00.000+02:00"))
                .email("performer@example.com")
                .password("hashedPassword")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .performances(null)
                .availability(new Availability(true,true,true,true,false,false,false))
                .build();

        performerDto = UserDto.builder()
                .id(performerId)
                .name("first performer")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00.000+02:00"))
                .email("performer@example.com")
                .role(Role.PERFORMER)
                .venue(venue)
                .availability(new AvailabilityDto(true,true,true,true,false,false,false))
                .performances(null)
                .build();

        secondPerformer = User.builder()
                .id(performerId)
                .name("second performer")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00.000+02:00"))
                .email("secondPerformer@example.com")
                .password("hashedPassword")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .performances(null)
                .availability(new Availability(false,false,false,false,true,true,true))
                .build();

        secondPerformerDto = UserDto.builder()
                .id(performerId)
                .name("second performer")
                .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00.000+02:00"))
                .email("secondPerformer@example.com")
                .role(Role.PERFORMER)
                .venue(venue)
                .availability(new AvailabilityDto(false,false,false,false,true,true,true))
                .performances(null)
                .build();


    }

    @Nested
    @DisplayName("makeAdmin")
    class MakeAdmin {
        /**
         * Given a valid user in the venue, makeAdmin should return a UserDto
         * reflecting the updated ADMIN role and the original email address.
         */
        @Transactional
        @Test
        void makeAdmin_success_returnsUpdatedUserDto() {
            UserDto adminDto = UserDto.builder()
                    .id(userId)
                    .name("first guest")
                    .birthDate(OffsetDateTime.parse("2000-01-01T00:00:00.000+02:00"))
                    .email("guest@example.com")
                    .role(Role.ADMIN)
                    .venue(venue)
                    .availability(null)
                    .performances(null)
                    .build();

            when(userRepository.findByIdAndVenueAndRole(userId, venue, Role.GUEST))
                    .thenReturn(Optional.of(guest));
            when(userRepository.save(guest)).thenReturn(guest);
            when(userMapper.toDto(guest)).thenReturn(adminDto);

            UserDto result = userService.makeAdmin(userId, venue);

            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo(Role.ADMIN);
            assertThat(result.getEmail()).isEqualTo("guest@example.com");
        }

        /**
         * Given a valid performer in the venue, makeAdmin should throw a
         * ResourceNotFoundException given that performers can't be made admins.
         */
        @Transactional
        @Test
        void makeAdmin_throwsResourceNotFoundException_whenUserIsPerformer() {

            when(userRepository.findByIdAndVenueAndRole(performerId, venue, Role.GUEST))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.makeAdmin(performerId, venue))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(performerId.toString());
        }


        /**
         * makeAdmin should throw ResourceNotFoundException when no user with the
         * given ID exists within the venue, and must not attempt to save anything.
         */
        @Transactional
        @Test
        void makeAdmin_throwsResourceNotFoundException_whenUserNotFound() {

            UUID otherUserId = UUID.randomUUID();

            when(userRepository.findByIdAndVenueAndRole(otherUserId, venue, Role.GUEST))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.makeAdmin(otherUserId, venue))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(otherUserId.toString());
        }


        /**
         * makeAdmin should throw ResourceNotFoundException when the user exists but
         * belongs to a different venue, enforcing venue isolation.
         */
        @Transactional
        @Test
        void makeAdmin_userInDifferentVenue_throwsResourceNotFoundException() {
            Venue otherVenue = Venue.builder().id(UUID.randomUUID()).name("other").build();
            when(userRepository.findByIdAndVenueAndRole(userId, otherVenue, Role.GUEST))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.makeAdmin(userId, otherVenue))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {
        /**
         * deleteUser should successfully soft delete a user who has no confirmed bookings.
         */
        @Transactional
        @Test
        void deleteUser_success_deletesUserWithNoActiveBookings() {
            when(userRepository.findByIdAndVenueId(userId, venue.getId()))
                    .thenReturn(Optional.of(guest));
            when(bookingRepository.countByUserIdAndStatus(userId, Status.CONFIRMED))
                    .thenReturn(0);

            userService.deleteUser(userId, venue);

            assertEquals(Status.CANCELLED, guest.getStatus());
        }

        /**
         * deleteUser should throw ResourceNotFoundException when no user with the
         * given ID exists within the venue, and must not attempt to delete anything.
         */
        @Transactional
        @Test
        void deleteUser_throwsResourceNotFoundException_whenUserNotFoundInVenue() {
            UUID otherUserId = UUID.randomUUID();
            when(userRepository.findByIdAndVenueId(otherUserId, venue.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(otherUserId, venue))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        /**
         * deleteUser should throw UserDeleteException when the user has one or more
         * confirmed bookings, and the exception message should include the booking count.
         */
        @Transactional
        @Test
        void deleteUser_throwsUserDeleteException_whenUserHasConfirmedBookings() {
            when(userRepository.findByIdAndVenueId(userId, venue.getId()))
                    .thenReturn(Optional.of(guest));
            when(bookingRepository.countByUserIdAndStatus(userId, Status.CONFIRMED))
                    .thenReturn(3);

            assertThatThrownBy(() -> userService.deleteUser(userId, venue))
                    .isInstanceOf(UserDeleteException.class)
                    .hasMessageContaining("3");
        }


        /**
         * deleteUser should only check for CONFIRMED bookings; canceled or other
         * statuses must not influence the deletion.
         */
        @Transactional
        @Test
        void deleteUser_checksOnlyConfirmedBookings() {
            when(userRepository.findByIdAndVenueId(userId, venue.getId()))
                    .thenReturn(Optional.of(guest));
            when(bookingRepository.countByUserIdAndStatus(userId, Status.CONFIRMED))
                    .thenReturn(0);

            userService.deleteUser(userId, venue);

            verify(bookingRepository).countByUserIdAndStatus(userId, Status.CONFIRMED);
            verify(bookingRepository, never()).countByUserIdAndStatus(userId, Status.CANCELLED);
        }

        /**
         * deleteUser should throw ResourceNotFoundException when the user exists but
         * belongs to a different venue, enforcing venue isolation.
         */
        @Transactional
        @Test
        void deleteUser_userInDifferentVenue_throwsResourceNotFoundException() {
            Venue otherVenue = Venue.builder().id(UUID.randomUUID()).name("other").build();
            when(userRepository.findByIdAndVenueId(userId, otherVenue.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(userId, otherVenue))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAvailablePerformersByDate")
    class GetAvailablePerformersByDate {

        /**
         * Given a date falling on a day when the performer is available,
         * getAvailablePerformersByDate should return a list with the UserDto of
         * that performer, belonging to the requested venue.
         */
        @Transactional
        @Test
        void getAvailablePerformersByDate_success_returnsPerformerDto() {

            when(userRepository.findByRoleAndVenueId(Role.PERFORMER, venue.getId()))
                    .thenReturn(List.of(performer));
            when(userMapper.toDto(performer)).thenReturn(performerDto);

            List<UserDto> result = userService.getAvailablePerformersByDate(LocalDate.of(2027,07,22), venue.getId());

            assertEquals(1, result.size());
            assertEquals(result.getFirst().getId(), performer.getId());
            assertEquals(Role.PERFORMER, result.getFirst().getRole());
            assertEquals(result.getFirst().getVenue().getId(), venue.getId());
        }

        /**
         * When the date is null, getAvailablePerformersByDate should skip the
         * availability filter and return all the performers of the venue.
         */
        @Transactional
        @Test
        void getAvailablePerformersByDate_success_returnsAllPerformersIfDateNull() {

            when(userRepository.findByRoleAndVenueId(Role.PERFORMER, venue.getId()))
                    .thenReturn(List.of(performer, secondPerformer));
            when(userMapper.toDto(performer)).thenReturn(performerDto);
            when(userMapper.toDto(secondPerformer)).thenReturn(performerDto);

            List<UserDto> result = userService.getAvailablePerformersByDate(null, venue.getId());

            assertEquals(2, result.size());
            assertEquals(result.get(0).getId(), performer.getId());
            assertEquals(result.get(1).getId(), secondPerformer.getId());
        }
    }

    @Nested
    @DisplayName("getPerformer")
    class GetPerformer {

        /**
         * Given a valid performer in the venue, getPerformer should return a
         * UserDto with the ID and the PERFORMER role of that performer.
         */
        @Transactional
        @Test
        void getPerformer_success_returnsPerformer() {

            when(userRepository.findByIdAndVenueAndRole(performer.getId(), venue, Role.PERFORMER))
                    .thenReturn(Optional.of(performer));
            when(userMapper.toDto(performer)).thenReturn(performerDto);

            UserDto result = userService.getPerformer(performer.getId(), venue, Role.PERFORMER);

            assertEquals(performer.getId(), result.getId());
            assertEquals(Role.PERFORMER, result.getRole());
        }

        /**
         * getPerformer should throw ResourceNotFoundException when no user with the
         * given ID and role exists within the venue.
         */
        @Transactional
        @Test
        void getPerformer_throwsResourceNotFoundException_whenUserNotFoundInVenue() {

            when(userRepository.findByIdAndVenueAndRole(performer.getId(), venue, Role.PERFORMER))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getPerformer(performer.getId(), venue, Role.PERFORMER))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("modifyAvailability")
    class ModifyAvailability {

        /**
         * Given a valid performer in the venue, modifyAvailability should update
         * the availability of the performer so that it reflects the requested day.
         */
        @Transactional
        @Test
        void modifyAvailability_success_modifiesPerformerAvailability() {

            when(userRepository.findByEmailAndVenue("performer@example.com", venue))
                    .thenReturn(Optional.of(performer));

            when(userRepository.save(performer)).thenReturn(performer);

            userService.modifyAvailability("monday", "performer@example.com", venue);

            assertEquals(true, performer.getAvailability().getMonday());
            assertEquals(false, performer.getAvailability().getTuesday());
            assertEquals(false, performer.getAvailability().getSaturday());
        }

        /**
         * modifyAvailability should throw ResourceNotFoundException when no user
         * with the given email exists within the venue, enforcing venue isolation.
         */
        @Transactional
        @Test
        void modifyAvailability_throwsResourceNotFoundException_whenUserNotFoundInVenue() {

            Venue otherVenue = Venue.builder()
                    .id(UUID.randomUUID())
                    .name("otherVenue")
                    .build();

            when(userRepository.findByEmailAndVenue("performer@example.com", otherVenue))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.modifyAvailability("monday", "performer@example.com", otherVenue))
                    .isInstanceOf(ResourceNotFoundException.class);

        }

        /**
         * modifyAvailability should throw InvalidRoleException when the user exists
         * but is not a performer, since only performers have an availability.
         */
        @Transactional
        @Test
        void modifyAvailability_throwsInvalidRoleException_whenUserNotAPerformer() {

            when(userRepository.findByEmailAndVenue("guest@example.com", venue))
                    .thenReturn(Optional.of(guest));



            assertThatThrownBy(() -> userService.modifyAvailability("monday", "guest@example.com", venue))
                    .isInstanceOf(InvalidRoleException.class)
                    .hasMessageContaining("Wrong role: it must be performer");
        }

    }
}