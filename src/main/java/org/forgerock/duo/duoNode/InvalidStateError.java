package org.forgerock.duo.duoNode;

/**
 * Indicates the session (sharedState) & callback URL parameters do not match.
 */
public class InvalidStateError extends Exception {
    public InvalidStateError(String message) { super(message); }
}
