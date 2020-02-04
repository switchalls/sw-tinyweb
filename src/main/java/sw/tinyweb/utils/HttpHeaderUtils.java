package sw.tinyweb.utils;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;

import sw.tinyweb.HttpStatusCodes;
import sw.tinyweb.TinyWebCookie;
import sw.tinyweb.TinyWebException;

/**
 * Utilities for interpreting HTTP requests.
 *
 * <p>
 * See HTTP 1.1 <a href="http://www.w3.org/Protocols/rfc2616/rfc2616">specification</a>.
 * </p>
 *
 * @see javax.servlet.http.HttpServletRequest
 */
public class HttpHeaderUtils {
    /** Date format. */
    public static final String RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

    private static HttpHeaderUtils globalInstance;

    /** @return the global instance */
    public static HttpHeaderUtils getInstance() {
        if (globalInstance == null) {
            globalInstance = new HttpHeaderUtils();
        }

        return globalInstance;
    }

    /**
     * Extract the cookie.
     *
     * @param aText
     *            The text from the HTTP header
     * @return the new cookie
     * @throws TinyWebException
     *             when the cookie information is rejected
     */
    public Cookie parseCookie(String aText) throws TinyWebException {
        TinyWebCookie cookie = null;

        final StringTokenizer tokeniser = new StringTokenizer(aText, ";");
        while (tokeniser.hasMoreTokens()) {
            final String token = tokeniser.nextToken().trim();
            if ("HttpOnly".equals(token)) {
                cookie.setHttpOnly(true);
            } else if ("Secure".equals(token)) {
                cookie.setSecure(true);
            } else {
                final int ipos = token.indexOf('=');
                if (ipos < 0) {
                    throw new TinyWebException(HttpStatusCodes.BAD_REQUEST, "Invalid cookie field: " + token);
                }

                final String pname = token.substring(0, ipos).trim();
                final String pvalue = token.substring(ipos + 1).trim();

                if (cookie == null) {
                    cookie = new TinyWebCookie(pname, pvalue);
                } else if ("Domain".equals(pname)) {
                    cookie.setDomain(this.parseQuotedString(pvalue));
                } else if ("Expires".equals(pname)) {
                    final Date expires = this.parseDate(pvalue);
                    final long diff = (System.currentTimeMillis() - expires.getTime());
                    if (diff > 0) {
                        cookie.setMaxAge((int) (diff / 1000)); // convert to seconds
                    }
                } else if ("Path".equals(pname)) {
                    cookie.setPath(this.parseQuotedString(pvalue));
                } else if ("Version".equals(pname)) {
                    cookie.setVersion(Integer.parseInt(pvalue));
                }
            }
        }

        if (cookie == null) {
            throw new TinyWebException(HttpStatusCodes.BAD_REQUEST, "Invalid HTTP header cookie: " + aText);
        }

        return cookie;
    }

    /**
     * Write a Set-Cookie directive.
     *
     * @param aWriter
     *            The writer
     * @param aCookie
     *            The cookie
     * @param aLocale
     *            The target locale
     *
     * @see #formatDate(Date, Locale)
     */
    public void writeSetCookie(PrintWriter aWriter, Cookie aCookie, Locale aLocale) {
        aWriter.print("Set-Cookie: ");
        aWriter.print(aCookie.getName());
        aWriter.print("=");
        aWriter.print(aCookie.getValue());

        if (aCookie.getMaxAge() > 0) {
            final Date expires = new Date(System.currentTimeMillis() + (aCookie.getMaxAge() * 1000));
            aWriter.print("; Expires=");
            aWriter.print(this.formatDate(expires, aLocale));
        }

        if (aCookie.getPath() != null) {
            aWriter.print("; Path=");
            aWriter.print(aCookie.getPath());
        }

        if (aCookie.getDomain() != null) {
            aWriter.print("; Domain=");
            aWriter.print(aCookie.getDomain());
        }

        if (aCookie.getVersion() > 0) {
            aWriter.print("; Version=");
            aWriter.print(aCookie.getVersion());
        }

        if (aCookie.getSecure()) {
            aWriter.print("; Secure");
        }

        if ((aCookie instanceof TinyWebCookie) && ((TinyWebCookie) aCookie).isHttpOnly()) {
            aWriter.print("; HttpOnly");
        }

        aWriter.println();
    }

    /**
     * Format the date as a <a href="http://tools.ietf.org/html/rfc1123">RFC1123</a> string.
     *
     * @param aTime
     *            The date to be formatted
     * @param aLocale
     *            The target locale
     * @return the formatted date
     */
    public String formatDate(Date aTime, Locale aLocale) {
        final SimpleDateFormat df = new SimpleDateFormat(RFC1123_DATE_FORMAT, aLocale);
        return df.format(aTime);
    }

    /**
     * Extract the date.
     *
     * @param aText
     *            The header text
     * @return the date
     * @throws TinyWebException
     *             when the date cannot be created
     */
    public Date parseDate(String aText) throws TinyWebException {
        SimpleDateFormat fmt = new SimpleDateFormat(RFC1123_DATE_FORMAT);
        try {
            return fmt.parse(aText);

        } catch (final ParseException e) {
            // ignore
        }

        fmt = new SimpleDateFormat();
        try {
            return fmt.parse(aText);

        } catch (final ParseException e) {
            throw new TinyWebException(HttpStatusCodes.BAD_REQUEST, "Invalid date field: " + aText, e);
        }
    }

    /**
     * Extract language preferences.
     *
     * @param aText
     *            The text
     * @return the languages in priority order
     * @throws IllegalArgumentException
     *             when the text is rejected
     */
    public List<LanguageTag> parseLanguages(String aText) throws IllegalArgumentException {
        final List<LanguageTag> languages = new ArrayList<LanguageTag>();

        final StringTokenizer tokeniser = new StringTokenizer(aText, ",");
        while (tokeniser.hasMoreTokens()) {
            final String token = tokeniser.nextToken().trim();

            int ipos = token.indexOf(';');
            if (ipos < 0) {
                languages.add(new LanguageTag(token));
            } else {
                final String language = token.substring(0, ipos);

                ipos = token.indexOf("q=");
                if (ipos < 0) {
                    throw new IllegalArgumentException("Invalid Accept-Language quality field: " + token);
                }

                final String q = token.substring(ipos + 2);
                languages.add(new LanguageTag(language, Double.parseDouble(q)));
            }
        }

        return languages;
    }

    /**
     * Remove enclosing quotes.
     *
     * @param aText
     *            The header text
     * @return the contents of the quoted string
     */
    public String parseQuotedString(String aText) {
        if (aText.startsWith("\"") && aText.endsWith("\"")) {
            return aText.substring(1, aText.length() - 2);
        }

        return aText;
    }

}
