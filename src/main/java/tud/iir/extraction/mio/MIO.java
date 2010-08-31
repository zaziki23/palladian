/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Entity;

/**
 * An interactive multimedia object.
 * 
 * @author Martin Werner
 */
public class MIO {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(MIO.class);

    /** The trust. */
    private double trust = 0;

    /** The ml trust. */
    private double mlTrust = 0;

    /** The MIO type. */
    private transient String mioType = "";

    /** The find page URL. */
    private String findPageURL = "";

    /** The direct URL. */
    private String directURL = "";

    /** The file name. */
    private String fileName = "";

    /** The file size. */
    private double fileSize = 0;

    /** The text content length. */
//    private double textContentLength = 0;

    /** The entity. */
    private Entity entity;

    /** The interactivity grade. */
    private String interactivityGrade = "unclear";

    /** The is dedicated page. */
    private boolean isDedicatedPage = true;

    /** The infos. */
    private Map<String, List<String>> infos;

    /** The features. */
    private Map<String, Double> features;

    /**
     * Instantiates a new MIO.
     * 
     * @param mioType the MIOtype
     * @param directURL the directLinkURL
     * @param findPageURL the find page URL
     * @param entity the entity
     */
    public MIO(final String mioType, final String directURL, final String findPageURL, final Entity entity) {

        this.features = new HashMap<String, Double>();
        this.mioType = mioType;
        this.findPageURL = findPageURL;
        this.entity = entity;
        this.directURL = directURL;
        this.fileName = extractFileName(directURL, mioType);

        infos = new HashMap<String, List<String>>();
    }
    
    /**
     * Initialize features.
     */
    public void initializeFeatures(){
        
        features.put("FileNameRelevance", 0.);
        features.put("FilePathRelevance", 0.);
        features.put("BadWordAbsence", 1.);
        features.put("ALTTextRelevance", 0.);
        features.put("HeadlineRelevance", 0.);
        features.put("SurroundingTextRelevance", 0.);
//        features.put("XMLFileNameRelevance", 0.);
//        features.put("XMLFileContentRelevance", 0.);
        features.put("TitleRelevance", 0.);
        features.put("LinkNameRelevance", 0.);
        features.put("LinkTitleRelevance", 0.);
        features.put("IFrameParentRelevance", 0.);
        features.put("PageURLRelevance", 0.);
        features.put("DedicatedPageTrustRelevance", 0.);
        features.put("TextContentRelevance", 0.);
        features.put("ResolutionRelevance", 0.);
        
        
    }

    /**
     * Extract file name.
     * 
     * @param directURL the directLinkURL
     * @param mioType the mio type
     * @return the string
     */
    private String extractFileName(final String directURL, final String mioType) {

        String fileEnding = "";

        if ("flash".equalsIgnoreCase(mioType)) {
            fileEnding = "swf";
        } else {
            if ("applet".equalsIgnoreCase(mioType)) {
                fileEnding = "class";
            } else {
                if ("silverlight".equalsIgnoreCase(mioType)) {
                    fileEnding = "xap";
                } else {
                    if ("quicktime".equalsIgnoreCase(mioType)) {
                        fileEnding = "mov";
                    }
                }
            }
        }

        if (!("").equals(fileEnding)) {

            final String regExp = "[/=]?.[^/=]*\\." + fileEnding;
            Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            final Matcher matcher = pattern.matcher(directURL);
            while (matcher.find()) {
                return matcher.group(0);
            }
        }
        return directURL;
    }

    /**
     * Resets the MIOInfos (for saving memory).
     */
    public void resetMIOInfos() {
        infos = new HashMap<String, List<String>>();
    }

    /**
     * Gets the trust.
     * 
     * @return the trust
     */
    public double getTrust() {
        return trust;
    }

    /**
     * Sets the trust.
     * 
     * @param trust the new trust
     */
    public void setTrust(final double trust) {
        this.trust = trust;
    }

    /**
     * Gets the find page URL.
     * 
     * @return the find page URL
     */
    public String getFindPageURL() {
        return findPageURL;
    }

