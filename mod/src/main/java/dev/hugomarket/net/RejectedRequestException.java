package dev.hugomarket.net;

public final class RejectedRequestException extends RuntimeException {
    private final int status;

    public RejectedRequestException(int status, String body) {
        super("http " + status + ": " + (body.length() > 300 ? body.substring(0, 300) : body));
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static boolean isRejection(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof RejectedRequestException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
