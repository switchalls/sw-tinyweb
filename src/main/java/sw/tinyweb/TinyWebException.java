package sw.tinyweb;

public class TinyWebException extends Exception {

    private static final long serialVersionUID = -4828564365190691512L;

    private final int errorCode;

    public TinyWebException(int aCode, String aMsg) {
        super(aMsg);
        this.errorCode = aCode;
    }

    public TinyWebException(int aCode, String aMsg, Throwable aCause) {
        super(aMsg, aCause);
        this.errorCode = aCode;
    }

    /** @return HTTP status code */
    public int getErrorCode() {
        return this.errorCode;
    }

}
