package cn.edu.aviationquiz.exception;

/** Persistence failure raised by the data-access layer. */
public class DataAccessException extends IllegalStateException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
