
package com.miniweb.http;

import java.io.IOException;

public class HttpParseException extends IOException {

    private static final long serialVersionUID = 1L;

    public HttpParseException(String message) {
        super(message);
    }
}
