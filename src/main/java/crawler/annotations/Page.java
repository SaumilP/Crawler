package crawler.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by PATEL1 on 12/27/14.
 */
@Retention(RetentionPolicy.RUNTIME )
@Target(ElementType.TYPE)
public @interface Page {
	String value() default "";
}
