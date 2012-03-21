package ws.palladian.classification.language;

import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.UrlHelper;

/**
 * <p>
 * The WebKnoxLangDetect wraps the PalladianLangDetect and offers the service over a REST API. See here
 * http://webknox.com/api#!/text/language_GET.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class WebKnoxLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WebKnoxLangDetect.class);

    private final String appId;
    private final String apiKey;

    public WebKnoxLangDetect(String appId, String apiKey) {
        this.appId = appId;
        this.apiKey = apiKey;
    }

    public WebKnoxLangDetect(Configuration configuration) {
        this.appId = configuration.getString("api.webknox.appId");
        this.apiKey = configuration.getString("api.webknox.apiKey");
    }

    @Override
    public String classify(String text) {

        DocumentRetriever retriever = new DocumentRetriever();
        String url = "http://webknox.com/api/text/language?text=";
            url += UrlHelper.urlEncode(text);
        url += "&appId=" + appId;
        url += "&apiKey=" + apiKey;
        JSONArray result = retriever.getJsonArray(url);

        String answer = "";
        try {
            answer = result.getJSONObject(0).getString("language");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }

        return answer;
    }

    public static void main(String[] args) throws IOException {

        WebKnoxLangDetect webKnoxLangDetect = new WebKnoxLangDetect(ConfigHolder.getInstance().getConfig());
        System.out.println(webKnoxLangDetect.classify("Dies ist ein ganz deutscher Text, soviel ist klar"));

    }

}