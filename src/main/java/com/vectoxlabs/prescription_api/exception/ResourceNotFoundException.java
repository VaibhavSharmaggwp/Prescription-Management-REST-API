package com.vectoxlabs.prescription_api.exception;


// We will throw this manually in the Service layer
// if someone tries to create a prescription for a User or Client ID that doesn't exist in the database.
public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message){
        super(message);
    }
}
