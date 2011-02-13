package tud.iir.classification;

import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.classification.numeric.KNNClassifier;
import tud.iir.classification.numeric.NumericClassifier;
import tud.iir.classification.numeric.NumericInstance;
import tud.iir.classification.page.ClassificationDocument;
import tud.iir.classification.page.DictionaryClassifier;
import tud.iir.classification.page.TextClassifier;
import tud.iir.helper.FileHelper;


public class UniversalClassifier extends Classifier<UniversalInstance> {
    
    /** The serialize version ID. */
    private static final long serialVersionUID = 6434885229397022001L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(UniversalClassifier.class);
    
    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private TextClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private NumericClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private BayesClassifier nominalClassifier;

    public UniversalClassifier() {

        textClassifier = DictionaryClassifier.load("data/temp/textClassifier.gz");
        numericClassifier = KNNClassifier.load("data/temp/numericClassifier.gz");
        nominalClassifier = BayesClassifier.load("data/temp/nominalClassifier.gz");

    }

    public void classify(UniversalInstance instance) {

        // separate instance in feature types
        String textFeature = instance.getTextFeature();
        List<Double> numericFeatures = instance.getNumericFeatures();
        List<String> nominalFeatures = instance.getNominalFeatures();

        // classify text using the dictionary classifier
        ClassificationDocument textResult = textClassifier.classify(textFeature);

        // classify numeric features with the KNN
        NumericInstance numericInstance = new NumericInstance(null);
        numericInstance.setFeatures(numericFeatures);
        numericClassifier.classify(numericInstance);

        // classify nominal features with the Bayes classifier
        UniversalInstance nominalInstance = new UniversalInstance(null);
        nominalInstance.setNominalFeatures(nominalFeatures);
        nominalClassifier.classify(nominalInstance);

        // merge classification results
        CategoryEntries mergedCategoryEntries = new CategoryEntries();
        mergedCategoryEntries.addAll(textResult.getAssignedCategoryEntries());
        mergedCategoryEntries.addAll(numericInstance.getAssignedCategoryEntries());
        mergedCategoryEntries.addAll(nominalInstance.getAssignedCategoryEntries());

        instance.assignCategoryEntries(mergedCategoryEntries);
    }

    public TextClassifier getTextClassifier() {
        return textClassifier;
    }

    public void setTextClassifier(TextClassifier textClassifier) {
        this.textClassifier = textClassifier;
    }

    public NumericClassifier getNumericClassifier() {
        return numericClassifier;
    }

    public void setNumericClassifier(NumericClassifier numericClassifier) {
        this.numericClassifier = numericClassifier;
    }

    public BayesClassifier getNominalClassifier() {
        return nominalClassifier;
    }

    public void setNominalClassifier(BayesClassifier nominalClassifier) {
        this.nominalClassifier = nominalClassifier;
    }

    @Override
    public void save(String classifierPath) {
        FileHelper.serialize(this, classifierPath + getName() + ".gz");
    }

    public static UniversalClassifier load(String classifierPath) {
        LOGGER.info("deserialzing classifier from " + classifierPath);
        return (UniversalClassifier) FileHelper.deserialize(classifierPath);
    }
    
}