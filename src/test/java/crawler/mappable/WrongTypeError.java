package crawler.mappable;

import crawler.annotations.Page;
import crawler.annotations.Selector;

/**
 * Created by PATEL1 on 12/27/14.
 */
@Page
public class WrongTypeError {
    @Selector("#boolean") public float badFloat;
}
