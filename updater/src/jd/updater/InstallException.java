package jd.updater;

public class InstallException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int               revertErrors;

    public InstallException(final String string) {
        super(string);
    }

    public InstallException(final String string, final int errors, final Throwable e) {
        super(string, e);
        revertErrors = errors;
    }

    public int getRevertErrors() {
        return revertErrors;
    }

}
