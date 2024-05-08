package ws.palladian.retrieval.search;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.FormEncodedHttpEntity;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.resources.WebContent;

/**
 * Base class for Openverse searchers (image, audio).
 *
 * @author David Urbansky
 * @author Philipp Katz
 * @see <a href="https://api.openverse.org/v1/">Openverse API</a>
 */
public abstract class AbstractOpenverseSearcher<T extends WebContent> extends AbstractMultifacetSearcher<T> {

    public static abstract class AbstractOpenverseSearcherMetaInfo<T extends WebContent> implements SearcherMetaInfo<AbstractOpenverseSearcher<T>, T> {
        private static final StringConfigurationOption CLIENT_ID = new StringConfigurationOption("Client ID",
                "client_id", null, false);
        private static final StringConfigurationOption CLIENT_SECRET = new StringConfigurationOption("Client Secret",
                "client_secret", null, false);

        @Override
        public final List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(CLIENT_ID, CLIENT_SECRET);
        }

        @Override
        public final AbstractOpenverseSearcher<T> create(Map<ConfigurationOption<?>, ?> config) {
            var clientId = CLIENT_ID.get(config);
            var clientSecret = CLIENT_SECRET.get(config);
            return create(clientId, clientSecret);
        }

        protected abstract AbstractOpenverseSearcher<T> create(String clientId, String clientSecret);

    }

    // Maximum is 500 for authenticated requests, and 20 for unauthenticated
    // requests.
    private static final int MAX_PER_PAGE_UNAUTHENTICATED = 20;
    private static final int MAX_PER_PAGE_AUTHENTICATED = 500;

    private String licenses = "all-cc,commercial";

    /** If null, search all sources. */
    private String sources = null;

    private final String clientId;

    private final String clientSecret;

    protected AbstractOpenverseSearcher(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public final SearchResults<T> search(MultifacetQuery query) throws SearcherException {
        String accessToken = null;
        if (clientId != null && clientSecret != null) {
            accessToken = getAccessToken();
            // TODO cache this
        }

        List<T> results = new ArrayList<>();
        Long totalAvailableResults = null;

        var resultCount = Math.min(10000, query.getResultCount());
        int maxPerPage = accessToken != null ? MAX_PER_PAGE_AUTHENTICATED : MAX_PER_PAGE_UNAUTHENTICATED;
        int resultsPerPage = Math.min(maxPerPage, resultCount);
        int pagesNeeded = (int) Math.ceil(resultCount / (double) resultsPerPage);

        for (int page = 1; page <= pagesNeeded; page++) {
            String requestUrl = buildRequest(query.getText(), page, Math.min(maxPerPage, resultCount - results.size()));
            try {
                var requestBuilder = new HttpRequest2Builder(HttpMethod.GET, requestUrl);
                if (accessToken != null) {
                    requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
                }
                var jsonResponse = HttpRetrieverFactory.getHttpRetriever().execute(requestBuilder.create());
                if (jsonResponse.errorStatus()) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = new JsonObject(jsonResponse.getStringContent(StandardCharsets.UTF_8));
                if (totalAvailableResults == null) {
                    totalAvailableResults = json.tryGetLong("result_count");
                }
                JsonArray jsonArray = json.getJsonArray("results");
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject resultHit = jsonArray.getJsonObject(i);
                        results.add(parseResult(resultHit));
                        if (results.size() >= resultCount) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new SearcherException(e);
            }
        }

        return new SearchResults<>(results, totalAvailableResults);
    }

    protected abstract T parseResult(JsonObject json);

    private String getAccessToken() throws SearcherException {
        var requestBuilder = new HttpRequest2Builder(HttpMethod.POST,
                "https://api.openverse.org/v1/auth_tokens/token/");
        var formEntityBuilder = new FormEncodedHttpEntity.Builder();
        formEntityBuilder.addData("client_id", clientId);
        formEntityBuilder.addData("client_secret", clientSecret);
        formEntityBuilder.addData("grant_type", "client_credentials");
        requestBuilder.setEntity(formEntityBuilder.create());
        var request = requestBuilder.create();
        HttpResult response;
        try {
            response = HttpRetrieverFactory.getHttpRetriever().execute(request);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error", e);
        }
        if (response.errorStatus()) {
            throw new SearcherException(
                    "HTTP status " + response.getStatusCode() + " from token endpoint: " + response.getStringContent());
        }
        try {
            var jsonResponse = new JsonObject(response.getStringContent());
            return jsonResponse.getString("access_token");
        } catch (JsonException e) {
            throw new SearcherException("Could not parse JSON: " + response.getStringContent(), e);
        }
    }

    protected abstract String buildRequest(String searchTerms, int page, int resultsPerPage);

    public String getLicenses() {
        return licenses;
    }

    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

}
