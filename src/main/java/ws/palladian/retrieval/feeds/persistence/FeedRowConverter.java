package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedContentClassifier.FeedContentType;

public class FeedRowConverter implements RowConverter<Feed> {

    @Override
    public Feed convert(ResultSet resultSet) throws SQLException {
        
        Feed feed = new Feed();
        feed.setId(resultSet.getInt("id"));
        feed.setFeedUrl(resultSet.getString("feedUrl"));
        feed.setSiteUrl(resultSet.getString("siteUrl"));
        feed.setTitle(resultSet.getString("title"));
        feed.setContentType(FeedContentType.getByIdentifier(resultSet.getInt("textType")));
        feed.setLanguage(resultSet.getString("language"));
        feed.setAdded(resultSet.getTimestamp("added"));
        feed.setChecks(resultSet.getInt("checks"));
        // feed.setUpdateInterval(resultSet.getInt("minCheckInterval"));
        feed.setUpdateInterval(resultSet.getInt("maxCheckInterval"));
        feed.setNewestItemHash(resultSet.getString("newestItemHash"));
        feed.setUnreachableCount(resultSet.getInt("unreachableCount"));
        feed.setLastFeedEntry(resultSet.getTimestamp("lastFeedEntry"));
        feed.setActivityPattern(resultSet.getInt("activityPattern"));
        feed.setLMSSupport(resultSet.getBoolean("supportsLMS"));
        feed.setETagSupport(resultSet.getBoolean("supportsETag"));
        feed.setCgHeaderSize(resultSet.getInt("conditionalGetResponseSize"));
        feed.setLastPollTime(resultSet.getTimestamp("lastPollTime"));
        feed.setLastETag(resultSet.getString("lastEtag"));
        feed.setTotalProcessingTime(resultSet.getLong("totalProcessingTime"));
        feed.setMisses(resultSet.getInt("misses"));
        feed.setLastMissTime(resultSet.getTimestamp("lastMissTimestamp"));
        feed.setBlocked(resultSet.getBoolean("blocked"));
        feed.setLastSuccessfulCheckTime(resultSet.getTimestamp("lastSuccessfulCheck"));
        feed.setWindowSize(resultSet.getInt("windowSize"));
        feed.setVariableWindowSize(resultSet.getBoolean("hasVariableWindowSize"));
        feed.setNumberOfItemsReceived(resultSet.getInt("numberOfItems"));
        
        return feed;

    }

}
