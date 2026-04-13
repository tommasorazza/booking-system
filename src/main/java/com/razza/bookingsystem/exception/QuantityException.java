package com.razza.bookingsystem.exception;

/**
 * Exception thrown when an invalid booking quantity is provided.
 *
 * This occurs when the quantity is zero, negative,
 * or exceeds allowed limits defined by the system.
 */
public class QuantityException extends RuntimeException {

    /**
     * Constructs a new QuantityException with the invalid quantity.
     *
     * @param quantity the invalid quantity value
     */
    public QuantityException(int quantity){
        super(quantity + " is not a valid quantity");
    }
}