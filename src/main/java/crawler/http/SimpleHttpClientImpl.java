package crawler.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

/**
 * Created by PATEL1 on 12/27/14.
 */
public class SimpleHttpClientImpl implements BrowserClient {

	public final CloseableHttpClient httpClient;
	private String userAgent = "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13";
	private final static String CONTENT_ENCODING = "UTF-8";

	public SimpleHttpClientImpl(){
		httpClient = HttpClients.createDefault();
	}

	public String getUserAgent(){
		return userAgent;
	}

	public void setUserAgent(String userAgent){
		this.userAgent = userAgent;
	}

	public String get(String getString){
		URL url;
		try {
			url = new URL(getString);
		} catch ( MalformedURLException ex ){
			throw new RuntimeException(ex);
		}

		if ( url.getProtocol().equalsIgnoreCase("file") ){
			try {
				final FileInputStream fileInputStream = new FileInputStream( new File(url.getPath() ) );
				return IOUtils.toString( fileInputStream, CONTENT_ENCODING );
			} catch ( IOException ex ) {
				throw new RuntimeException(ex);
			}
		}

		try {
			return internalGet( url );
		} catch ( ClientProtocolException ex){
			throw new RuntimeException( ex );
		} catch ( IOException ex ){
			throw new RuntimeException( ex );
		} catch ( URISyntaxException ex ){
			throw new RuntimeException( ex );
		}
	}

	private String internalGet(final URL get) throws IOException, ClientProtocolException, URISyntaxException {
		HttpUriRequest request = RequestBuilder.get()
                .setUri(get.toURI())
                .setHeader("User-Agent", userAgent)
                .build();
        return executeRequest(request);
	}

	private String executeRequest(HttpUriRequest request) throws IOException,ClientProtocolException {
		CloseableHttpResponse execute = httpClient.execute(request);
        final HttpEntity entity =  execute.getEntity();
        final InputStream contentIS = entity.getContent();
        final Header contentType = entity.getContentType();
        final HeaderElement[] elements = contentType.getElements();
        final HeaderElement headerElement = elements[0];
        final NameValuePair parameterByName = headerElement.getParameterByName("charset");
        String encoding = "UTF-8";
        if(parameterByName != null)
            encoding = parameterByName.getValue();
        if(encoding != null  && encoding.equals("ISO-8859-1")){
            encoding = "CP1252";
        }
        final String content = IOUtils.toString(contentIS,encoding);
        contentIS.close();
        return content;
	}

	public String post(final String post, BasicNameValuePair... params ){
		try {
			return internalPost(post, params );
		} catch ( ClientProtocolException  ex ){
			throw new RuntimeException( ex );
		} catch( IOException ex ){
            throw new RuntimeException( ex );
        }
	}

	private String internalPost( String post, BasicNameValuePair... params) throws IOException, ClientProtocolException {
		HttpUriRequest request = RequestBuilder.post()
									.addParameters(params)
									.setUri(post)
									.setHeader("User-Agent", userAgent)
									.build();
		return executeRequest(request);
	}
}
