package tud.iir.knowledge;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.ExtractionType;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.extraction.XPathSet;
import tud.iir.extraction.entity.ListDiscoverer;
import tud.iir.helper.StringHelper;
import tud.iir.news.PageContentExtractor;
import tud.iir.news.PageContentExtractorException;
import tud.iir.web.Crawler;

import com.temesoft.google.pr.JenkinsHash;

/**
 * A source from which an extraction was performed.
 * 
 * @author David Urbansky
 * @author Christopher Friedrich
 */
public class Source implements Serializable {

    private static final long serialVersionUID = 958602024883353847L;

    // TODO duplicate content (e.g. copied wikipedia articles) have strong bias
    private int id = -1;
    
    /** the fact value the source belongs to */
    private FactValue factValue; 
    //private Entity entity; // which entity has been found at this source
    
    /** the URL of the source */
    private String url;
    
    /** how much can the source be trusted in giving correct information */
    private double trust = 0.5;
    
    /** determines how the extraction was performed */
    private int extractionType = ExtractionType.UNKNOWN;

    private int pageRank = -1;
    private String mainContent;

    public Source(String url, double trust, int extractionType) {
        this.setUrl(url);
        this.setTrust(trust);
        this.setExtractionType(extractionType);
    }

    public Source(String url, int extractionType) {
        this.setUrl(url);
        this.setExtractionType(extractionType);
    }

    public Source(String url, double trust) {
        this.setUrl(url);
        this.setTrust(trust);
    }

    public Source(String url) {
        this.setUrl(url);
    }

    public FactValue getFactValue() {
        return factValue;
    }

