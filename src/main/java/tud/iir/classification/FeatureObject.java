package tud.iir.classification;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import tud.iir.helper.CollectionHelper;

/**
 * An object holding features.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * 
 */
public class FeatureObject {

    /** The features. */
    private Double[] features;

    /** The feature names. */
    private String[] featureNames;

    /** The class association. */
    private int classAssociation;

    /**
     * Create a feature object with a feature vector of doubles. The last index of the features must be 0 or 1 and
     * refers to the class.
     * 
     * @param features the features
     * @param featureNames the feature names
     */
    public FeatureObject(final Double[] features, final String[] featureNames) {
        // setFeatures(features);
        this.features = features;
        // setFeatureNames(featureNames);
        this.featureNames = featureNames;
        // setClassAssociation((int) Math.floor((features[features.length - 1])));
        this.classAssociation = (int) Math.floor((features[features.length - 1]));
    }

    /**
     * Instantiates a new feature object.
     * 
     * @param features the features
     * @param classAssociation the class association
     */
    public FeatureObject(final Map<String, Double> features, final Double classAssociation) {
        this(features);
        // setClassAssociation((int) Math.floor(classAssociation));
        this.classAssociation = (int) Math.floor(classAssociation);
    }

    /**
     * Instantiates a new feature object.
     * 
     * @param features the features
     */
    public FeatureObject(final Map<String, Double> features) {

        String[] featureNames = new String[features.size()];
        Double[] featureValues = new Double[features.size()];

        int counter = 0;
        for (Entry<String, Double> feature : features.entrySet()) {
            featureNames[counter] = feature.getKey();
            featureValues[counter] = feature.getValue();
            counter++;
        }
        this.features = featureValues;
        // setFeatures(featureValues);
        this.featureNames = featureNames;
        // setFeatureNames(featureNames);

    }

    /**
     * Gets the features.
     * 
     * @return the features
     */
    public Double[] getFeatures() {
        return features;
    }

    /**
     * Sets the features.
     * 
     * @param features the new features
     */
    public void setFeatures(Double[] features) { // NOPMD by David on 14.06.10 01:02
        this.features = features;
    }

    /**
     * Gets the feature names.
     * 
     * @return the feature names
     */
    public String[] getFeatureNames() {
        return featureNames;
    }

    /**
     * Sets the feature names.
     * 
     * @param featureNames the featureNames as StringArray
     */
    public void setFeatureNames(final String[] featureNames) {

        if (featureNames.length < features.length) {
            this.featureNames = new String[features.length];
            for (int i = 0; i < features.length; i++) {

                this.featureNames[i] = "feature" + i;
            }
        } else {

            this.featureNames = featureNames;
        }
    }

    public int getClassAssociation() {
        return classAssociation;
    }

    /**
     * Gets the class association as string.
     * 
     * @return the class association as string
     */
    public String getClassAssociationAsString() {
        if (getClassAssociation() == 1.0) {
            return "positive";
        }
        return "negative";
    }

    /**
     * Sets the class association.
     * 
     * @param classAssociation the new class association
     */
    public void setClassAssociation(final int classAssociation) {
        this.classAssociation = classAssociation;
    }
    
    /**
     * Get a feature by its featureName.
     * 
     * @param featureName
     * @return value of the specified featureName, or <code>null</code> if no feature with specified name, or no
     *         featureNames specified at all.
     * @author Philipp Katz
     */
    public Double getFeature(String featureName) {
        Double feature = null;
        if (featureNames != null) {
            int position = Arrays.asList(featureNames).indexOf(featureName);
            if (position != -1) {
                feature = features[position];
            }
        }
        return feature;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder sBuilder = new StringBuilder();
        final Double[] features = getFeatures();
        for (int i = 0; i < features.length; i++) {
            // sb.append(features[i]).append(" ; ");
            sBuilder.append(featureNames[i] + ":" + features[i]).append("\n");
        }
        return sBuilder.toString();
    }

}