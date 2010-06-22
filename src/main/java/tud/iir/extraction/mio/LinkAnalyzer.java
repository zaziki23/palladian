package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The LinkAnalyzer checks if some of the Links of MIOPageCandidates have targets with MIOs (simulates an indirect search)
 * 
 * @author Martin Werner
 */
public class LinkAnalyzer extends GeneralAnalyzer {

    private SearchWordMatcher swMatcher;

    public LinkAnalyzer(SearchWordMatcher swMatcher) {
        this.swMatcher = swMatcher;
    }

    public List<MIOPage> getLinkedMioPages(String parentPageContent, String parentPageURL) {
        List<MIOPage> mioPages = new ArrayList<MIOPage>();

        // find all <a>-tags
        Pattern p = Pattern.compile("<a[^>]*href=\\\"?[^(>|)]*\\\"?[^>]*>[^<]*</a>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(parentPageContent);
        while (m.find()) {
            String completeLinkTag = m.group(0);

            String linkURL = getLinkURL(completeLinkTag, parentPageURL);
            if (!linkURL.equals("") && linkURL != null) {

                // extract the Text of a Link
                String linkName = getLinkName(completeLinkTag);

                // extract the value of the title-attribute of a link
                String linkTitle = getLinkTitle(completeLinkTag);

                // check if the linkURL or linkInfo or linkTitle contains entity
                // relevant words
                if (isRelevantLinkCheck(linkURL, linkName, linkTitle)) {
                    String linkedPageContent = getPage(linkURL, false);
                    if (!linkedPageContent.equals("")) {
                        FastMIODetector mioDetector = new FastMIODetector();
                        if (mioDetector.containsMIO(linkedPageContent)) {
                            MIOPage mioPage = generateMIOPage(linkURL, parentPageURL, linkName, linkTitle, linkedPageContent);
                            mioPages.add(mioPage);
                        }
                    }

                }
            }

        }

        return mioPages;
    }

    private String getLinkURL(String linkTag, String pageURL) {
        String extractedLink = extractElement("href=\"[^>#\"]*\"", linkTag, "href=");
        return verifyURL(extractedLink, pageURL);

    }

    private String getLinkName(String linkTag) {
        return extractElement(">[^<]*<", linkTag, "");
    }

    private String getLinkTitle(String linkTag) {
        return extractElement("title=\"[^>\"]*\"", linkTag, "title=");
    }

    /**
     * Check if the linkURL or linkInfo or linkTitle contains entity relevant words
     */
    private boolean isRelevantLinkCheck(String linkURL, String linkName, String linkTitle) {

        if (swMatcher.containsSearchWordOrMorphs(linkURL) || swMatcher.containsSearchWordOrMorphs(linkName) || swMatcher.containsSearchWordOrMorphs(linkTitle)) {
            return true;
        }

        // a string is relevant if a minimum of 2words (>1) of the given
        // SearchWord or morpheme is contained
        // if (swMatcher.getNumberOfSearchWordMatches(linkURL) > 1
        // || swMatcher.getNumberOfSearchWordMatches(linkName) > 1
        // || swMatcher.getNumberOfSearchWordMatches(linkTitle) > 1) {
        // return true;
        // }
        return false;
    }

    /**
     * Create a MIOPage
     */
    private MIOPage generateMIOPage(String linkURL, String parentURL, String linkName, String linkTitle, String pageContent) {

        MIOPage mioPage = new MIOPage(linkURL, pageContent);
        mioPage.setLinkParentPage(parentURL);
        mioPage.setLinkName(linkName);
        mioPage.setLinkTitle(linkTitle);
        mioPage.setLinkedPage(true);

        return mioPage;
    }

}
