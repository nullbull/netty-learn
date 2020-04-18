package com.niu.netty.rpc.exceptions;


public class OutMaxLengthException extends  RuntimeException {

    public OutMaxLengthException() {
        super();
    }


    public OutMaxLengthException(String message) {
        super(message);
    }

    public OutMaxLengthException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutMaxLengthException(Throwable cause) {
        super(cause);
    }

}
