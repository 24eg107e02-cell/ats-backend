package com.akshay.ats.common;

public class InvalidStageTransitionException extends RuntimeException {
    public InvalidStageTransitionException(String message) {
        super(message);
    }
}
