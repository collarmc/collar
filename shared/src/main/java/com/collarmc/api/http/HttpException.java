package com.collarmc.api.http;

public abstract class HttpException extends RuntimeException {
    public final int code;

    public HttpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public HttpException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public static class BadRequestException extends HttpException {

        public final String body;

        public BadRequestException(String message) {
            super(400, message);
            this.body = null;
        }

        public BadRequestException(String message, String body) {
            super(400, message);
            this.body = body;
        }
    }

    public static class UnauthorisedException extends HttpException {
        public UnauthorisedException(String message) {
            super(401, message);
        }
    }

    public static class ForbiddenException extends HttpException {
        public ForbiddenException(String message) {
            super(403, message);
        }
    }

    public final static class NotFoundException extends HttpException {

        public NotFoundException() {
            this("not found");
        }

        public NotFoundException(String message) {
            super(404, message);
        }
    }

    public final static class ConflictException extends HttpException {
        public ConflictException(String message) {
            super(409, message);
        }
    }

    public final static class ServerErrorException extends HttpException {
        public ServerErrorException(String message) {
            super(500, message);
        }

        public ServerErrorException(String message, Throwable cause) {
            super(500, message, cause);
        }
    }

    public final static class UnmappedHttpException extends HttpException {
        public UnmappedHttpException(int httpCode, String message) {
            super(httpCode, message);
        }
    }
}
