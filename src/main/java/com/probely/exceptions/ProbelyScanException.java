package com.probely.exceptions;

import hudson.AbortException;

public class ProbelyScanException extends AbortException {
    private static final long serialVersionUID = 1L;

    public ProbelyScanException(String s) {
        super(s);
    }
}
