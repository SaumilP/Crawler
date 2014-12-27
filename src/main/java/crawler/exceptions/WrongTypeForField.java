package crawler.exceptions;

import org.jsoup.nodes.Element;

/**
 * Created by PATEL1 on 12/27/14.
 */
public class WrongTypeForField extends RuntimeException {
	public WrongTypeForField( Element node, String attribute, @SuppressWarnings("rawtypes") Class clazz, Exception ex ){
		super("Element can't be mapped to attribute " + attribute + " with type " + clazz.getSimpleName() + "\n"
			+ "Element contents: \n" + node.html(), ex);
	}
}
