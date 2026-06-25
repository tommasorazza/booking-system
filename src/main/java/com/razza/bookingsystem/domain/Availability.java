package com.razza.bookingsystem.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder;

import java.util.UUID;

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

    public Availability(Boolean monday, Boolean tuesday, Boolean wednesday, Boolean thursday, Boolean friday, Boolean saturday, Boolean sunday){
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
    }

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
