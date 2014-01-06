package ws.palladian.retrieval.wikipedia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlElement;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.CharStack;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * Utility functionality for working with <a href="http://www.mediawiki.org/">MediaWiki</a> markup.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Help:Wiki_markup">Wiki markup</a>
 * @author Philipp Katz
 */
public final class WikipediaUtil {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaUtil.class);

    private static final Pattern REF_PATTERN = Pattern.compile("<ref(?:\\s[^>]*)?>[^<]*</ref>|<ref[^/>]*/>",
            Pattern.MULTILINE);
    public static final Pattern HEADING_PATTERN = Pattern.compile("^={1,6}\\s*([^=]*)\\s*={1,6}$", Pattern.MULTILINE);
    private static final Pattern CONVERT_PATTERN = Pattern
            .compile("\\{\\{convert\\|([\\d.]+)\\|([\\w°]+)(\\|[^}]*)?\\}\\}");
    public static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile("\\[\\[([^|\\]]*)(?:\\|([^|\\]]*))?\\]\\]");
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("\\[http([^\\s]+)(?:\\s([^\\]]+))\\]");

    public static final Pattern REDIRECT_PATTERN = Pattern.compile("#redirect\\s*:?\\s*\\[\\[(.*)\\]\\]",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern OPEN_TAG_PATTERN = Pattern.compile("<\\w+[^>/]*>");
    private static final Pattern CLOSE_TAG_PATTERN = Pattern.compile("</\\w+[^>]*>");

    /**
     * matcher for coordinate template: {{Coord|47|33|27|N|10|45|00|E|display=title}}
     */
    private static final Pattern COORDINATE_TAG_PATTERN = Pattern.compile("\\{\\{Coord" + //
            // match latitude, either DMS or decimal, N/S is optional
            "\\|(-?\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?))?)?(?:\\|([NS]))?" +
            // ..-(1)--------------.......-(2)------------........-(3)---------------.........-(4)--
            // match longitude, either DMS or decimal, W/E is optional
            "\\|(-?\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?)(?:\\|(\\d+(?:\\.\\d+)?))?)?(?:\\|([WE]))?" +
            // ..-(5)--------------.......-(6)--------------......-(7)---------------.........-(8)--
            // additional data
            "((?:\\|[^}|<]+(?:<\\w+>[^<]*</\\w+>)?)*)" + //
            // -(9)----------------------------------
            "\\}\\}", Pattern.CASE_INSENSITIVE);//

    public static String stripMediaWikiMarkup(String markup) {
        Validate.notNull(markup, "markup must not be null");

        // strip everything in <ref> tags
        String result = REF_PATTERN.matcher(markup).replaceAll("");

        // resolve HTML entities
        result = StringEscapeUtils.unescapeHtml4(result);

        // remove HTML markup
        result = HtmlHelper.stripHtmlTags(result);

        // replace headlines
        result = HEADING_PATTERN.matcher(result).replaceAll("$1\n");

        // replace formatting
        result = result.replaceAll("'''''|'''|''", "");

        // replace {{convert|...}} tags
        result = CONVERT_PATTERN.matcher(result).replaceAll("$1 $2");

        // replace internal links
        result = processLinks(result, INTERNAL_LINK_PATTERN);
        result = processLinks(result, EXTERNAL_LINK_PATTERN);

        // remove everything left in between { ... } and [ ... ]
        // result = removeArea(result, '{', '}');
        result = removeBetween(result, '{', '{', '}', '}');
        result = removeBetween(result, '{', '|', '|', '}');

        // result = removeArea(result, '[', ']');
        // XXX replaced by RegEx, not sure if accurate
        result = result.replaceAll("\\[\\[[^]]*\\]\\]", "");

        // remove single line breaks; but keep lists (lines starting with *)
        result = result.replaceAll("(?<!\n)\n(?![*\n])", " ");

        // remove double whitespace and line breaks, trim
        result = StringHelper.removeDoubleWhitespaces(result);
        result = result.replaceAll("\n{2,}", "\n\n");
        result = result.trim();

        return result;
    }

    private static String processLinks(String string, Pattern linkPattern) {
        Matcher linkMatcher = linkPattern.matcher(string);
        StringBuffer buffer = new StringBuffer();
        while (linkMatcher.find()) {
            String target = linkMatcher.group(1);
            String text = linkMatcher.group(2);
            String replacement = StringUtils.EMPTY;
            // special treatment: ignore category: links completely for now
            if (!target.toLowerCase().startsWith("category:")) {
                replacement = text != null ? text : target;
            }
            linkMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Remove portions of text which are in between two opening and two closing characters. This is typically for texts
     * like {{abc}}.
     * 
     * @param string The text.
     * @param begin1 The first opening character.
     * @param begin2 The second opening character.
     * @param end1 The first closing character.
     * @param end2 The second closing character.
     * @return The text with parts in between opening/closing characters removed.
     */
    static String removeBetween(String string, char begin1, char begin2, char end1, char end2) {
        if (string.length() < 2) {
            return string;
        }
        // XXX some regex, iteratively applied, like "\\{\\{[^{}]*\\}\\}", might also work
        CharStack charStack = new CharStack();
        charStack.push(string.charAt(0));
        for (int idx = 1; idx < string.length(); idx++) {
            char previous = string.charAt(idx - 1);
            char current = string.charAt(idx);
            if (current == end2 && previous == end1) { // closing brackets
                while (charStack.size() > 1) {
                    // remove from stack until we found opening brackets
                    if (charStack.pop() == begin2 && charStack.peek() == begin1) {
                        charStack.pop();
                        // in case, closing brackets follow immediately, advance on the index by one
                        if (idx < string.length() - 1 && string.charAt(idx + 1) == end1) {
                            idx++;
                        }
                        break;
                    }
                }
            } else {
                charStack.push(current);
            }
        }
        return charStack.toString();
    }

    public static String extractSentences(String text) {
        // remove lines which do not contain a sentence and bulleted items
        Pattern pattern = Pattern.compile("^(\\*.*|.*\\w)$", Pattern.MULTILINE);
        String result = pattern.matcher(text).replaceAll("");
        result = result.replaceAll("\n{2,}", "\n\n");
        result = result.trim();
        return result;
    }

    /**
     * <p>
     * Retrieve a {@link WikipediaPage} directly from the web.
     * </p>
     * 
     * @param title The title of the article to retrieve, not <code>null</code> or empty. Escaping with underscores is
     *            done automatically.
     * @param language The langugae of the Wikipedia to check, not <code>null</code>.
     * @return The {@link WikipediaPage} for the given title, or <code>null</code> in case no article with that title
     *         was given.
     */
    public static final WikipediaPage retrieveArticle(String title, Language language) {
        // http://de.wikipedia.org/w/api.php?action=query&prop=revisions&rvlimit=1&rvprop=content&format=json&titles=Dresden
        String baseUrl = String.format("http://%s.wikipedia.org/w", language.getIso6391());
        return retrieveArticle(baseUrl, title);
    }
    
    /**
     * <p>
     * Retrieve a {@link WikipediaPage} directly from the web.
     * </p>
     * 
     * @param baseUrl The base URL of the API, e.g. <code>http://en.wikipedia.org/w</code>, not <code>null</code> or
     *            empty.
     * @param title The title to retrieve; will be escaped automatically.
     * @return The {@link WikipediaPage} for the given title, or <code>null</code> in case no article with that title
     *         was given.
     */
    public static final WikipediaPage retrieveArticle(String baseUrl, String title) {
        Validate.notEmpty(baseUrl, "baseUrl must not be empty");
        Validate.notEmpty(title, "title must not be empty");
        
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult httpResult;
        try {
            String escapedTitle = title.replace(" ", "_");
            escapedTitle = UrlHelper.encodeParameter(escapedTitle);
            String url = String.format("%s/api.php?action=query"
                    + "&prop=revisions&rvlimit=1&rvprop=content&format=json&titles=%s", baseUrl, escapedTitle);
            httpResult = retriever.httpGet(url);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        }

        String stringResult = httpResult.getStringContent();

        try {
            JsonObject jsonResult = new JsonObject(stringResult);
            JsonObject queryJson = jsonResult.getJsonObject("query");
            JsonObject pagesJson = queryJson.getJsonObject("pages");
            for (String key : pagesJson.keySet()) {
                JsonObject pageJson = pagesJson.getJsonObject(key);
                // System.out.println(pageJson);

                if (pageJson.containsKey("missing")) {
                    return null;
                }

                String pageTitle = pageJson.getString("title");
                int namespaceId = pageJson.getInt("ns");
                int pageId = pageJson.getInt("pageid");

                JsonArray revisionsJson = pageJson.getJsonArray("revisions");
                JsonObject firstRevision = revisionsJson.getJsonObject(0);
                String pageText = firstRevision.getString("*");
                return new WikipediaPage(pageId, namespaceId, pageTitle, pageText);
            }
            return null;
        } catch (JsonException e) {
            throw new IllegalStateException("Error while parsing the JSON: " + e.getMessage() + ", JSON='"
                    + stringResult + "'", e);
        }
    }

    /**
     * <p>
     * Retrieve {@link WikipediaPageReference}s in the specified category.
     * </p>
     * 
     * @param baseUrl The base URL of the Mediawiki API, not <code>null</code>.
     * @param categoryName The name of the category, must start with the <texttt> Category:</texttt> prefix, not
     *            <code>null</code>.
     * @return A list of {@link WikipediaPageReference}s in the specified category, or an empty list, never
     *         <code>null</code>.
     */
    public static final List<WikipediaPageReference> retrieveArticlesForCategory(String baseUrl, String categoryName) {
        String url = String.format(
                "%s/api.php?action=query&list=categorymembers&cmtitle=%s&cmsort=timestamp&cmdir=desc&format=json",
                baseUrl, categoryName);
        List<WikipediaPageReference> pages = CollectionHelper.newArrayList();
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(url);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        }
        try {
            JsonObject jsonResult = new JsonObject(httpResult.getStringContent());
            JsonArray resultArray = jsonResult.queryJsonArray("/query/categorymembers");
            for (int i = 0; i < resultArray.size(); i++) {
                JsonObject jsonEntry = resultArray.getJsonObject(i);
                int pageId = jsonEntry.getInt("pageid");
                int namespaceId = jsonEntry.getInt("ns");
                String title = jsonEntry.getString("title");
                pages.add(new WikipediaPageReference(pageId, namespaceId, title));
            }
        } catch (JsonException e) {
            throw new IllegalStateException("Error while parsing the JSON: " + e.getMessage() + ", JSON='"
                    + httpResult.getStringContent() + "'", e);
        }
        return pages;
    }
    
    /**
     * <p>
     * Retrieve a random article from the main namespace (ID 0).
     * </p>
     * 
     * @param baseUrl The base UR of the Mediawiki API, not <code>null</code>.
     * @return A {@link WikipediaPageReference} for a random article.
     */
    public static final WikipediaPageReference retrieveRandomArticle(String baseUrl) {
        Validate.notNull(baseUrl, "baseUrl must not be null");
        
        String url = String.format("%s/api.php?action=query&list=random&rnnamespace=0&format=json", baseUrl);
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(url);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        }
        try {
            JsonObject jsonResult = new JsonObject(httpResult.getStringContent());
            int pageId = jsonResult.queryInt("/query/random[0]/id");
            int namespaceId = jsonResult.queryInt("/query/random[0]/ns");
            String title = jsonResult.queryString("/query/random[0]/title");
            return new WikipediaPageReference(pageId, namespaceId, title);
        } catch (JsonException e) {
            throw new IllegalStateException("Error while parsing the JSON: " + e.getMessage() + ", JSON='"
                    + httpResult.getStringContent() + "'", e);
        }
    }

    /**
     * <p>
     * Extract key-value pairs from Wikipedia template markup.
     * </p>
     * 
     * @param markup The markup, not <code>null</code>.
     * @return A {@link Map} containing extracted key-value pairs from the template, entries in the map have the same
     *         order as in the markup. Entries without a key are are indexed by running numbers as strings (0,1,2…).
     * @see <a href="http://en.wikipedia.org/wiki/Help:Template">Help:Template</a>
     */
    static WikipediaTemplate extractTemplate(String markup) {
        Validate.notNull(markup, "markup must not be null");
        
        Map<String, String> properties = new LinkedHashMap<String, String>();
        // trim surrounding {{ and }}
        String content = markup.substring(2, markup.length() - 2);
        String templateName = getTemplateName(content);

        // in case of geobox, we must remove the first | character
        if (markup.toLowerCase().startsWith("{{geobox")) {
            content = markup.substring(markup.indexOf('|') + 1, markup.length() - 2);
        }

        int i = 0;
        for (String part : splitTemplateMarkup(content)) {
            String key = String.valueOf(i++);
            int equalIdx = part.indexOf('=');
            if (equalIdx > 0) {
                String potentialKey = part.substring(0, equalIdx);
                if (isBracketBalanced(potentialKey) && isTagBalanced(potentialKey)) {
                    key = part.substring(0, equalIdx).trim();
                } else {
                    equalIdx = -1;
                }
            }
            String value = part.substring(equalIdx + 1).trim();
            properties.put(key, value);
        }

        return new WikipediaTemplate(templateName, properties);
    }
    
    
    private static final String getTemplateName(String markup) {
        Pattern pattern = Pattern.compile("(?:geobox\\|)?[^|<}]+");
        Matcher matcher = pattern.matcher(markup.toLowerCase());
        return matcher.find() ? matcher.group().trim() : null;
    }

    private static final List<String> splitTemplateMarkup(String markup) {
        List<String> result = CollectionHelper.newArrayList();
        int startIdx = markup.indexOf('|') + 1;
        for (int currentIdx = startIdx; currentIdx < markup.length(); currentIdx++) {
            char currentChar = markup.charAt(currentIdx);
            String substring = markup.substring(0, currentIdx);
            if (currentChar == '|' && isBracketBalanced(substring)) {
                result.add(markup.substring(startIdx, currentIdx));
                startIdx = currentIdx + 1;
            }
        }
        result.add(markup.substring(startIdx));
        return result;
    }

    /**
     * Check, whether opening/closing brackets are in balance.
     * 
     * @param markup The markup.
     * @return <code>true</code>, if number of opening = number of closing characters.
     */
    private static final boolean isBracketBalanced(String markup) {
        // check the balance of bracket-like characters
        int open = markup.replace("{{", "").replace("[", "").replace("<", "").length();
        int close = markup.replace("}}", "").replace("]", "").replace(">", "").length();
        return open - close == 0;
    }

    private static final boolean isTagBalanced(String markup) {
        // check the balance of HTML tags
        int openTags = StringHelper.countRegexMatches(markup, OPEN_TAG_PATTERN);
        int closeTags = StringHelper.countRegexMatches(markup, CLOSE_TAG_PATTERN);
        return openTags == closeTags;
    }

    /**
     * <p>
     * Extract geographical data from Wikipedia page markup.
     * </p>
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Wikipedia:WikiProject_Geographical_coordinates">WikiProject
     *      Geographical coordinates</a>
     * @param text The markup, not <code>null</code>.
     * @return {@link List} of extracted {@link MarkupCoordinate}s, or an empty list, never <code>null</code>.
     */
    public static List<MarkupCoordinate> extractCoordinateTag(String text) {
        Validate.notNull(text, "text must not be null");
        List<MarkupCoordinate> result = CollectionHelper.newArrayList();
        Matcher m = COORDINATE_TAG_PATTERN.matcher(text);
        while (m.find()) {
            double lat = parseComponents(m.group(1), m.group(2), m.group(3), m.group(4));
            double lng = parseComponents(m.group(5), m.group(6), m.group(7), m.group(8));

            // get coordinate parameters
            String data = m.group(9);
            String type = getCoordinateParam(data, "type");
            Long population = null;
            if (type != null) {
                population = getNumberInBrackets(type);
                type = type.replaceAll("\\(.*\\)", ""); // remove population
            }
            String region = getCoordinateParam(data, "region");
            // get other parameters
            String display = getOtherParam(data, "display");
            String name = getOtherParam(data, "name");

            result.add(new MarkupCoordinate(lat, lng, name, population, display, type, region));
        }
        return result;
    }

    /**
     * <p>
     * Try to parse {@link GeoCoordinate}s in a given info box.
     * </p>
     * 
     * @param parsedTemplate The parsed template, not <code>null</code>.
     * @return Set with extracted {@link GeoCoordinate}s, or an empty list in case nothing could be extracted, never
     *         <code>null</code>.
     * @see #extractTemplate(String)
     */
    public static Set<MarkupCoordinate> extractCoordinatesFromInfobox(WikipediaTemplate infobox) {
        Validate.notNull(infobox, "parsedTemplate must not be null");
        Set<MarkupCoordinate> coordinates = CollectionHelper.newHashSet();
        
        String display = infobox.getEntry("coordinates_display");
        String type = infobox.getEntry("coordinates_type");

        // try lat/long_deg/min_sec
        try {
            String latDeg = infobox.getEntry("lat_deg", "latd", "lat_d", "lat_degrees", "source_lat_d", "mouth_lat_d");
            String lngDeg = infobox.getEntry("lon_deg", "longd", "long_d", "long_degrees", "source_long_d",
                    "mouth_long_d");
            if (StringUtils.isNotBlank(latDeg) && StringUtils.isNotBlank(lngDeg)) {
                String latMin = infobox.getEntry("lat_min", "latm", "lat_m", "lat_minutes", "source_lat_m",
                        "mouth_lat_m");
                String latSec = infobox.getEntry("lat_sec", "lats", "lat_s", "lat_seconds", "source_lat_s",
                        "mouth_lat_s");
                String lngMin = infobox.getEntry("lon_min", "longm", "long_m", "long_minutes", "source_long_m",
                        "mouth_long_m");
                String lngSec = infobox.getEntry("lon_sec", "longs", "long_s", "long_seconds", "source_long_s",
                        "mouth_long_s");
                String latNS = infobox.getEntry("latNS", "lat_direction", "lat_NS", "source_lat_NS", "mouth_lat_NS");
                String lngEW = infobox.getEntry("longEW", "long_direction", "long_EW", "source_long_EW",
                        "mouth_long_EW");
                double lat = parseComponents(latDeg, latMin, latSec, latNS);
                double lng = parseComponents(lngDeg, lngMin, lngSec, lngEW);
                coordinates.add(new MarkupCoordinate(lat, lng, display, type));
            }
        } catch (Exception e) {
            LOGGER.warn("Error while parsing: {}", e.getMessage());
        }

        // try all-in-one format
        String lat = infobox.getEntry("latitude");
        String lng = infobox.getEntry("longitude");
        if (StringUtils.isNotBlank(lat) && StringUtils.isNotBlank(lng)) {
            try {
                // try decimal format
                coordinates.add(new MarkupCoordinate(Double.valueOf(lat), Double.valueOf(lng), display, type));
            } catch (Exception e) {
                try {
                    // try DMS format
                    coordinates
                            .add(new MarkupCoordinate(GeoUtils.parseDms(lat), GeoUtils.parseDms(lng), display, type));
                } catch (Exception e1) {
                    // try decdeg markup
                    try {
                        coordinates.add(new MarkupCoordinate(WikipediaUtil.parseDecDeg(lat), WikipediaUtil
                                .parseDecDeg(lng), display, type));
                    } catch (Exception e2) {
                        LOGGER.warn("Error while parsing: {} and/or {}: {}", lat, lng, e.getMessage());
                    }
                }
            }
        }
        return coordinates;
    }

    private static Long getNumberInBrackets(String string) {
        Matcher matcher = Pattern.compile("\\(([\\d,]+)\\)").matcher(string);
        if (matcher.find()) {
            String temp = matcher.group(1).replace(",", "");
            try {
                return Long.valueOf(temp);
            } catch (NumberFormatException e) {
                LOGGER.error("Error parsing {}", temp);
            }
        }
        return null;
    }

    private static String getOtherParam(String group, String name) {
        String[] parts = group.split("\\|");
        for (String temp1 : parts) {
            String[] keyValue = temp1.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(name)) {
                return keyValue[1].trim();
            }
        }
        return null;
    }

    private static String getCoordinateParam(String group, String name) {
        String[] parts = group.split("\\|");
        for (String temp1 : parts) {
            for (String temp2 : temp1.split("_")) {
                String[] keyValue = temp2.split(":");
                if (keyValue.length == 2 && keyValue[0].equals(name)) {
                    return keyValue[1].trim();
                }
            }
        }
        return null;
    }

    /**
     * Parse DMS components. The only part which must not be <code>null</code> is deg.
     * 
     * @param deg Degree part, not <code>null</code> or empty.
     * @param min Minute part, may be <code>null</code>.
     * @param sec Second part, may be <code>null</code>.
     * @param nsew NSEW modifier, should be in [NSEW], may be <code>null</code>.
     * @return Parsed double value.
     */
    private static double parseComponents(String deg, String min, String sec, String nsew) {
        Validate.notEmpty(deg, "deg must not be null or empty");
        double parsedDeg = Double.valueOf(deg);
        double parsedMin = StringUtils.isNotBlank(min) ? Double.valueOf(min) : 0;
        double parsedSec = StringUtils.isNotBlank(sec) ? Double.valueOf(sec) : 0;
        int sgn = ("S".equals(nsew) || "W".equals(nsew)) ? -1 : 1;
        return sgn * (parsedDeg + parsedMin / 60. + parsedSec / 3600.);
    }
    
    /**
     * <p>
     * Get the content of markup area between double curly braces, like {{infobox …}}, {{quote …}}, etc.
     * </p>
     * 
     * @param markup The media wiki markup, not <code>null</code>.
     * @param name The name, like infobox, quote, etc.
     * @return The content in the markup, or an empty list of not found, never <code>null</code>.
     */
    public static List<String> getNamedMarkup(String markup, String... names) {
        List<String> result = CollectionHelper.newArrayList();
        int startIdx = 0;
        String cleanMarkup = HtmlHelper.stripHtmlTags(markup, HtmlElement.COMMENTS);
        String namesSeparated = StringUtils.join(names, "|").toLowerCase();
        Pattern pattern = Pattern.compile("\\{\\{(?:" + namesSeparated + ")(?:\\s|\\|)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(cleanMarkup);
        while (matcher.find()) {
            startIdx = matcher.start();
            int brackets = 0;
            int endIdx;
            for (endIdx = startIdx; startIdx < cleanMarkup.length(); endIdx++) {
                char current = cleanMarkup.charAt(endIdx);
                if (current == '{') {
                    brackets++;
                } else if (current == '}') {
                    brackets--;
                }
                if (brackets == 0) {
                    break;
                }
            }
            try {
                result.add(cleanMarkup.substring(startIdx, endIdx + 1));
            } catch (StringIndexOutOfBoundsException e) {
                LOGGER.warn("Encountered {}, potentially caused by invalid markup.");
            }
            startIdx = endIdx;
        }
        return result;
    }

    /**
     * <p>
     * Parse markup in {{decdeg|...}} element (decimal degrees for geographic coordinates). See <a
     * href="http://en.wikipedia.org/wiki/Template:Decdeg/sandbox">here</a> for more details.
     * </p>
     * 
     * @param docDegMarkup The markup, not <code>null</code>.
     * @return The double value with the coordinates.
     * @throws NumberFormatException in case the string could not be parsed.
     */
    static double parseDecDeg(String docDegMarkup) {
        Validate.notNull(docDegMarkup, "string must not be null");
        WikipediaTemplate templateData = extractTemplate(docDegMarkup);
        String degStr = templateData.getEntry("deg", "0");
        String minStr = templateData.getEntry("min", "1");
        String secStr = templateData.getEntry("sec", "2");
        String hem = templateData.getEntry("hem", "3");
        try {
            double deg = StringUtils.isNotBlank(degStr) ? Double.valueOf(degStr) : 0;
            double min = StringUtils.isNotBlank(minStr) ? Double.valueOf(minStr) : 0;
            double sec = StringUtils.isNotBlank(secStr) ? Double.valueOf(secStr) : 0;
            int sgn;
            if (StringUtils.isNotBlank(hem)) {
                sgn = "W".equals(hem) || "S".equals(hem) ? -1 : 1;
            } else {
                sgn = degStr.startsWith("-") ? -1 : 1;
            }
            double result = sgn * (Math.abs(deg) + min / 60. + sec / 3600.);
            String rndStr = templateData.getEntry("rnd", "4");
            if (StringUtils.isNotBlank(rndStr)) {
                int rnd = Integer.valueOf(rndStr);
                result = MathHelper.round(result, rnd);
            }
            return result;
        } catch (Exception e) {
            throw new NumberFormatException("The coordinate data from \"" + docDegMarkup + "\" could not be parsed.");
        }
    }

    private WikipediaUtil() {
        // leave me alone!
    }

    public static void main(String[] args) {
        WikipediaPageReference random = retrieveRandomArticle("http://en.wikipedia.org/w");
        System.out.println(random);
        System.exit(0);
        
        // System.out.println(getDoubleBracketBalance("{{xx{{{{"));
        // System.exit(0);
        // String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/newYork.wikipedia");
        // String wikipediaPage = FileHelper.readFileToString("/Users/pk/Desktop/sample2.wikipedia");
        // String text = stripMediaWikiMarkup(wikipediaPage);
        // System.out.println(text);

        // WikipediaPage page = getArticle("Mit Schirm, Charme und Melone (Film)", Language.GERMAN);
        WikipediaPage page = retrieveArticle("Charles River", Language.ENGLISH);
        WikipediaTemplate infoboxData = extractTemplate(getNamedMarkup(page.getMarkup(), "geobox").get(0));
        // CollectionHelper.print(infoboxData);
        System.out.println(infoboxData);
        System.out.println(page);

    }

}
