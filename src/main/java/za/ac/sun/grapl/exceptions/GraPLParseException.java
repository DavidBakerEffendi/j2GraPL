package za.ac.sun.grapl.exceptions;

import java.text.ParseException;

public class GraPLParseException extends ParseException {
    /**
     * Constructs a ParseException with the specified detail message and
     * offset.
     * A detail message is a String that describes this particular exception.
     *
     * @param s the detail message.
     */
    public GraPLParseException(String s) {
        super(s, 0);
    }
}
