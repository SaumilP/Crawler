package crawler.exceptions;

/**
 * @author Saumil Patel
 * @version 1.0
 */
public class TooManyResultsException extends RuntimeException {

    private static final long serialVersionUID = -160247095684560019L;

    public TooManyResultsException(String message, int size) {
        super(String.format("The Query %s should return one result but returned %d. For more than one result a list should be used as the field type."));
    }
}
