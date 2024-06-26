package ws.palladian.retrieval.ranking.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * <p>
 * Web tests for different {@link RankingService}s. These tests are run as integration tests.
 * </p>
 *
 * @author Philipp Katz
 */
@RunWith(Parameterized.class)
public class RankingServicesIT {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RankingServicesIT.class);

    private final RankingService rankingService;

    @Parameters(name = "{0}")
    public static Collection<Object[]> rankers() throws ConfigurationException, FileNotFoundException {
        Configuration configuration = loadConfiguration();
        List<Object[]> rankers = new ArrayList<>();
        rankers.add(new Object[]{new BibsonomyBookmarks(configuration)});
        rankers.add(new Object[]{new FacebookLinkStats(configuration)});
        rankers.add(new Object[]{new GoogleCachedPage()});
        rankers.add(new Object[]{new HackerNewsRankingService()});
        rankers.add(new Object[]{new PinterestPins()});
        rankers.add(new Object[]{new RedditStats()});
        return rankers;
    }

    private static Configuration loadConfiguration() throws ConfigurationException, FileNotFoundException {
        return new PropertiesConfiguration(ResourceHelper.getResourceFile("/palladian-test.properties"));
    }

    public RankingServicesIT(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * <p>
     * Check, that the request is processed in under 30 seconds and that no exceptions occur. We do no further checking
     * about the actual results, as this is to fragile.
     * </p>
     */
    @Test(timeout = 30000)
    public void testRanking() {
        LOGGER.info("testing " + rankingService.getServiceId());
        try {
            StopWatch stopWatch = new StopWatch();
            // Ranking result = rankingService.getRanking("http://global.nytimes.com");
            Ranking result = rankingService.getRanking("http://google.com");
            LOGGER.info("# results from " + rankingService.getServiceId() + ": " + result.getValues());
            LOGGER.info("retieval took " + stopWatch);
        } catch (RankingServiceException e) {
            LOGGER.error("Fail for " + rankingService.getServiceId() + ": " + e.getMessage(), e);
            fail();
        }
    }

}
