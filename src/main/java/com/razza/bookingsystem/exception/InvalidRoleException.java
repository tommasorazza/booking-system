package com.razza.bookingsystem.exception;

import com.razza.bookingsystem.domain.Role;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException(){
        super("this role does not exist, only possible options are: guest or performer");
    }
}