    /**
     * Sets the find page URL.
     * 
     * @param findPageURL the new find page URL
     */
    public void setFindPageURL(final String findPageURL) {
        this.findPageURL = findPageURL;
    }

    /**
     * Gets the direct URL.
     * 
     * @return the direct URL
     */
    public String getDirectURL() {
        return directURL;
    }

    /**
     * Sets the direct url.
     * 
     * @param directURL the new direct url
     */
    public void setDirectURL(final String directURL) {
        this.directURL = directURL;
    }

    /**
     * Gets the entity.
     * 
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Sets the entity.
     * 
     * @param entity the new entity
     */
    public void setEntity(final Entity entity) {
        this.entity = entity;
    }

    /**
     * Gets the interactivity grade.
     * 
     * @return the interactivity grade
     */
    public String getInteractivityGrade() {
        return interactivityGrade;
    }

    /**
     * Sets the interactivity grade.
     * 
     * @param interactivityGrade the new interactivity grade
     */
    public void setInteractivityGrade(final String interactivityGrade) {
        this.interactivityGrade = interactivityGrade;
    }

    /**
     * Checks if is dedicated page.
     * 
     * @return true, if is dedicated page
     */
    public boolean isDedicatedPage() {
        return isDedicatedPage;
    }

    /**
     * Sets the dedicated page.
     * 
     * @param isDedicatedPage the new dedicated page
     */
    public void setDedicatedPage(final boolean isDedicatedPage) {
        this.isDedicatedPage = isDedicatedPage;
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public String getMIOType() {
        return mioType;
    }

    /**
     * Sets the type.
     * 
     * @param type the new type
     */
    public void setMIOType(final String type) {
        this.mioType = type;
    }

    /**
     * Gets the infos.
     * 
     * @return the infos
     */
    public Map<String, List<String>> getInfos() {
        return infos;
    }

    /**
     * Sets specific infos like surroundingText needed for feature calculation.
     * 
     * @param infos the infos
     */
    public void setInfos(final Map<String, List<String>> infos) {
        this.infos = infos;
    }

    /**
     * Adds the specific info like surroundingText needed for feature calculation.
     * 
     * @param infoName the info name
     * @param infoList the info list
     */
    public void addInfos(final String infoName, final List<String> infoList) {
        infos.put(infoName, infoList);
    }

    /**
     * Gets the file name.
     * 
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the file name.
     * 
     * @param fileName the new file name
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * Sets the feature.
     * 
     * @param name the name
     * @param value the value
     */
    public void setFeature(final String name, final double value) {
        features.put(name, value);
    }

    /**
     * Gets the feature.
     * 
     * @param name the name
     * @return the feature
     */
    public double getFeature(final String name) {
        double result = 0;
        try {
            result = features.get(name);
        } catch (Exception e) {
            LOGGER.info("getFeature for: " + name + " failed!");
        }
        return result;
    }

    /**
     * Gets the features.
     * 
     * @return the features
     */
    public Map<String, Double> getFeatures() {
        return features;
    }

    /**
     * Gets the ml trust.
     * 
     * @return the ml trust
     */
    public double getMlTrust() {
        return mlTrust;
    }

    /**
     * Sets the ml trust.
     * 
     * @param mlTrust the new ml trust
     */
    public void setMlTrust(final double mlTrust) {
        this.mlTrust = mlTrust;
    }

    /**
     * Gets the file size.
     * 
     * @return the file size
     */
    public double getFileSize() {
        return fileSize;
    }

    /**
     * Sets the file size.
     * 
     * @param fileSize the new file size
     */
    public void setFileSize(final double fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * Gets the text content length.
     * 
     * @return the text content length
     */
//    public double getTextContentLength() {
//        return textContentLength;
//    }

    /**
     * Sets the text content length.
     * 
     * @param textContentLength the new text content length
     */
//    public void setTextContentLength(final double textContentLength) {
//        this.textContentLength = textContentLength;
//    }

    /**
     * Sets the features.
     * 
     * @param features the features
     */
    public void setFeatures(final Map<String, Double> features) {
        this.features = features;
    }

}
