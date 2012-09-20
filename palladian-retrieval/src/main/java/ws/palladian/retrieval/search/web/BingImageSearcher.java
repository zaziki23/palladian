package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Bing Image search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BingImageSearcher extends BaseBingSearcher<WebImageResult> {

    /**
     * @see BaseBingSearcher#BaseBingSearcher(String)
     */
    public BingImageSearcher(String accountKey) {
        super(accountKey);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher(Configuration)
     */
    public BingImageSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    public String getName() {
        return "Bing Images";
    }

    @Override
    protected WebImageResult parseResult(JSONObject currentResult) throws JSONException {
        String pageUrl = currentResult.getString("SourceUrl");
        String imageUrl = currentResult.getString("MediaUrl");
        int width = currentResult.getInt("Width");
        int height = currentResult.getInt("Height");
        String title = currentResult.getString("Title");
        return new WebImageResult(pageUrl, imageUrl, title, null, width, height, null, null);
    }

    @Override
    protected String getSourceType() {
        return "Image";
    }

    @Override
    protected int getDefaultFetchSize() {
        return 25;
    }

}
