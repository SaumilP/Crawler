package crawler.http;

/**
 * Created by PATEL1 on 12/27/14.
 */
public class GetException extends RuntimeException {
	public GetException( Exception ex , String getString ){
		super("Error while getting " + getString, ex );
	}
}
