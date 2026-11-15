package com.razza.bookingsystem.service;

import com.razza.bookingsystem.domain.*;
import com.razza.bookingsystem.dto.PerformanceDto;
import com.razza.bookingsystem.dto.ScheduledPerformanceDto;
import com.razza.bookingsystem.exception.ResourceNotFoundException;
import com.razza.bookingsystem.mapper.PerformanceMapper;
import com.razza.bookingsystem.repository.EventRepository;
import com.razza.bookingsystem.repository.PerformanceRepository;
import com.razza.bookingsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PerformanceService}.
 *
 * Covers getPerformances, addPerformance, deletePerformance and modifyPerformance,
 * verifying correct behavior for the admin and performer paths, venue-scoping
 * enforcement, event filtering and expected exception cases.
 */
@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private PerformanceRepository performanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private PerformanceMapper performanceMapper;

    @InjectMocks
    private PerformanceService performanceService;

    private Venue venue;
    private User performer, secondPerformer;
    private UUID adminId, performerId, secondPerformerId, performanceId;
    private Performance performance;

    /**
     * Sets up a default venue, two performers and one performance before each test.
     * The timezone field is injected manually, since it is normally filled by Spring
     * from the app.timezone property.
     */
    @BeforeEach
    void setUp() {
        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("music bar")
                .build();

        adminId = UUID.randomUUID();
        performerId = UUID.randomUUID();
        secondPerformerId = UUID.randomUUID();
        performanceId = UUID.randomUUID();

        performer = User.builder()
                .id(performerId)
                .name("first performer")
                .email("performer@example.com")
                .password("hashedPassword")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .build();

        secondPerformer = User.builder()
                .id(secondPerformerId)
                .name("second performer")
                .email("secondPerformer@example.com")
                .password("hashedPassword")
                .role(Role.PERFORMER)
                .venue(venue)
                .status(Status.CONFIRMED)
                .build();

        performance = Performance.builder()
                .id(performanceId)
                .user(performer)
                .name("stand up")
                .description("a funny show")
                .duration(30)
                .performanceType(PerformanceType.COMEDY)
                .build();

        ReflectionTestUtils.setField(performanceService, "timezone", "UTC");
    }

    /**
     * Builds a time slot of 30 minutes assigned to the given user, referencing
     * the default performance.
     */
    private TimeSlot slotFor(User user, OffsetDateTime start) {
        return TimeSlot.builder()
                .userEmail(user.getEmail())
                .startTime(start)
                .endTime(start.plusMinutes(30))
                .performanceId(performanceId)
                .build();
    }

    /**
     * Builds an event in the default venue with the given date, status and schedule.
     */
    private Event eventAt(OffsetDateTime date, Status status, List<TimeSlot> schedule) {
        return Event.builder()
                .id(UUID.randomUUID())
                .name("open mic")
                .location("main hall")
                .date(date)
                .venue(venue)
                .status(status)
                .schedule(schedule)
                .build();
    }

    @Nested
    @DisplayName("getPerformances")
    class GetPerformances {

        /**
         * An admin asking for the performances of a performer in the same venue should
         * get the scheduled performance with the data of the performance and the
         * location of the event.
         */
        @Transactional
        @Test
        void getPerformances_admin_returnsPerformancesOfRequestedUser() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2);
            Event event = eventAt(start, Status.CONFIRMED, List.of(slotFor(performer, start)));

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.of(performer));
            when(eventRepository.findByVenue(venue)).thenReturn(List.of(event));
            when(userRepository.findByEmailAndVenue(performer.getEmail(), venue))
                    .thenReturn(Optional.of(performer));
            when(performanceRepository.findById(performanceId)).thenReturn(Optional.of(performance));

            List<ScheduledPerformanceDto> result = performanceService.getPerformances(performerId, adminId, true, venue);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("stand up");
            assertThat(result.get(0).getDescription()).isEqualTo("a funny show");
            assertThat(result.get(0).getDuration()).isEqualTo(30);
            assertThat(result.get(0).getPerformanceType()).isEqualTo(PerformanceType.COMEDY);
            assertThat(result.get(0).getLocation()).isEqualTo("main hall");
            assertThat(result.get(0).getStartTime().toInstant()).isEqualTo(start.toInstant());
        }

        /**
         * A performer should always get his own performances, the userId passed as
         * parameter must be ignored and the admin lookup must not be used.
         */
        @Transactional
        @Test
        void getPerformances_performer_usesCurrentUserIdAndIgnoresRequestedUserId() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2);
            Event event = eventAt(start, Status.CONFIRMED, List.of(slotFor(performer, start)));

            when(userRepository.findByIdAndVenueAndRole(performerId, venue, Role.PERFORMER))
                    .thenReturn(Optional.of(performer));
            when(eventRepository.findByVenue(venue)).thenReturn(List.of(event));
            when(userRepository.findByEmailAndVenue(performer.getEmail(), venue))
                    .thenReturn(Optional.of(performer));
            when(performanceRepository.findById(performanceId)).thenReturn(Optional.of(performance));

            List<ScheduledPerformanceDto> result = performanceService.getPerformances(secondPerformerId, performerId, false, venue);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("stand up");
            verify(userRepository, never()).findByIdAndVenueId(any(), any());
        }

        /**
         * The start time of the slot should be converted to the configured timezone,
         * the instant must stay the same while the offset changes.
         */
        @Transactional
        @Test
        void getPerformances_startTime_isConvertedToConfiguredTimezone() {
            ReflectionTestUtils.setField(performanceService, "timezone", "Europe/Budapest");
            OffsetDateTime start = OffsetDateTime.parse("2099-06-15T10:00:00Z");
            Event event = eventAt(start, Status.CONFIRMED, List.of(slotFor(performer, start)));

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.of(performer));
            when(eventRepository.findByVenue(venue)).thenReturn(List.of(event));
            when(userRepository.findByEmailAndVenue(performer.getEmail(), venue))
                    .thenReturn(Optional.of(performer));
            when(performanceRepository.findById(performanceId)).thenReturn(Optional.of(performance));

            List<ScheduledPerformanceDto> result = performanceService.getPerformances(performerId, adminId, true, venue);

            assertThat(result.get(0).getStartTime().getOffset()).isEqualTo(ZoneOffset.ofHours(2));
            assertThat(result.get(0).getStartTime().toInstant()).isEqualTo(start.toInstant());
        }

        /**
         * Events in the past must not be returned, and the performance repository
         * must not even be queried for them.
         */
        @Transactional
        @Test
        void getPerformances_pastEvent_isExcluded() {
            OffsetDateTime start = OffsetDateTime.now().minusDays(2);
            Event pastEvent = eventAt(start, Status.CONFIRMED, List.of(slotFor(performer, start)));

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.of(performer));
            when(eventRepository.findByVenue(venue)).thenReturn(List.of(pastEvent));

            List<ScheduledPerformanceDto> result = performanceService.getPerformances(performerId, adminId, true, venue);

            assertThat(result).isEmpty();
            verifyNoInteractions(performanceRepository);
        }

        /**
         * Cancelled events must not be returned, even if they are in the future.
         */
        @Transactional
        @Test
        void getPerformances_cancelledEvent_isExcluded() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2);
            Event cancelledEvent = eventAt(start, Status.CANCELLED, List.of(slotFor(performer, start)));

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.of(performer));
            when(eventRepository.findByVenue(venue)).thenReturn(List.of(cancelledEvent));

            List<ScheduledPerformanceDto> result = performanceService.getPerformances(performerId, adminId, true, venue);

            assertThat(result).isEmpty();
            verifyNoInteractions(performanceRepository);
        }

        /**
         * An event where all the slots belong to another performer must not be returned.
         */
        @Transactional
        @Test
        void getPerformances_eventOfAnotherPerformer_isExcluded() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2);
            Event event = eventAt(start, Status.CONFIRMED, List.of(slotFor(secondPerformer, start)));

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.of(performer));
            when(eventRepository.findByVenue(venue)).thenReturn(List.of(event));
            when(userRepository.findByEmailAndVenue(secondPerformer.getEmail(), venue))
                    .thenReturn(Optional.of(secondPerformer));

            List<ScheduledPerformanceDto> result = performanceService.getPerformances(performerId, adminId, true, venue);

            assertThat(result).isEmpty();
            verifyNoInteractions(performanceRepository);
        }

        /**
         * When an event has slots of two performers, only the slot of the requested
         * performer should be returned.
         */
        @Transactional
        @Test
        void getPerformances_eventWithTwoPerformers_returnsOnlyRequestedPerformerSlot() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2);
            Event event = eventAt(start, Status.CONFIRMED,
                    List.of(slotFor(performer, start), slotFor(secondPerformer, start.plusMinutes(30))));

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.of(performer));
            when(eventRepository.findByVenue(venue)).thenReturn(List.of(event));
            when(userRepository.findByEmailAndVenue(performer.getEmail(), venue))
                    .thenReturn(Optional.of(performer));
            when(performanceRepository.findById(performanceId)).thenReturn(Optional.of(performance));

            List<ScheduledPerformanceDto> result = performanceService.getPerformances(performerId, adminId, true, venue);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStartTime().toInstant()).isEqualTo(start.toInstant());
            verify(performanceRepository, times(1)).findById(performanceId);
        }

        /**
         * If the performance referenced by the slot does not exist anymore, the slot
         * is skipped and no exception is thrown.
         */
        @Transactional
        @Test
        void getPerformances_performanceNotFound_isSkipped() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2);
            Event event = eventAt(start, Status.CONFIRMED, List.of(slotFor(performer, start)));

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.of(performer));
            when(eventRepository.findByVenue(venue)).thenReturn(List.of(event));
            when(userRepository.findByEmailAndVenue(performer.getEmail(), venue))
                    .thenReturn(Optional.of(performer));
            when(performanceRepository.findById(performanceId)).thenReturn(Optional.empty());

            List<ScheduledPerformanceDto> result = performanceService.getPerformances(performerId, adminId, true, venue);

            assertThat(result).isEmpty();
        }

        /**
         * An admin asking for a user that does not exist in the venue should get a
         * ResourceNotFoundException, and no event must be loaded.
         */
        @Transactional
        @Test
        void getPerformances_admin_throwsResourceNotFoundException_whenUserNotFound() {
            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> performanceService.getPerformances(performerId, adminId, true, venue))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(performerId.toString());

            verifyNoInteractions(eventRepository);
        }

        /**
         * A user without the PERFORMER role in the venue should get a
         * ResourceNotFoundException, and no event must be loaded.
         */
        @Transactional
        @Test
        void getPerformances_performer_throwsResourceNotFoundException_whenUserNotFound() {
            when(userRepository.findByIdAndVenueAndRole(performerId, venue, Role.PERFORMER))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> performanceService.getPerformances(null, performerId, false, venue))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(eventRepository);
        }
    }

    @Nested
    @DisplayName("addPerformance")
    class AddPerformance {

        /**
         * An admin adding a performance for a performer of the same venue should save
         * a performance with the data of the dto, owned by the requested performer.
         */
        @Transactional
        @Test
        void addPerformance_admin_savesPerformanceForRequestedUser() {
            PerformanceDto dto = PerformanceDto.builder()
                    .name("poetry night")
                    .description("original poems")
                    .duration(20)
                    .performanceType(PerformanceType.POETRY)
                    .build();

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.of(performer));

            performanceService.addPerformance(dto, performerId, adminId, true, venue);

            ArgumentCaptor<Performance> captor = ArgumentCaptor.forClass(Performance.class);
            verify(performanceRepository).save(captor.capture());
            Performance saved = captor.getValue();

            assertThat(saved.getUser()).isEqualTo(performer);
            assertThat(saved.getName()).isEqualTo("poetry night");
            assertThat(saved.getDescription()).isEqualTo("original poems");
            assertThat(saved.getDuration()).isEqualTo(20);
            assertThat(saved.getPerformanceType()).isEqualTo(PerformanceType.POETRY);
        }

        /**
         * A performer adding a performance should save it for himself, the userId
         * passed as parameter must be ignored.
         */
        @Transactional
        @Test
        void addPerformance_performer_savesPerformanceForCurrentUser() {
            PerformanceDto dto = PerformanceDto.builder()
                    .name("poetry night")
                    .description("original poems")
                    .duration(20)
                    .performanceType(PerformanceType.POETRY)
                    .build();

            when(userRepository.findByIdAndVenueAndRole(performerId, venue, Role.PERFORMER))
                    .thenReturn(Optional.of(performer));

            performanceService.addPerformance(dto, secondPerformerId, performerId, false, venue);

            ArgumentCaptor<Performance> captor = ArgumentCaptor.forClass(Performance.class);
            verify(performanceRepository).save(captor.capture());

            assertThat(captor.getValue().getUser()).isEqualTo(performer);
            verify(userRepository, never()).findByIdAndVenueId(any(), any());
        }

        /**
         * An admin adding a performance for a user that does not exist in the venue
         * should get a ResourceNotFoundException, and nothing must be saved.
         */
        @Transactional
        @Test
        void addPerformance_admin_throwsResourceNotFoundException_whenUserNotFound() {
            PerformanceDto dto = PerformanceDto.builder().name("poetry night").build();

            when(userRepository.findByIdAndVenueId(performerId, venue.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> performanceService.addPerformance(dto, performerId, adminId, true, venue))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(performerId.toString());

            verify(performanceRepository, never()).save(any());
        }

        /**
         * A user without the PERFORMER role in the venue should get a
         * ResourceNotFoundException, and nothing must be saved.
         */
        @Transactional
        @Test
        void addPerformance_performer_throwsResourceNotFoundException_whenUserNotFound() {
            PerformanceDto dto = PerformanceDto.builder().name("poetry night").build();

            when(userRepository.findByIdAndVenueAndRole(performerId, venue, Role.PERFORMER))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> performanceService.addPerformance(dto, null, performerId, false, venue))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(performanceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deletePerformance")
    class DeletePerformance {

        /**
         * deletePerformance should not remove the performance, it should only mark
         * it as INACTIVE.
         */
        @Transactional
        @Test
        void deletePerformance_success_setsTypeToInactive() {
            when(performanceRepository.findById(performanceId)).thenReturn(Optional.of(performance));

            performanceService.deletePerformance(performanceId);

            assertThat(performance.getPerformanceType()).isEqualTo(PerformanceType.INACTIVE);
            verify(performanceRepository, never()).delete(any());
        }

        /**
         * deletePerformance should throw ResourceNotFoundException when the
         * performance does not exist.
         */
        @Transactional
        @Test
        void deletePerformance_throwsResourceNotFoundException_whenPerformanceNotFound() {
            UUID otherId = UUID.randomUUID();

            when(performanceRepository.findById(otherId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> performanceService.deletePerformance(otherId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(otherId.toString());
        }
    }

    @Nested
    @DisplayName("modifyPerformance")
    class ModifyPerformance {

        /**
         * modifyPerformance should update the type and the duration of the performance.
         */
        @Transactional
        @Test
        void modifyPerformance_success_updatesTypeAndDuration() {
            when(performanceRepository.findById(performanceId)).thenReturn(Optional.of(performance));

            performanceService.modifyPerformance(performanceId, PerformanceType.LIVE_MUSIC, 45);

            assertThat(performance.getPerformanceType()).isEqualTo(PerformanceType.LIVE_MUSIC);
            assertThat(performance.getDuration()).isEqualTo(45);
        }

        /**
         * modifyPerformance should throw ResourceNotFoundException when the
         * performance does not exist.
         */
        @Transactional
        @Test
        void modifyPerformance_throwsResourceNotFoundException_whenPerformanceNotFound() {
            UUID otherId = UUID.randomUUID();

            when(performanceRepository.findById(otherId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> performanceService.modifyPerformance(otherId, PerformanceType.COMEDY, 30))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(otherId.toString());
        }
    }
}