    public void setFactValue(FactValue factValue) {
        this.factValue = factValue;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null) {
            Logger.getRootLogger().error("source url was set with NULL value");
            new Error().printStackTrace();
            System.exit(1);
        }
        this.url = url;
    }

    public double getTrust() {
        switch (ExtractionProcessManager.getTrustFormula()) {
            case ExtractionProcessManager.QUANTITY_TRUST:
                return getTrust1();
            case ExtractionProcessManager.SOURCE_TRUST:
                return getTrust2();
            case ExtractionProcessManager.EXTRACTION_TYPE_TRUST:
                return getTrust2();
            case ExtractionProcessManager.COMBINED_TRUST:
                return getTrust3();
            case ExtractionProcessManager.CROSS_TRUST:
                return getTrust3();
        }

        return trust;
    }

    private double getTrust1() {
        return trust;
    }

    /**
     * Do not count trust from the same page several times (e.g. in forums many repetitions...).
     * 
     * @return The absolute applicability of the source.
     */
    private double getTrust2() {
        double numberOfDifferentSources = getNumberOfDifferentValues();
        if (numberOfDifferentSources == 0.0)
            return this.trust;
        return (this.trust / numberOfDifferentSources);
    }

    private double getTrust3() {
        double numberOfDifferentValues = getNumberOfDifferentValues();
        if (numberOfDifferentValues == 0.0)
            return this.trust;

        // exponential: applicability(D) = S/D
        // return ExtractionType.getTrust(this.getExtractionType())*(getNumberOfSameValues() * this.trust / numberOfDifferentValues);

        // linear: applicability(D) = |5-D| / 4
        // return ExtractionType.getTrust(this.getExtractionType())*(Math.max(0, 5 - numberOfDifferentValues) * this.trust / 4);

        // logarithmic: applicability(D) = 1 - 2^(x-4)
        // return ExtractionType.getTrust(this.getExtractionType()) * (this.trust * Math.max(1,(1 - Math.pow(2, numberOfDifferentValues - 4))));

        // exponential: applicability(D) = 1/D
        return ExtractionType.getTrust(this.getExtractionType()) * (this.trust / numberOfDifferentValues);

        // cut: applicability(D) = 1 if D = 1 else 0
        // if (numberOfDifferentValues == 1) return ExtractionType.getTrust(this.getExtractionType()) * (this.trust);
        // return 0.0;
    }

    public void setTrust(double trust) {
        this.trust = trust;
    }

    /**
     * Count the number of different fact values that have been extracted from this source for the fact. The more different values the less trust for one
     * certain value.
     * 
     * @return The number of different values.
     */
    private double getNumberOfDifferentValues() {
        HashSet<String> differentValues = new HashSet<String>();

        FactValue factValue = getFactValue();
        if (factValue == null)
            return 1;

        Fact fact = factValue.getFact();
        ArrayList<FactValue> factValues = fact.getValues(false);

        for (int i = 0, l = factValues.size(); i < l; i++) {
            FactValue currentFactValue = factValues.get(i);
            // if (currentFactValue == getFactValue()) continue;

            ArrayList<Source> sourceList = factValues.get(i).getSources();
            for (int j = 0, l2 = sourceList.size(); j < l2; j++) {
                Source currentSource = sourceList.get(j);
                if (currentSource == null || this.getUrl() == null)
                    continue;

                if (currentSource.getUrl().equalsIgnoreCase(this.getUrl())) {
                    differentValues.add(currentFactValue.getValue());
                    break;
                }
            }
        }

        return differentValues.size();
    }

    /**
     * Count the number of same fact values that have been extracted from this source for the fact. The more same values the more trust for one certain value.
     * 
     * @return The number of same values.
     */
    private double getNumberOfSameValues() {
        double sameValues = 0;

        FactValue factValue = getFactValue();
        if (factValue == null)
            return 1;

        ArrayList<Source> sourceList = factValue.getSources();
        for (int j = 0, l2 = sourceList.size(); j < l2; j++) {
            Source currentSource = sourceList.get(j);
            // Logger.getInstance().log(l2 + "current source"+currentSource.getUrl()+" =? "+ this.getUrl());
            if (currentSource.getUrl().equalsIgnoreCase(this.getUrl())) {
                sameValues++;
            }
        }

        return sameValues;
    }

    public int getExtractionType() {
        return extractionType;
    }

    public void setExtractionType(int extractionType) {
        this.extractionType = extractionType;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    /**
     * Retrieve the PageRank for the source URL from Google. Using the toolbarqueries.google.com endpoint.
     * 
     * @return Google Page Rank for source URL.
     * 
     * @author Christopher Friedrich
     */
    private static int getPageRank(String domain) {

        // TODO: optimize, by caching PR's in the DB

        int result = -1;
        JenkinsHash jHash = new JenkinsHash();

        String googlePrResult = "";

        long hash = jHash.hash(("info:" + domain).getBytes());

        String url = "http://toolbarqueries.google.com/search?client=navclient-auto&hl=en&" + "ch=6" + hash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:"
                + domain;

        try {
            URLConnection con;

            con = new URL(url).openConnection();

            InputStream is = con.getInputStream();
            byte[] buff = new byte[1024];
            int read = is.read(buff);
            while (read > 0) {
                googlePrResult = new String(buff, 0, read);
                read = is.read(buff);
            }
            googlePrResult = googlePrResult.split(":")[2].trim();
            result = new Long(googlePrResult).intValue();
        } catch (MalformedURLException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }

        return result;
    }

    /**
     * Get Google's PageRank for the source URL.
     * 
     * @return Google Page Rank for source URL.
     * 
     * @author Christopher Friedrich
     */
    public double getPageRank() {
        if (this.pageRank < 0) {
            this.pageRank = getPageRank(this.getUrl());
        }
        return this.pageRank;
    }

    /**
     * Get the main content block from the source URL page.
     * 
     * @return The main content string.
     * 
     * @author Christopher Friedrich
     */
    public String getMainContent() {
        if (mainContent == null) {
            // this.mainContent = getMainContentFromListDiscoverer();
            this.mainContent = getMainContentFromAlchemy();
        }

        return mainContent;
    }

    /**
     * Override the main content block for this object.
     * 
     * @author Christopher Friedrich
     */
    public void setMainContent(String mainContent) {
        this.mainContent = mainContent;
    }

    /**
     * Get the main content block from the source URL page using the AlchemyAPI web service.
     * 
     * @return The main content string.
     * 
     * @author Christopher Friedrich
     * @author David Urbansky
     */
    private String getMainContentFromAlchemy() {
        String result = null;

        try {
            PageContentExtractor e = new PageContentExtractor();
            result = e.setDocument(new URL(url)).getResultText();
        } catch (PageContentExtractorException e1) {
            Logger.getRootLogger().error(e1.getMessage());
        } catch (MalformedURLException e1) {
            Logger.getRootLogger().error(e1.getMessage());
        }
        // try {
        // AlchemyAPI alchemyAPI = new AlchemyAPI();
        // alchemyAPI.SetAPIKey("edc9bbd6f2107c7c11ddc42962eabcab1b8aef85");
        //
        // Document doc = alchemyAPI.URLGetText(url);
        // result = doc.getElementsByTagName("text").item(0).getTextContent();
        // } catch (Exception e) {
        //           
        // }

        return result;
    }

    /**
     * Get the main content block from the source URL page using the PageAnalyzer.
     * 
     * @return The main content string.
     * 
     * @author Christopher Friedrich
     */
    // TODO: needs more improvements before it can be used to obtain reasonable results
    /*private String getMainContentFromListDiscoverer() {
        Crawler c = new Crawler();
        Document document = c.getWebDocument(url);

        PageAnalyzer pa = new PageAnalyzer();
        pa.setDocument(document);

        ListDiscoverer ld = new ListDiscoverer();
        XPathSet xPathSet = ld.getXPathSet(document);
        xPathSet = ld.removeSiblingPagePaths(xPathSet, url, document);

        StringBuilder result = new StringBuilder();
        for (String xPathSetEntry : xPathSet.getXPathMap().keySet()) {
            String entry = StringHelper.trim(pa.getTextByXPath(xPathSetEntry));
            if (entry != null && !entry.isEmpty()) {
                // System.out.println("XXXXXXX: " + entry);
                result.append(entry);
            }
        }

        return result.toString();
    }*/

    /**
     * Get the top level domain (TLD) of the source URL.
     * 
     * @return The TLD of the source URL.
     * 
     * @author Christopher Friedrich
     */
    public String getTLD() {
        try {
            URL currentUrl = new URL(url);
            String host = currentUrl.getHost();
            return host.substring(host.lastIndexOf(".") + 1).toLowerCase();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        Source s = (Source) obj;
        if (s.getUrl().equalsIgnoreCase(getUrl()) && s.getExtractionType() == getExtractionType())
            return true;
        return false;
    }

}