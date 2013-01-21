package ws.palladian.retrieval.feeds;

import java.util.Date;
import java.util.Map;

/**
 * <p>
 * Represents a news item within a feed ({@link Feed}).
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Sandro Reichert
 */
public class FeedItem {

    /** The logger for this class. */
//    private static final Logger LOGGER = Logger.getLogger(FeedItem.class);

    private int id = -1;

    /** The feed to which this item belongs to. */
    private Feed feed;

    /**
     * For performance reasons, we need to get feed items from the database and in that case we don't have the feed
     * object.
     */
    private int feedId = -1;

    private String title;
    private String link;

    /** Original ID from the feed entry. */
    private String rawId;

    /** The original publish date as read from the feed('s entry). */
    private Date published;

    /** When the entry was added to the database, usually set by the database. */
    private Date added;

    /** Author information. */
    private String authors;

    /** Description text of feed entry */
    private String description;

    /** Text directly from the feed entry */
    private String text;

    /** The item's hash. */
    private String itemHash = null;

    /** Allows to keep arbitrary, additional information. */
    private Map<String, Object> additionalData;

    /** The timestamp this item was fetched the first time. */
    private Date pollTimestamp;

    /**
     * The item's corrected published date. In contrast to {@link #published}, this value may be modified at the time of
     * the poll this item has been received the first time.
     */
    private Date correctedPublishedDate = null;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFeedId() {
        if (getFeed() != null) {
            return getFeed().getId();
        }
        return feedId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getRawId() {
        return rawId;
    }

    public void setRawId(String rawId) {
        this.rawId = rawId;
    }

    /**
     * The original publish date as read from the feed('s item). Might be in the future.
     * 
     * @return
     */
    public Date getPublished() {
        return published;
    }

    /**
     * The original publish date as read from the feed('s item).
     * 
     * @param published
     */
    public void setPublished(Date published) {
        this.published = published;
    }

    /**
     * When the entry was added to the database, usually set by the database.
     * 
     * @return Date the entry was added to the database, usually set by the database.
     */
    public Date getAdded() {
        return added;
    }

    /**
     * When the entry was added to the database, usually set by the database.
     * 
     * @param added Date the entry was added to the database, usually generated by the database.
     */
    public void setAdded(Date added) {
        this.added = added;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FeedItem");
        sb.append(" feedId:").append(feedId);
        sb.append(" id:").append(id);
        sb.append(" title:").append(title);
        sb.append(" link:").append(link);
        sb.append(" rawId:").append(rawId);
        sb.append(" published:").append(published);
        return sb.toString();
    }

    public String getFeedUrl() {
        if (getFeed() != null) {
            return getFeed().getFeedUrl();
        }
        return "";
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
        setFeedId(feed.getId());
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    /**
     * Replaces the current item hash with the given one. Don't never ever ever ever use this. This is meant to be used
     * only by the persistence layer and administrative authorities. And Chuck Norris.
     * 
     * <p>
     * Setting an item hash that has not been calculated by the current implementation of {@link #generateHash()} voids
     * the {@link Feed}'s duplicate detection. Duplicate items are not identified, you may get false positive MISSes.
     * This setter may be used to create items from persisted csv files used in the TUDCS6 dataset.
     * </p>
     * 
     * @param itemHash New item hash to set.
     */
    public void setHash(String itemHash) {
        this.itemHash = itemHash;
    }

    /**
     * The custom hash used to identify items beyond their raw id that is empty for 20% of the feeds.
     * 
     * @return The items custom hash.
     */
    public String getHash() {
        if (itemHash == null) {
            itemHash = FeedItemHashGenerator.generateHash(this);
        }
        return itemHash;
    }

//    /**
//     * Returns a custom hash representation calculated by the item's title, link and raw id or <code>null</code> if it
//     * is impossible to calculate a meaningful hash because title, link and raw id are all <code>null</code> or the
//     * empty string. SessionIDs are removed from link and raw id (in case raw id contains a url string only)
//     * 
//     * @return sha1 hash.
//     */
//    private String generateHash() {
//        String newHash = null;
//
//        StringBuilder hash = new StringBuilder();
//        hash.append(getTitle());
//        hash.append(UrlHelper.removeSessionId(getLink(), false));
//        hash.append(UrlHelper.removeSessionId(getRawId(), true));
//        // if (getFeed().getActivityPattern() != FeedClassifier.CLASS_UNKNOWN
//        // && getFeed().getActivityPattern() != FeedClassifier.CLASS_ON_THE_FLY) {
//        // hash.append(getPublished().toString());
//        // }
//        if (getTitle() != null || getLink() != null || getRawId() != null) {
//            newHash = StringHelper.sha1(hash.toString());
//
//        } else {
//            LOGGER.error("Could not generate custom item hash, all values are null or empty. Feed id " + feed.getId());
//        }
//
//        return newHash;
//    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public Object getAdditionalData(String key) {
        return additionalData.get(key);
    }

    /**
     * The item's corrected published date. In contrast to {@link #getPublished()}, this value may be modified at the
     * time of the poll this item has been received the first time.
     * 
     * @return the correctedPublishedTimestamp
     */
    public final Date getCorrectedPublishedDate() {
        return correctedPublishedDate;
    }

    /**
     * The item's corrected published date. In contrast to {@link #setPublished(Date)}, this value may be modified at
     * the time of the poll this item has been received the first time.
     * 
     * @param correctedPublishedDate the correctedPublishedTimestamp to set
     */
    public final void setCorrectedPublishedDate(Date correctedPublishedDate) {
        this.correctedPublishedDate = correctedPublishedDate;
    }

    /**
     * @return the pollTimestamp
     */
    public final Date getPollTimestamp() {
        return pollTimestamp;
    }

    /**
     * @param pollTimestamp the pollTimestamp to set
     */
    public final void setPollTimestamp(Date pollTimestamp) {
        this.pollTimestamp = pollTimestamp;
    }

//    /**
//     * <p>
//     * Free the memory because feed item objects might be held in memory. Rests everything to <code>null</code> except
//     * the dates {@link #published}, {@link #correctedPublishedDate}, {@link #httpDate} and the {@link #itemHash}. Use
//     * with caution :)
//     * </p>
//     * Usually used in case one wants to generate feed post statistics using all items a feed has--this number may
//     * exceed 10 million as seen in TUDCS6 dataset.
//     */
//    public final void freeMemory() {
//        added = null;
//        additionalData = null;
//        authors = null;
//        description = null;
//        feed = null;
//        link = null;
//        rawId = null;
//        text = null;
//        title = null;
//    }

}