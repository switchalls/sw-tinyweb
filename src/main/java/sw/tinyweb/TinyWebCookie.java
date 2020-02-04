package sw.tinyweb;

import javax.servlet.http.Cookie;

/**
 * TinyWeb cookie.
 *
 * <p>
 * Provides additional attributes over standard cookie definition.
 * </p>
 */
public class TinyWebCookie extends Cookie {

    private boolean httpOnly;

    public TinyWebCookie(String aName, String aValue) {
        super(aName, aValue);
    }

    /** @return false when this cookie can be used by cross-domain requests */
    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    public void setHttpOnly(boolean aFlag) {
        this.httpOnly = aFlag;
    }

}
