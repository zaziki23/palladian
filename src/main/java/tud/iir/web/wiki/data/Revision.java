package tud.iir.web.wiki.data;

import java.util.Date;

/**
 * Represents a simplified revision of a Wiki page.
 * 
 * @author Sandro Reichert
 * 
 */
public class Revision {

    /**
     * The page's revision id, generated by Wiki.
     */
    private final long revisionID;

    /**
     * The timestamp this revision has been created on the Wiki.
     */
    private final Date timestamp;

    /**
     * The name of the Wiki user that created this revision.
     */
    private final String user;

    /**
     * A revision as retrieved from the Wiki
     * 
     * @param revisionID The page's revision id, generated by Wiki.
     * @param timestamp The timestamp this revision has been created on the Wiki.
     * @param user The name of the Wiki user that created this revision.
     */
    public Revision(final long revisionID, final Date timestamp, final String user) {
        this.revisionID = revisionID;
        this.timestamp = timestamp;
        this.user = user;
    }

    /**
     * @return The page's revision id, generated by Wiki.
     */
    public final long getRevisionID() {
        return revisionID;
    }

    /**
     * @return The timestamp this revision has been created on the Wiki.
     */
    public final Date getTimestamp() {
        return timestamp;
    }

    /**
     * @return The name of the Wiki user that created this revision.
     */
    public final String getAuthor() {
        return user;
    }

}
