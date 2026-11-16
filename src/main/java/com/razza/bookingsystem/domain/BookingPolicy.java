package com.razza.bookingsystem.domain;

import com.razza.bookingsystem.exception.BadCapacityException;
import jakarta.persistence.Access;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable describing how the capacity of an event is handled.
 * It holds the total capacity and how much of it is still available.
 * If the total capacity is null, the event does not require booking.
 */
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BookingPolicy {

    /** Maximum number of seats for the event. Null if no booking is required. */
    private Integer totalCapacity;

    /** Number of seats still available for booking. */
    private Integer availableCapacity;

    /**
     * Tells whether the event requires a booking.
     *
     * @return true if a total capacity is set, false otherwise
     */
    public Boolean isBookingRequired(){
        return totalCapacity != null;
    }

    /**
     * Creates a booking policy with a given total and available capacity.
     *
     * The total capacity must be greater than 0 and lower than 10000.
     * The available capacity must be between 0 and the total capacity.
     *
     * @param totalCapacity the maximum number of seats
     * @param availableCapacity the number of seats still available
     * @throws BadCapacityException if any of the values is out of range
     */
    public BookingPolicy(int totalCapacity, int availableCapacity){
        if(totalCapacity > 0) {
            this.totalCapacity = totalCapacity;
        } else {
            throw new BadCapacityException();
        }
        if(availableCapacity >= 0 && totalCapacity < 10000 && availableCapacity <= totalCapacity) {
            this.totalCapacity = totalCapacity;
            this.availableCapacity = availableCapacity;
        } else {
            throw new BadCapacityException();
        }
    }


    /**
     * Creates a booking policy with every seat still available.
     *
     * The total capacity must be greater than 0 and lower than 10000.
     *
     * @param totalCapacity the maximum number of seats
     * @throws BadCapacityException if the total capacity is out of range
     */
    public BookingPolicy(int totalCapacity){
        if(totalCapacity > 0 && totalCapacity < 10000) {
            this.totalCapacity = totalCapacity;
        } else {
            throw new BadCapacityException();
        }
        this.availableCapacity = this.totalCapacity;
    }

}