package sw.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * <code>URL</code> related utilities.
 *
 * @author Stewart Witchalls
 * @version 3.0
 */
public class URLHelper {

    /** UTF-8 URL encode/decode option. */
    public static final String UTF8 = "UTF-8";

    /**
     * Decode the URL text.
     *
     * @param aEncodedText
     *            The text
     * @return the decoded text
     */
    public static String decodeString(String aEncodedText) {
        try {
            return URLDecoder.decode(aEncodedText, UTF8);

        } catch (final UnsupportedEncodingException e) {
            return aEncodedText;
        }
    }

    /**
     * Convert the string into to the W3C
     * <code>application/x-www-form-urlencoded</code> MIME format.
     *
     * @param aText
     *            The text
     * @return the encoded text
     * @see java.net.URLEncoder
     */
    public static String w3cEncodeString(String aText) {
        String s = aText;
        try {
            if (aText != null) {
                s = URLEncoder.encode(aText, UTF8);
            }

        } catch (final UnsupportedEncodingException e) {
            // should never happen for UTF8
        }

        return s;
    }

    /**
     * Get the URL to the resource's parent folder.
     *
     * @param aResourceURL
     *            The URL
     * @return the URL as a string
     */
    public static final String getParentURL(URL aResourceURL) {
        final String eform = aResourceURL.toExternalForm();
        final int p = eform.lastIndexOf('/');
        return getParentURL(aResourceURL, p);
    }

    /**
     * Get the URL to the resource's parent folder.
     *
     * @param aResourceURL
     *            The URL
     * @param aLastSlashPos
     *            Position of the last '/'
     * @return the URL as a string
     * @see #getParentURL(String, int)
     */
    public static final String getParentURL(URL aResourceURL, int aLastSlashPos) {
        return URLHelper.getParentURL(aResourceURL.toExternalForm(), aLastSlashPos);
    }

    /**
     * Get the URL to the resource's parent folder.
     *
     * <p>
     * NB. <code>jar:</code> URLs must always contain <code>!/</code>.
     * </p>
     *
     * @param aResourceURL
     *            The URL
     * @param aLastSlashPos
     *            Position of the last '/'
     * @return the URL as a string
     */
    public static final String getParentURL(String aResourceURL, int aLastSlashPos) {
        final StringBuffer sb = new StringBuffer(aLastSlashPos + 1);
        sb.append(aResourceURL.substring(0, aLastSlashPos));
        if (sb.charAt(aLastSlashPos - 1) == '!') {
            sb.append('/');
        }

        return sb.toString();
    }

    /**
     * Append the path to the URL.
     *
     * @param aBaseURL
     *            The base URL
     * @param aPath
     *            The path
     * @return the new URL
     * @throws MalformedURLException
     *             when the new URL cannot be created
     * @see #newURL(String, String)
     */
    public static URL newURL(URL aBaseURL, String aPath) throws MalformedURLException {
        final String s = URLHelper.newURL(aBaseURL.toExternalForm(), aPath);
        return new URL(s);
    }

    /**
     * Append the path to the URL.
     *
     * <p>
     * Not all URL protocols implement the same path constructs. For example,
     * the <code>file:</code> protocol will accept <code>./</code> and
     * <code>../</code> where the <code>jar:</code> protocol will reject them.
     * </p>
     *
     * <p>
     * Some URL types must adhere to a specific syntax. For example,
     * <code>jar:</code> URLs take the form:
     * <b>jar:</b>&lt;ZIP location&gt;<b>!/</b>&lt;path inside ZIP&gt;
     * and will be rejected when they do contain <code>!/</code>.
     * </p>
     *
     * <p>
     * This helper method will assemble the URL in the correct manner
     * for its stated protocol.
     * </p>
     *
     * @param aBaseURL
     *            The base URL
     * @param aPath
     *            The path
     * @return the new URL
     * @throws MalformedURLException
     *             when the new URL cannot be created
     */
    public static String newURL(String aBaseURL, String aPath) throws MalformedURLException {
        // Assume that aPath never starts with a '/'

        String path = aPath;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        final StringBuffer sb = new StringBuffer(aBaseURL.length() + path.length() + 1);

        // Handle JAR URLs differently to normal ones
        //
        // NB. JAR URLs do not like './' or '../' in the path.
        // java.net.URL(URL,path) will remove them from the path.

        final int p = aBaseURL.lastIndexOf("!/");
        if (p > 0) {
            final URL baseURL = new URL(aBaseURL.substring(0, p + 2));
            final String base = aBaseURL.substring(p + 1);

            sb.append(base);

            if (!base.endsWith("/")) {
                sb.append('/');
            }

            sb.append(path);

            // JAR URLs handle concatenation differently to normal URLs,
            // so must delegate construction to java.net.URL

            final URL newURL = new URL(baseURL, decodeString(sb.toString()));
            return newURL.toExternalForm();
        }

        sb.append(aBaseURL);

        if (!aBaseURL.endsWith("/")) {
            sb.append('/');
        }

        sb.append(path);

        return sb.toString();
    }

}
