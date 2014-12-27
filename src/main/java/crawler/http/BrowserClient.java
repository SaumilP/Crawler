package crawler.http;

import org.apache.http.message.BasicNameValuePair;

/**
 * Created by PATEL1 on 12/27/14.
 */
public interface BrowserClient {
	public String get(final String getString);
	public String post(final String postString, BasicNameValuePair... params );
}
