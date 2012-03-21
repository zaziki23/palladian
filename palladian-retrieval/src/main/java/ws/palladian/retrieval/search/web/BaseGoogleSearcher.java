package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.UrlHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Base implementation for all Google searchers. Subclasses must implement {@link #getBaseUrl()}, which provides the URL
 * to the API endpoint and {@link #parseResult(JSONObject)}, which is responsible for parsing the JSONObject for each
 * result to the desired type ({@link WebResult} or subclasses).
 * </p>
 * 
 * @see http://code.google.com/intl/de/apis/websearch/docs/reference.html
 * @author Philipp Katz
 */
abstract class BaseGoogleSearcher<R extends WebResult> extends WebSearcher<R> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaseGoogleSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /**
     * <p>
     * Creates a new Google searcher.
     * </p>
     */
    public BaseGoogleSearcher() {
        super();
    }

    @Override
    public List<R> search(String query, int resultCount, Language language) throws SearcherException {

        List<R> webResults = new ArrayList<R>();

        // the number of pages we need to check; each page returns 8 results
        int necessaryPages = (int) Math.ceil(resultCount / 8.);

        try {
            for (int i = 0; i < necessaryPages; i++) {

                int offset = i * 8;
                JSONObject responseData = getResponseData(query, language, offset);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                // in the first iteration find the maximum of available pages and limit the search to those
                if (i == 0) {
                    int availablePages = getAvailablePages(responseData);
                    if (necessaryPages > availablePages) {
                        necessaryPages = availablePages;
                    }
                }

                JSONArray results = responseData.getJSONArray("results");
                for (int j = 0; j < results.length(); j++) {
                    JSONObject resultJson = results.getJSONObject(j);
                    R webResult = parseResult(resultJson);
                    webResults.add(webResult);
                    if (webResults.size() >= resultCount) {
                        break;
                    }
                }
            }
        } catch (HttpException e) {
            throw new SearcherException("HTTP exception while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ": " + e.getMessage(), e);
        }

        LOGGER.debug("google requests: " + TOTAL_REQUEST_COUNT.get());
        return webResults;
    }

    /**
     * Return the base URL for accessing this specific search.
     * 
     * @return
     */
    protected abstract String getBaseUrl();

    private String getRequestUrl(String query, Language language, int start) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(getBaseUrl());
        queryBuilder.append("?v=1.0");
        if (start > 0) {
            queryBuilder.append("&start=").append(start);
        }
        // rsz=large will respond 8 results
        queryBuilder.append("&rsz=large");
        queryBuilder.append("&safe=off");
        if (language != null) {
            queryBuilder.append("&lr=").append(getLanguageString(language));
        }
        queryBuilder.append("&q=").append(UrlHelper.urlEncode(query));
        return queryBuilder.toString();
    }

    private JSONObject getResponseData(String query, Language language, int offset) throws HttpException,
            JSONException {
        String requestUrl = getRequestUrl(query, language, offset);
        HttpResult httpResult = retriever.httpGet(requestUrl);
        String jsonString = new String(httpResult.getContent());
        JSONObject jsonObject = new JSONObject(jsonString);
        return jsonObject.getJSONObject("responseData");
    }

    /**
     * Get the string representation for the desired language.
     * 
     * @see http://www.google.com/cse/docs/resultsxml.html#languageCollections
     * @param language
     * @return
     */
    private String getLanguageString(Language language) {
        switch (language) {
            case GERMAN:
                return "lang_de";
        }
        return "lang_en";
    }

    /**
     * Parse the number of available pages from the responseData object.
     * 
     * @param responseData
     * @return
     * @throws JSONException
     */
    private int getAvailablePages(JSONObject responseData) throws JSONException {
        int availablePages = -1;
        if (responseData.getJSONObject("cursor") != null) {
            JSONObject cursor = responseData.getJSONObject("cursor");
            if (cursor.getJSONArray("pages") != null) {
                JSONArray pages = cursor.getJSONArray("pages");
                availablePages = pages.length();
            }
        }
        return availablePages;
    }

    /**
     * Parse the number of estimed results from the responseData object.
     * 
     * @param responseData
     * @return
     * @throws JSONException
     */
    private int getResultCount(JSONObject responseData) throws JSONException {
        int resultCount = -1;
        if (responseData.getJSONObject("cursor") != null) {
            JSONObject cursor = responseData.getJSONObject("cursor");
            if (cursor.has("estimatedResultCount")) {
                resultCount = cursor.getInt("estimatedResultCount");
            }
        }
        return resultCount;
    }

    /**
     * Parse one result object from JSON to an instance of {@link WebResult}.
     * 
     * @param resultData
     * @return
     * @throws JSONException
     */
    protected abstract R parseResult(JSONObject resultData) throws JSONException;

    @Override
    public int getTotalResultCount(String query) throws SearcherException {
        int hitCount = 0;
        try {
            JSONObject responseData = getResponseData(query, null, 0);
            hitCount = getResultCount(responseData);
        } catch (HttpException e) {
            throw new SearcherException("HTTP exception while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ": " + e.getMessage(), e);
        }
        return hitCount;
    }

    /**
     * Gets the number of HTTP requests sent to Google.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }
}