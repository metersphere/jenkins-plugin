package com.metersphere;

/**
 * Created by linjinbo on 2017/10/22.
 */
public class CodeDeployException extends Exception{
    private static final long serialVersionUID = 1582215285822395979L;

    public CodeDeployException() {
        super();
    }

    public CodeDeployException(final String message, final Throwable cause) {
        super(message,cause);
    }

    public CodeDeployException(final String message) {
        super(message);
    }
}
