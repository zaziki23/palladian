package ws.palladian.retrieval.ranking.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for likes, shares and comments on Facebook.
 * </p>
 * 
 * @author Julien Schmehl
 */
public class FacebookLinkStats extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(FacebookLinkStats.class);

    private static final String FQL_QUERY = "https://api.facebook.com/method/fql.query?format=json&query=select+total_count,like_count,comment_count,share_count+from+link_stat+where+";

    /** The id of this service. */
    private static final String SERVICE_ID = "facebook";

    /** The ranking value types of this service **/
    public static final RankingType LIKES = new RankingType("facebook_likes", "Facebook Likes",
            "The number of times Facebook users have \"Liked\" the page, or liked any comments or re-shares of this page.");

    public static final RankingType SHARES = new RankingType("facebook_shares", "Facebook Shares",
            "The number of times users have shared the page on Facebook.");

    public static final RankingType COMMENTS = new RankingType("facebook_comments", "Facebook Comments",
            "The number of comments users have made on the shared story.");
    
    private static final List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(LIKES);
        RANKING_TYPES.add(SHARES);
        RANKING_TYPES.add(COMMENTS);
    }

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public FacebookLinkStats() {
        super();
    }

    @Override
    public Ranking getRanking(String url) {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        try {

            String encUrl = UrlHelper.urlEncode(url);
            JSONObject json = null;
            try {
                HttpResult httpResult = retriever.httpGet(FQL_QUERY + "url='" + encUrl + "'");
                System.err.println(new String(httpResult.getContent()));
                JSONArray jsonArray = new JSONArray(new String(httpResult.getContent()));
                if (jsonArray.length() == 1) {
                    json = jsonArray.getJSONObject(0);
                }
            } catch (HttpException e) {
                LOGGER.error(e);
            }
            if (json != null) {
                results.put(LIKES, (float) json.getInt("like_count"));
                results.put(SHARES, (float) json.getInt("share_count"));
                results.put(COMMENTS, (float) json.getInt("comment_count"));
                LOGGER.trace("Facebook link stats for " + url + " : " + results);
            } else {
                results.put(LIKES, null);
                results.put(SHARES, null);
                results.put(COMMENTS, null);
                LOGGER.trace("Facebook link stats for " + url + "could not be fetched");
                checkBlocked();
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        }
        return ranking;
    }

    @Override
    public Map<String, Ranking> getRanking(List<String> urls) {

        Map<String, Ranking> results = new HashMap<String, Ranking>();
        if (isBlocked()) {
            return results;
        }
        String encUrls = "";

        try {

            for (int i = 0; i < urls.size(); i++) {
                if (i == urls.size() - 1) {
                    encUrls += "url='" + UrlHelper.urlEncode(urls.get(i)) + "'";
                } else {
                    encUrls += "url='" + UrlHelper.urlEncode(urls.get(i)) + "' or ";
                }
            }

            HashMap<String, String> postData = new HashMap<String, String>();
            postData.put("format", "json");
            postData.put("query", "select total_count,like_count,comment_count,share_count from link_stat where "
                    + encUrls);

            HttpResult response = retriever.httpPost("https://api.facebook.com/method/fql.query", postData);
            String content = new String(response.getContent());
            JSONArray json = null;
            if (content.length() > 0) {
                try {
                    json = new JSONArray(content);
                } catch (JSONException e) {
                    LOGGER.error("JSONException: " + e.getMessage());
                }
            }

            Timestamp retrieved = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());

            if (json != null) {

                float likeCount = -1;
                float shareCount = -1;
                float commentCount = -1;

                for (int i = 0; i < urls.size(); i++) {
                    likeCount = json.getJSONObject(i).getInt("like_count");
                    shareCount = json.getJSONObject(i).getInt("share_count");
                    commentCount = json.getJSONObject(i).getInt("comment_count");
                    Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                    result.put(LIKES, likeCount);
                    result.put(SHARES, shareCount);
                    result.put(COMMENTS, commentCount);
                    results.put(urls.get(i), new Ranking(this, urls.get(i), result, retrieved));
                    LOGGER.trace("Facebook link stats for " + urls.get(i) + " : " + result);
                }
            } else {
                for (String u : urls) {
                    Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                    result.put(LIKES, null);
                    result.put(SHARES, null);
                    result.put(COMMENTS, null);
                    results.put(u, new Ranking(this, u, result, retrieved));
                }
                LOGGER.trace("Facebook link stats for " + urls + "could not be fetched");
                checkBlocked();
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }

        return results;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            status = retriever.httpGet(FQL_QUERY + "url='http://www.google.com/'").getStatusCode();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }
        if (status == 200) {
            blocked = false;
            lastCheckBlocked = new Date().getTime();
            return false;
        }
        blocked = true;
        lastCheckBlocked = new Date().getTime();
        LOGGER.error("Facebook Ranking Service is momentarily blocked. Will check again in 1min.");
        return true;
    }

    @Override
    public boolean isBlocked() {
        if (new Date().getTime() - lastCheckBlocked < checkBlockedIntervall) {
            return blocked;
        } else {
            return checkBlocked();
        }
    }

    @Override
    public void resetBlocked() {
        blocked = false;
        lastCheckBlocked = new Date().getTime();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

}
