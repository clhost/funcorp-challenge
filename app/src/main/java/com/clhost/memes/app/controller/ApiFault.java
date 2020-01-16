package com.clhost.memes.app.controller;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApiFault extends Exception {

    private final Long code;
    private final String message;

    public ApiFault(String message) {
        this.code = 5050L;
        this.message = message;
    }
}
