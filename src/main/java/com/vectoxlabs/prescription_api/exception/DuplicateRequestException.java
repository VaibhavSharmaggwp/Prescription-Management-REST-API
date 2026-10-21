package com.vectoxlabs.prescription_api.exception;

public class DuplicateRequestException extends RuntimeException{
    public DuplicateRequestException(String message){
        super(message);
    }
}
