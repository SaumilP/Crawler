package crawler.elements;

import crawler.Browser;
import org.jsoup.nodes.Element;

/**
 * Created by PATEL1 on 12/27/14.
 */
public class Link<T> {
    private final Class<T> type;
    private final Element hrefElement;
    private final String currentPageUrl;

    public Link(final Element hrefElement,final Class<T> visitingType, String baseUrl) {
        this.type = visitingType;
        this.hrefElement = hrefElement;
        this.currentPageUrl = baseUrl;
    }

    public String getLinkUrl(){
        String urlToVisit;
        final String href = hrefElement.attr("href");
        urlToVisit = href;
        if ( href.startsWith("/")){
            final String newPageUrl = currentPageUrl + "/" + href;
            urlToVisit = newPageUrl;
        }
        return urlToVisit;
    }

    public T visit(){
        return Browser.get(getLinkUrl(), type);
    }
}
