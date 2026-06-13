package com.razza.bookingsystem.domain;

import com.razza.bookingsystem.exception.BadCapacityException;
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
            throw new BadCapacityException();
        }
        if(availableCapacity > 0 && totalCapacity < 10000 && availableCapacity <= totalCapacity) {
            this.totalCapacity = totalCapacity;
            this.availableCapacity = availableCapacity;
        } else {
            throw new BadCapacityException();
        }
    }


    public BookingPolicy(int totalCapacity){
        if(totalCapacity > 0 && totalCapacity < 10000) {
            this.totalCapacity = totalCapacity;
        } else {
            throw new BadCapacityException();
        }
        this.availableCapacity = this.totalCapacity;
    }

}
