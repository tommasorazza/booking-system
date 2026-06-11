package com.razza.bookingsystem.domain;

import jakarta.persistence.Access;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BookingPolicy {

    private Integer totalCapacity;

    private Integer availableCapacity;

    public Boolean isBookingRequired(){
        return totalCapacity != null;
    }

    public BookingPolicy(int totalCapacity, int availableCapacity){
        if(totalCapacity > 0) {
            this.totalCapacity = totalCapacity;
        } else {
            throw new RuntimeException("capacity should be higher than 0");
        }
        if(availableCapacity > 0 && availableCapacity <= totalCapacity) {
            this.totalCapacity = totalCapacity;
            this.availableCapacity = availableCapacity;
        } else {
            throw new RuntimeException("capacity should be higher than 0 and less or equal than total capacity");
        }
    }


    public BookingPolicy(int totalCapacity){
        if(totalCapacity > 0) {
            this.totalCapacity = totalCapacity;
        } else {
            throw new RuntimeException("capacity should be higher than 0");
        }
        this.availableCapacity = this.totalCapacity;
    }

}
