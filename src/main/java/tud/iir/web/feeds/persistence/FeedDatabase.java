package tud.iir.web.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.ResultIterator;
import tud.iir.persistence.ResultSetCallback;
import tud.iir.persistence.RowConverter;
import tud.iir.web.feeds.Feed;
import tud.iir.web.feeds.FeedItem;

/**
 * The FeedDatabase is an implementation of the FeedStore that stores feeds and items in a relational database.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class FeedDatabase extends DatabaseManager implements FeedStore {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
    private static final String psAddFeedItem = "INSERT IGNORE INTO feed_items SET feedId = ?, title = ?, link = ?, rawId = ?, published = ?, authors = ?, text = ?, pageText = ?";
    private static final String psAddFeed = "INSERT IGNORE INTO feeds SET feedUrl = ?, siteUrl = ?, title = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, activityPattern = ?";
    private static final String psUpdateFeed = "UPDATE feeds SET feedUrl = ?, siteUrl = ?, title = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, lastEtag = ?, lastPollTime = ?, activityPattern = ? WHERE id = ?";
    private static final String psUpdateFeedPostDistribution = "REPLACE INTO feeds_post_distribution SET feedID = ?, minuteOfDay = ?, posts = ?, chances = ?";
    private static final String psGetFeedPostDistribution = "SELECT minuteOfDay, posts, chances FROM feeds_post_distribution WHERE feedID = ?";
    private static final String psGetFeeds = "SELECT * FROM feeds";
    private static final String psGetFeedByUrl = "SELECT * FROM feeds WHERE feedUrl = ?";
    private static final String psGetFeedByID = "SELECT * FROM feeds WHERE id = ?";
    private static final String psGetItemsByRawId = "SELECT * FROM feed_items WHERE rawID = ?";
    private static final String psGetItemsByRawId2 = "SELECT * FROM feed_items WHERE feedId = ? AND rawID = ?";
    private static final String psChangeCheckApproach = "UPDATE feeds SET minCheckInterval = 5, maxCheckInterval = 1, lastHeadlines = '', checks = 0, lastFeedEntry = NULL";
    private static final String psGetItems = "SELECT * FROM feed_items LIMIT ? OFFSET ?";
    private static final String psGetAllItems = "SELECT * FROM feed_items";
    private static final String psGetItemById = "SELECT * FROM feed_items WHERE id = ?";
    private static final String psDeleteItemById = "DELETE FROM feed_items WHERE id = ?";

    @Override
    public boolean addFeed(Feed feed) {
        LOGGER.trace(">addFeed " + feed);
        boolean added = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getFeedUrl());
        parameters.add(feed.getSiteUrl());
        parameters.add(feed.getTitle());
        parameters.add(feed.getContentType().getIdentifier());
        parameters.add(feed.getLanguage());
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getLastHeadlines());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getLastFeedEntrySQLTimestamp());
        parameters.add(feed.getActivityPattern());
        int result = runUpdateReturnId(psAddFeed, parameters);
        if (result > 0) {
            feed.setId(result);
            added = true;
        }

        LOGGER.trace("<addFeed " + added);
        return added;
    }

    @Override
    public boolean updateFeed(Feed feed) {
        LOGGER.trace(">updateFeed " + feed);

        if (feed.getId() == -1) {
            LOGGER.debug("feed does not exist and is added therefore");
            return addFeed(feed);
        }

        boolean updated = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getFeedUrl());
        parameters.add(feed.getSiteUrl());
        parameters.add(feed.getTitle());
        parameters.add(feed.getContentType().getIdentifier());
        parameters.add(feed.getLanguage());
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getLastHeadlines());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getLastFeedEntrySQLTimestamp());
        parameters.add(feed.getLastETag());
        parameters.add(feed.getLastPollTimeSQLTimestamp());
        parameters.add(feed.getActivityPattern());
        parameters.add(feed.getId());

        int result = runUpdate(psUpdateFeed, parameters);
        if (result == 1) {
            updated = true;
        }

        LOGGER.trace("<updateFeed " + updated);

        return updated;
    }

    public Map<Integer, int[]> getFeedPostDistribution(Feed feed) {

        final Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                int minuteOfDay = resultSet.getInt("minuteOfDay");
                int posts = resultSet.getInt("posts");
                int chances = resultSet.getInt("chances");
                int[] postsChances = { posts, chances };
                postDistribution.put(minuteOfDay, postsChances);
            }
        };

        runQuery(callback, psGetFeedPostDistribution, feed.getId());
        return postDistribution;
    }

    public void updateFeedPostDistribution(Feed feed, Map<Integer, int[]> postDistribution) {
        for (java.util.Map.Entry<Integer, int[]> distributionEntry : postDistribution.entrySet()) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(feed.getId());
            parameters.add(distributionEntry.getKey());
            parameters.add(distributionEntry.getValue()[0]);
            parameters.add(distributionEntry.getValue()[1]);
            runUpdate(psUpdateFeedPostDistribution, parameters);
        }
    }

    /**
     * When the check approach is switched we need to reset learned and calculated values such as check intervals,
     * checks, lastHeadlines etc.
     */
    public void changeCheckApproach() {
        runUpdate(psChangeCheckApproach);
    }

    @Override
    public List<Feed> getFeeds() {
        return runQuery(new FeedRowConverter(), psGetFeeds);
    }

    @Override
    public Feed getFeedByUrl(String feedUrl) {
        return runSingleQuery(new FeedRowConverter(), psGetFeedByUrl, feedUrl);
    }

    @Override
    public Feed getFeedByID(int feedID) {
        return runSingleQuery(new FeedRowConverter(), psGetFeedByID, feedID);
    }

    @Override
    public boolean addFeedItem(Feed feed, FeedItem entry) {
        LOGGER.trace(">addEntry " + entry + " to " + feed);
        boolean added = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getId());
        parameters.add(entry.getTitle());
        parameters.add(entry.getLink());
        parameters.add(entry.getRawId());
        parameters.add(entry.getPublishedSQLTimestamp());
        parameters.add(entry.getAuthors());
        parameters.add(entry.getItemText());
        parameters.add(entry.getPageText());

        int result = runUpdateReturnId(psAddFeedItem, parameters);
        if (result > 0) {
            entry.setId(result);
            added = true;
        }

        LOGGER.trace("<addEntry " + added);
        return added;
    }

    @Deprecated
    public FeedItem getFeedItemByRawId(String rawId) {
        return runSingleQuery(new FeedItemRowConverter(), psGetItemsByRawId, rawId);
    }

    @Override
    public FeedItem getFeedItemByRawId(int feedId, String rawId) {
        return runSingleQuery(new FeedItemRowConverter(), psGetItemsByRawId2, feedId, rawId);
    }

    public FeedItem getFeedItemById(int id) {
        return runSingleQuery(new FeedItemRowConverter(), psGetItemById, id);
    }

    /**
     * Get the specified count of feed items, starting at offset.
     * 
     * @param limit
     * @param offset
     * @return
     */
    public List<FeedItem> getFeedItems(int limit, int offset) {
        return runQuery(new FeedItemRowConverter(), psGetItems, limit, offset);
    }

    /**
     * Get FeedItems by using a custom SQL query. The SELECT part must contain all appropriate columns with their
     * names from the feed_items table.
     * 
     * @param sqlQuery
     * @return
     */
    @Override
    public List<FeedItem> getFeedItems(String sqlQuery) {
        return runQuery(new FeedItemRowConverter(), sqlQuery);
    }

    public List<FeedItem> getFeedItemsForEvaluation(String sqlQuery) {
        RowConverter<FeedItem> converter = new RowConverter<FeedItem>() {

            @Override
            public FeedItem convert(ResultSet resultSet) throws SQLException {
                FeedItem entry = new FeedItem();

                entry.setId(resultSet.getInt("id"));
                entry.setFeedId(resultSet.getInt("feedId"));
                entry.setTitle(resultSet.getString("title"));
                entry.setLink(resultSet.getString("link"));
                entry.setRawId(resultSet.getString("rawId"));
                entry.setPublished(resultSet.getTimestamp("published"));
                entry.setItemText(resultSet.getString("text"));
                entry.setPageText(resultSet.getString("pageText"));
                entry.setAdded(resultSet.getTimestamp("added"));
                entry.putFeature("relevant", resultSet.getFloat("relevant"));

                return entry;
            }

        };
        return runQuery(converter, sqlQuery);
    }

    public void deleteFeedItemById(int id) {
        runUpdate(psDeleteItemById, id);
    }

    public ResultIterator<FeedItem> getFeedItems() {
        return runQueryWithIterator(new FeedItemRowConverter(), psGetAllItems);
    }

    public void clearFeedTables() {
        runUpdate("TRUNCATE TABLE feeds");
        runUpdate("TRUNCATE TABLE feed_items");
        runUpdate("TRUNCATE TABLE feeds_post_distribution");
        runUpdate("TRUNCATE TABLE feed_evaluation_polls");
    }

}