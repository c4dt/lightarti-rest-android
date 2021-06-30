package org.c4dt.artiwrapper;

/**
 * Exception thrown by the library during the processing of a request
 */
public class TorLibException extends Exception {
    public TorLibException(String message) {
        super(message);
    }
}
