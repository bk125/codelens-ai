package com.codereviewer.exception;

public class CodeReviewNotFoundException extends RuntimeException {

    public CodeReviewNotFoundException(String message) {
        super(message);
    }

    public CodeReviewNotFoundException(Long id) {
        super("Code review not found with id: " + id);
    }
}
