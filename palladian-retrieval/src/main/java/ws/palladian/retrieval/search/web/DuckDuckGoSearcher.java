package ws.palladian.retrieval.search.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.FixedIntervalRequestThrottle;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Web searcher scrapes HTML from <a href="http://duckduckgo.com/">DuckDuckGo</a> search results.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class DuckDuckGoSearcher extends AbstractSearcher<WebContent> {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DuckDuckGoSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /**
     * The number of entries which are returned for each page.
     */
    private static final int ENTRIES_PER_PAGE = 10;

    /**
     * Prevent over penetrating the searcher.
     */
    private static final FixedIntervalRequestThrottle THROTTLE = new FixedIntervalRequestThrottle(1000);

    /**
     * The JavaScript URL for the search results.
     */
    // private static final String JS_URL = "https://duckduckgo.com/d.js?q=%s&t=A&l=us-en&p=1&s=%s";
    private static final String HTML_URL = "https://duckduckgo.com/html/?q=%s";

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebContent> result = new ArrayList<>();

        DocumentRetriever documentRetriever = new DocumentRetriever();

        String requestUrl = String.format(HTML_URL, UrlHelper.encodeParameter(query));
        LOGGER.debug("Request URL = {}", requestUrl);

        Document document;
        try {
            THROTTLE.hold();
            document = documentRetriever.getWebDocument(requestUrl);
        } catch (Exception e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + " (request URL: \"" + requestUrl + "\"): " + e.getMessage(), e);
        }

        if (document == null) {
            return result;
        }

        List<Node> resultBodies = XPathHelper.getXhtmlNodes(document, "//div[contains(@class,'result__body')]");

        for (Node node : resultBodies) {
            BasicWebContent.Builder builder = new BasicWebContent.Builder();
            builder.setSummary(XPathHelper.getXhtmlNodeTextContent(node, ".//a[contains(@class,'result__snippet')]"));
            builder.setTitle(XPathHelper.getXhtmlNodeTextContent(node, ".//h2"));
            String url = XPathHelper.getXhtmlNodeTextContent(node, ".//h2/a/@href");
            builder.setUrl(url);
            if (!url.isEmpty()) {
                result.add(builder.create());
            }
        }

        return CollectionHelper.getSublist(result, 0, resultCount);
    }

    @Override
    public String getName() {
        return "DuckDuckGo";
    }

    /**
     * <p>
     * Gets the number of HTTP requests sent to DuckDuckGo.
     * </p>
     *
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }
}
