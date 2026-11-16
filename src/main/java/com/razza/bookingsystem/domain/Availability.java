package com.razza.bookingsystem.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder;

import java.util.UUID;

/**
 * Entity representing the weekly availability of a performer.
 *
 * Each day of the week is a Boolean flag telling whether the performer
 * is available on that day. It is linked one to one with its user.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "availability")
public class Availability {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Boolean monday;
    private Boolean tuesday;
    private Boolean wednesday;
    private Boolean thursday;
    private Boolean friday;
    private Boolean saturday;
    private Boolean sunday;

    /**
     * Creates an availability with a flag for each day of the week.
     * The user is not set here, it has to be linked afterwards.
     *
     * @param monday true if available on Monday
     * @param tuesday true if available on Tuesday
     * @param wednesday true if available on Wednesday
     * @param thursday true if available on Thursday
     * @param friday true if available on Friday
     * @param saturday true if available on Saturday
     * @param sunday true if available on Sunday
     */
    public Availability(Boolean monday, Boolean tuesday, Boolean wednesday, Boolean thursday, Boolean friday, Boolean saturday, Boolean sunday){
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
    }

    /**
     * Returns whether the user is available on a given day.
     *
     * The day name has to be lowercase, for example "monday".
     * Any unknown day name returns false.
     *
     * @param day the lowercase name of the day
     * @return true if available on that day, false otherwise
     */
    public Boolean getDay(String day){
        return switch (day) {
            case "monday" -> this.user.getAvailability().getMonday();
            case "tuesday" -> this.user.getAvailability().getTuesday();
            case "wednesday" -> this.user.getAvailability().getWednesday();
            case "thursday" -> this.user.getAvailability().getThursday();
            case "friday" -> this.user.getAvailability().getFriday();
            case "saturday" -> this.user.getAvailability().getSaturday();
            case "sunday" -> this.user.getAvailability().getSunday();
            default -> false;
        };
    }
}