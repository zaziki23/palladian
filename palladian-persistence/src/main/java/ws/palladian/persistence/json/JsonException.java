package ws.palladian.persistence.json;

import ws.palladian.persistence.ParserException;

/**
 * The JSONException is thrown by the JSON.org classes when things are amiss.
 *
 * @author JSON.org
 * @author Philipp Katz
 * @version 2013-02-10
 */
public class JsonException extends ParserException {
    private static final long serialVersionUID = 0;
    private Throwable cause;

    /**
     * Constructs a JSONException with an explanatory message.
     *
     * @param message Detail about the reason for the exception.
     */
    JsonException(String message) {
        super(message);
    }

    /**
     * Constructs a new JSONException with the specified cause.
     */
    JsonException(Throwable cause) {
        super(cause.getMessage());
        this.cause = cause;
    }

    /**
     * Returns the cause of this exception or null if the cause is nonexistent
     * or unknown.
     *
     * @returns the cause of this exception or null if the cause is nonexistent
     * or unknown.
     */
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
