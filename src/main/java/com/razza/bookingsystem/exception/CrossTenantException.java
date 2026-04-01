package com.razza.bookingsystem.exception;

public class CrossTenantException extends RuntimeException {
    public CrossTenantException(){
        super("cross-tenant booking not allowed");
    }
}
