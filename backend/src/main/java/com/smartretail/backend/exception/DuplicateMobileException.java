package com.smartretail.backend.exception;

import lombok.Getter;

@Getter
public class DuplicateMobileException extends RuntimeException {
    private final String mobile;

    public DuplicateMobileException(String mobile) {
        super("Mobile already exists: " + mobile); // parent message bhi set ho gaya
        this.mobile = mobile;
    }

}
