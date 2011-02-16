package tud.iir.web.wiki.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

import tud.iir.persistence.RowConverter;
import tud.iir.web.wiki.data.WikiDescriptor;

public class WikiDescriptorRowConverter implements RowConverter<WikiDescriptor> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WikiDescriptorRowConverter.class);

    @Override
    public WikiDescriptor convert(ResultSet resultSet) throws SQLException {
        WikiDescriptor wd = new WikiDescriptor();
        wd.setWikiID(resultSet.getInt("wikiID"));
        wd.setWikiName(resultSet.getString("wikiName"));
        wd.setWikiURL(resultSet.getString("wikiURL"));
        wd.setPathToAPI(resultSet.getString("pathToAPI"));
        wd.setPathToContent(resultSet.getString("pathToContent"));

        String lastCheckSt = resultSet.getString("lastCheckNewPages");
        if (lastCheckSt != null && !lastCheckSt.equalsIgnoreCase("NULL")) {
            Date lastCheck = null;
            try {
                lastCheck = MediaWikiDatabase.convertSQLDateTimeToDate(lastCheckSt);
            } catch (Exception e) {
                LOGGER.error(
                        "Could not process the timestamp the wiki has been checked for new pages the last time. Wiki \""
                                + wd.getWikiName() + "\", timestamp: " + lastCheckSt + " ", e);
            }
            wd.setLastCheckForModifications(lastCheck);
        }
        wd.setCrawlerUserName(resultSet.getString("crawler_username"));
        wd.setCrawlerPassword(resultSet.getString("crawler_password"));
        return wd;
    }

}
