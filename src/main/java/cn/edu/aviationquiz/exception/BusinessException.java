package cn.edu.aviationquiz.exception;

/** Expected business-rule failure whose message can be shown directly to the user. */
public class BusinessException extends IllegalArgumentException {
    public BusinessException(String message) {
        super(message);
    }
}
