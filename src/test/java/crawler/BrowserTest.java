package crawler;

import crawler.exceptions.TooManyResultsException;
import crawler.http.BrowserClient;
import crawler.mappable.Foo;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by PATEL1 on 12/27/14.
 */
public class BrowserTest {
    @Test public void testUrlSubstitution() throws Exception {
        Browser.setClient( new BrowserClient() {
            @Override
            public String get(String getString) {
                return "DUMMY";
            }

            @Override
            public String post(String postString, BasicNameValuePair... params) {
                return "DUMMY";
            }
        });
        Browser.get(PageWithParameterizedURL.class, "x", "y");
        assertEquals("http://www.foo.com/x/bar/y/baz", Browser.getCurrentPageUrl() );
    }

    @Test public void testUriInvalidFormat() throws Exception {
        Browser.get("jkadfadff", Foo.class);
        Assert.fail("Should have thrown GetException");
    }

    @Test public void tooManyResults() throws Exception {
        final String fooUrl = Foo.class.getResource("Foo.html").toString();
        Browser.get( fooUrl, TooManyResultsException.class );
        Assert.fail("Should have thrown GetException");
    }

    @Test public void testUriNotAccessible() throws Exception{
        Browser.get("www.google.com", Foo.class);
        Assert.fail("Should have thrown GetException");
    }

    @Test
    public void testMappingFromResource(){

        final String fooUrl = Foo.class.getResource("Foo.html").toString();
        final Foo foo = Browser.get(fooUrl,Foo.class);

        assertEquals("Title",foo.someContent.title);
        assertEquals("Lorem ipsum",foo.someContent.text);

        assertEquals("Nested content Title",foo.someNestedContent.getHeader());
        assertEquals("Nested content",foo.someNestedContent.content);

        assertEquals(2,foo.section.someRepeatingContent.size());
        assertEquals("bar baz",foo.section.someRepeatingContent.get(0));
        assertEquals("bar2 baz2",foo.section.someRepeatingContent.get(1));

        assertEquals("<p> Get content as <br /> element </p>",foo.htmlContent.html());

        assertEquals("<a href=\"linkToBeExtracted1\">Some useless text</a> \n<a href=\"linkToBeExtracted2\">Some useless text</a>",foo.linksInnerHtml);
        assertEquals("<a href=\"./page2\">link to next page</a>",foo.linksOuterHtml);

        assertEquals("linkToBeExtracted1",foo.linksWithHref.get(0));
        assertEquals("linkToBeExtracted2",foo.linksWithHref.get(1));

        assertEquals(fooUrl+"/./page2",foo.nextPage.getLinkUrl());

        assertEquals("www.example.com",foo.linkList.get(0).getLinkUrl());
        assertEquals(fooUrl+"/./page3",foo.linkList.get(1).getLinkUrl());

        assertEquals("HEAD1",foo.repeatingContentsNoSurroundingTag.get(0).head);
        assertEquals("TAIL1",foo.repeatingContentsNoSurroundingTag.get(0).tail);
        assertEquals("HEAD2",foo.repeatingContentsNoSurroundingTag.get(1).head);
        assertEquals("TAIL2",foo.repeatingContentsNoSurroundingTag.get(1).tail);

        assertEquals(0,foo.doesNotExist.size());

        assertEquals(42,foo.getIntValue());
        assertEquals(42.24,foo.getFloatValue(),0.001);
        assertEquals(3.1415,foo.fHref,0.00001);
        assertTrue(foo.getBoolValue());

        assertEquals(41,foo.afterLoadValue);
    }
}
