package crawler.elements;

import java.util.List;

import crawler.exceptions.WrongTypeForField;
import org.jsoup.nodes.Element;

/**
 * Created by PATEL1 on 12/27/14.
 */
public class Instantiator {
	public static boolean typeIsKnown( final Class clazz ){
		if ( clazz.equals( String.class ) ){
			return true;
		} else if ( clazz.equals(Integer.class) || clazz.getSimpleName().equals("int") ){
			return true;
		} else if ( clazz.equals(Float.class) || clazz.getSimpleName().equals("float") ){
			return true;
		} else if ( clazz.equals(Boolean.class) || clazz.getSimpleName().equals("boolean") ){
			return true;
		} else if ( clazz.equals(Link.class)){
			return true;
		} else if ( clazz.equals(Element.class)){
			return true;
		} else if ( clazz.equals(List.class)){
			return true;
		}
        return false;
	}

	public static <T> T instanceForNode(final Element node, String attribute, final Class<T> clazz){
		String value;
		try {
			if ( clazz.equals( Element.class )){
				return (T)node;
			}

			if ( attribute != null && !attribute.isEmpty()){
				if ( attribute.equals("html") ){
					value = node.html();
				} else if ( attribute.equals("outerHtml")){
					value = node.outerHtml();
				} else {
					value = node.attr(attribute);
				}
			} else {
				value = node.text();
			}

			if ( clazz.equals( String.class ) ){
				return (T)value;
			} else if ( clazz.equals( Integer.class) || clazz.getSimpleName().equals("int") ){
				return (T)Integer.valueOf(value);
			} else if ( clazz.equals( Float.class) || clazz.getSimpleName().equals("flaot") ){
				return (T)Float.valueOf(value);
			} else if ( clazz.equals( Boolean.class) || clazz.getSimpleName().equals("boolean") ){
				return (T)Boolean.valueOf(value);
			}
		} catch( Exception ex ){
			throw new WrongTypeForField(node, attribute, clazz, ex );
		}
		return (T) value;
	}

	public static boolean typeIsVisitable(final Class<?> fieldClass){
		return fieldClass.equals( Link.class );
	}

	public static <T> T visitableForNode( final Element node, final Class clazz, final String currentPageUrl ){
		return (T) new Link<T>(node, clazz, currentPageUrl);
	}
}
