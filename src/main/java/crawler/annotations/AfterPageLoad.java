package crawler.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method to be called after the browser maps the value to class.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target(ElementType.METHOD)
public @interface AfterPageLoad {
}
