package de.l3s.boilerpipe.sax;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * A very simple HTTP/HTML fetcher, really just for demo purposes.
 * 
 * @author Christian Kohlschütter
 */
public class HTMLFetcher {
	private HTMLFetcher() {
	}

	private static final Pattern PAT_CHARSET = Pattern
			.compile("charset=([^; ]+)$");
	private static final String BROWSER_AGENT = "WebScraper/1.0";

	/**
	 * Fetches the document at the given URL, using {@link URLConnection}.
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static HTMLDocument fetch(final URL url) throws IOException {
		final URLConnection conn = url.openConnection();

		if (conn instanceof HttpURLConnection) {
			HttpURLConnection httpConn = (HttpURLConnection) conn;

//			httpConn.setConnectTimeout(30000);
//			httpConn.setReadTimeout(30000);
			httpConn.setRequestMethod("GET");
			httpConn.setRequestProperty("User-Agent", BROWSER_AGENT);
			httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
			httpConn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpConn.setRequestProperty("Connection", "keep-alive");
			
			// Set cookies to bypass common cookie banners
			String cookieHeader = buildCookieBypassHeader(url.getHost());
			httpConn.setRequestProperty("Cookie", cookieHeader);

			// Follow redirects
//			httpConn.setInstanceFollowRedirects(true);

			// Check response code
			int responseCode = httpConn.getResponseCode();
			if (responseCode == 429) {
				throw new IOException("Error 429: Too Many Requests for " + url);
			}
		}

		final String ct = conn.getContentType();

		if (ct == null
				|| !(ct.equals("text/html") || ct.startsWith("text/html;"))) {
			throw new IOException("Unsupported content type: "+ct);
		}

		Charset cs = Charset.forName("Cp1252");
        Matcher m = PAT_CHARSET.matcher(ct);
        if (m.find()) {
            final String charset = m.group(1);
            try {
                cs = Charset.forName(charset);
            } catch (UnsupportedCharsetException e) {
                // keep default
            }
        }

        InputStream in = conn.getInputStream();

		final String encoding = conn.getContentEncoding();
		if (encoding != null) {
			if ("gzip".equalsIgnoreCase(encoding)) {
				in = new GZIPInputStream(in);
			} else {
				System.err.println("WARN: unsupported Content-Encoding: "
						+ encoding);
			}
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int r;
		while ((r = in.read(buf)) != -1) {
			bos.write(buf, 0, r);
		}
		in.close();

		final byte[] data = bos.toByteArray();

		return new HTMLDocument(data, cs);
	}
	
	/**
	 * Builds a cookie header to bypass common cookie banners and consent forms.
	 * 
	 * @param host The hostname of the website
	 * @return Cookie header string
	 */
	private static String buildCookieBypassHeader(String host) {
		StringBuilder cookies = new StringBuilder();
		
		// Generic cookie consent bypass cookies
		addCookie(cookies, "cookieConsent", "true");
		addCookie(cookies, "cookie-consent", "accepted");
		addCookie(cookies, "cookiesAccepted", "true");
		addCookie(cookies, "acceptCookies", "true");
		addCookie(cookies, "gdpr-consent", "accepted");
		addCookie(cookies, "privacy-consent", "true");
		addCookie(cookies, "cookie_notice_accepted", "true");
		addCookie(cookies, "cookies_policy", "accepted");

		// Cookiebot
		addCookie(cookies, "CookieConsent", "{necessary:true,preferences:true,statistics:true,marketing:false}");
		addCookie(cookies, "CookieConsentBulkTicket", "accepted");

		return cookies.toString();
	}
	
	/**
	 * Helper method to add a cookie to the cookie string
	 */
	private static void addCookie(StringBuilder cookies, String key, String value) {
		if (cookies.length() > 0) {
			cookies.append("; ");
		}
		cookies.append(key).append("=").append(value);
	}

}
