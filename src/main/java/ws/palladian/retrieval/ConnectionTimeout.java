package ws.palladian.retrieval;

import java.net.HttpURLConnection;
import java.net.URLConnection;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;

/**
 * The ConnectionTimeout is necessary because Java does not set timeouts when a server starts sending data and stops without sending an end signal.
 * 
 * @deprecated replaced by {@link ConnectionTimeoutPool}.
 * @author David Urbansky
 * @author Philipp Katz
 */
@Deprecated
public final class ConnectionTimeout implements Runnable {

    /** The connection timeout class. */
    private URLConnection urlConnection = null;

    private int timeout = (int) (2 * DateHelper.MINUTE_MS);

    /** Whether the timeout is active or not. */
    private boolean active = true;

    public ConnectionTimeout(URLConnection urlConnection, int timeout) {
        this.urlConnection = urlConnection;
        this.timeout = timeout;
        new Thread(this, "ConnectionTimeoutThread").start();
    }

    @Override
    public final void run() {

        // sleep
        try {

            // check here every second if the ConnectionTimeout is still active;
            // if not, we can terminate, this prevents lagging caused by the timeout.
            for (int i = 0; i < timeout / 1000; i++) {
                if (isActive()) {
                    Thread.sleep(1000);
                } else {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Logger.getRootLogger().error("connection timeout has been interrupted, " + e.getMessage());
        }

        // close URLConnection
        if (urlConnection instanceof HttpURLConnection && isActive()) {
            try {
                ((HttpURLConnection) urlConnection).disconnect();
                // Logger.getRootLogger().warn("urlConnection had to be timed out " + urlConnection.getURL()); //XXX
            }catch(Exception e){
                Logger.getRootLogger().error("urlConnection time-out failed" + urlConnection.getURL());
            }

        } else {
            // System.out.println("Disconnect not attempted.");
        }
    }

    public boolean isActive() {
        return active;
    }

    /**
     * (De)activates the timer.<br />
     * <br />
     * CAUTION! Value currently not set, this is a hack for the feed dataset paper. Ask Philipp Katz :)
     * 
     * @param active <code>true</code> if the timeout is active.
     */
    public void setActive(boolean active) {
        // this.active = active; //XXX
    }
}