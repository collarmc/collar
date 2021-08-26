package com.collarmc.security.messages;

public class CipherException extends Exception {

    public CipherException(String message) {
        super(message);
    }

    public CipherException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Thrown when a function of the cipher is not available (e.g. not implemented)
     */
    public static class UnavailableCipherException extends CipherException {
        public UnavailableCipherException(String message, Exception e) {
            super(message, e);
        }

        public UnavailableCipherException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when there was an unrecoverable problem with the cipher state
     */
    public static class UnknownCipherException extends CipherException {

        public UnknownCipherException(String message) {
            super(message);
        }

        public UnknownCipherException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when there was a recoverable problem with the cipher state.
     * on the client
     */
    public static class InvalidCipherSessionException extends CipherException {
        public InvalidCipherSessionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
