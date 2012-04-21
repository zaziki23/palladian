package ws.palladian.classification.language;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.DatasetManager;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.ClassifierManager;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextClassifier;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.ClassifierPerformance;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.EvaluationSetting;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.Cache;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;

/**
 * The best setting for medium to long texts is to use word n-grams with 1<=n<=3.
 * Evaluation results can be found in the Palladian book.
 * 
 * @author David Urbansky
 * 
 */
public class PalladianLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PalladianLangDetect.class);

    private final TextClassifier palladianClassifier;

    /** We can specify which classes are possible and discard all others for the classification task. */
    private Set<String> possibleClasses = null;

    public PalladianLangDetect(String modelPath) {
        palladianClassifier = (TextClassifier)Cache.getInstance().getDataObject(modelPath, new File(modelPath));
    }

    public PalladianLangDetect() {
        palladianClassifier = DictionaryClassifier.load("data/models/palladianLanguageJRC/palladianLanguageJRC.ser");
    }

    public Set<String> getPossibleClasses() {
        return possibleClasses;
    }

    public void setPossibleClasses(Set<String> possibleClasses) {
        this.possibleClasses = possibleClasses;
    }

    public ClassifierPerformance test(Dataset dataset) {
        ClassifierManager cm = new ClassifierManager();
        ClassifierPerformance cp = cm.testClassifier(dataset, palladianClassifier);
        LOGGER.info("Average Accuracy: " + cp.getAverageAccuracy(false));
        return cp;
    }

    public void evaluateBestSetting() {
        ClassifierManager classifierManager = new ClassifierManager();

        // build a set of classification type settings to evaluate
        List<ClassificationTypeSetting> classificationTypeSettings = new ArrayList<ClassificationTypeSetting>();
        ClassificationTypeSetting cts = new ClassificationTypeSetting();
        cts.setClassificationType(ClassificationTypeSetting.SINGLE);
        cts.setSerializeClassifier(false);
        classificationTypeSettings.add(cts);

        // build a set of classifiers to evaluate
        List<TextClassifier> classifiers = new ArrayList<TextClassifier>();
        TextClassifier classifier = null;
        classifier = new DictionaryClassifier();
        classifiers.add(classifier);

        // build a set of feature settings for evaluation
        List<FeatureSetting> featureSettings = new ArrayList<FeatureSetting>();
        FeatureSetting fs = null;
        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(1);
        fs.setMaxNGramLength(3);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(1);
        fs.setMaxNGramLength(7);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(4);
        fs.setMaxNGramLength(7);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(3);
        fs.setMaxNGramLength(8);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        fs.setMinNGramLength(1);
        fs.setMaxNGramLength(3);
        featureSettings.add(fs);

        // build a set of datasets that should be used for evaluation
        Set<Dataset> datasets = new HashSet<Dataset>();
        Dataset dataset = new Dataset();
        dataset.setPath("C:\\Safe\\Datasets\\jrc language data converted\\indexAll22Languages_ipc1000.txt");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        datasets.add(dataset);

        // set evaluation settings
        EvaluationSetting evaluationSetting = new EvaluationSetting();
        evaluationSetting.setTrainingPercentageMin(20);
        evaluationSetting.setTrainingPercentageMax(50);
        evaluationSetting.setTrainingPercentageStep(10);
        evaluationSetting.setkFolds(3);
        evaluationSetting.addDataset(dataset);

        // train and test all classifiers in all combinations
        StopWatch stopWatch = new StopWatch();

        // train + test
        classifierManager.learnBestClassifier(classificationTypeSettings, classifiers, featureSettings,
                evaluationSetting);

        LOGGER.info("finished training and testing classifier in " + stopWatch.getElapsedTimeString());
    }

    public void train(UniversalInstance instance) {
        palladianClassifier.train(instance);
    }

    public ClassifierPerformance evaluate(Dataset dataset) {
        return palladianClassifier.evaluate(dataset);
    }

    /**
     * Train the language detector on a dataset.
     * 
     * @param dataset The dataset to train on.
     * @param classifierName The name of the classifier. The name is added to the classifierPath.
     * @param classifierPath The path where the classifier should be saved to. For example, <tt>data/models/</tt>
     */
    public static void train(Dataset dataset, String classifierName, String classifierPath) {
        train(dataset, classifierName, classifierPath, null, null);
    }

    public static void train(Dataset dataset, String classifierName, String classifierPath,
            ClassificationTypeSetting cts, FeatureSetting fs) {

        // take the time for the learning
        StopWatch stopWatch = new StopWatch();

        // create a classifier mananger object
        ClassifierManager classifierManager = new ClassifierManager();

        // create a text classifier by giving a name and a path where it should be saved to
        TextClassifier classifier = new DictionaryClassifier(classifierName, classifierPath + classifierName + "/");
        // TextClassifier classifier = new DictionaryClassifier(classifierName,classifierPath);

        // specify the settings for the classification
        ClassificationTypeSetting classificationTypeSetting = cts;
        if (classificationTypeSetting == null) {
            classificationTypeSetting = new ClassificationTypeSetting();

            // we use only a single category per document
            classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

            // we want the classifier to be serialized in the end
            classificationTypeSetting.setSerializeClassifier(true);
        }

        // specify feature settings that should be used by the classifier
        FeatureSetting featureSetting = fs;

        if (featureSetting == null) {
            featureSetting = new FeatureSetting();

            // we want to create character-level n-grams
            featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);

            // the minimum length of our n-grams should be 4
            featureSetting.setMinNGramLength(4);

            // the maximum length of our n-grams should be 7
            featureSetting.setMaxNGramLength(7);
        }

        // we assign the settings to our classifier
        classifier.setClassificationTypeSetting(classificationTypeSetting);
        classifier.setFeatureSetting(featureSetting);

        // now we can train the classifier using the given dataset
        // classifier.train(dataset);
        // classifier.save(classifierPath);
        // classifierManager.trainClassifier(dataset, classifier);

        classifier.train(dataset);

        // test the classifier
        // Dataset testDataset = new Dataset();
        //
        // // set the path to the dataset, the first field is a link, and columns are separated with a space
        // testDataset.setPath("C:\\Data\\datasets\\JRCLanguageCorpus\\indexAll22Languages_ipc20_split2.txt");
        //
        // testDataset.setFirstFieldLink(true);
        // testDataset.setSeparationString(" ");
        //
        // System.out.println(classifier.evaluate(testDataset));

        FileHelper.serialize(classifier, classifierPath + classifierName + ".gz");

        LOGGER.info("finished training classifier in " + stopWatch.getElapsedTimeString());
    }

    @Override
    public String classify(String text) {
        return palladianClassifier.classify(text, getPossibleClasses()).getAssignedCategoryEntryNames();
    }

    public CategoryEntries classifyAsCategoryEntry(String text) {
        return palladianClassifier.classify(text, getPossibleClasses()).getAssignedCategoryEntries();
    }

    public static void main(String[] args) throws IOException {

        // ///////////////// use the language classifier ///////////////////
        // String languageModelPath = "data/models/palladianLanguageClassifier/LanguageClassifier.gz";
        // String languageModelPath = "data/models/palladianLanguageJRC/palladianLanguageJRC.gz";
        // String languageModelPath =
        // "C:\\My Dropbox\\KeywordExtraction\\palladianLanguageJRC_o\\palladianLanguageJRC.gz";
        //
        PalladianLangDetect pld0 = new PalladianLangDetect("data/models/language/wikipedia76Languages20ipc.gz");
        // PalladianLangDetect pld0 = new PalladianLangDetect("data/models/language/languageMicroblogging.gz");
        String language = pld0.classify("This is a sample text in English");
        System.out.println("The text was classified as: " + language);
        language = pld0.classify("Das ist ein Beispieltext auf Deutsch");
        System.out.println("The text was classified as: " + language);
        language = pld0.classify("Se trata de un texto de muestra en español");
        System.out.println("The text was classified as: " + language);
        System.exit(0);
        // ////////////////////////////////////////////////////////////////

        // ///////////////// find the best performing settings ///////////////////
        // specify the dataset that should be used as training data
        // PalladianLangDetect pld0 = new PalladianLangDetect();
        // pld0.evaluateBestSetting();
        // System.exit(0);
        // ////////////////////////////////////////////////////////////////

        // ///////////////// learn from a given dataset ///////////////////
        String datasetRootFolder = "H:\\PalladianData\\Datasets\\JRCLanguageCorpus";

        // // create an index over the dataset
        DatasetManager dsManager = new DatasetManager();
        // String path = dsManager.createIndex(datasetRootFolder, new String[] { "en", "es", "de" });
        // String path = dsManager.createIndex(datasetRootFolder);
        //
        // // create an excerpt with 1000 instances per class
        String indexExcerpt = dsManager.createIndexExcerpt(
                "H:\\PalladianData\\Datasets\\Wikipedia76Languages\\languageDocumentIndex.txt", " ", 20);
        //
        // // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(true);

        // set the path to the dataset, the first field is a link, and columns are separated with a space
        // dataset.setPath("H:\\PalladianData\\Datasets\\JRCLanguageCorpus\\indexAll22Languages_ipc20.txt");
        // dataset.setPath("H:\\PalladianData\\Datasets\\Microblogging35Languages\\languageDocumentIndex.txt");
        // dataset.setPath("H:\\PalladianData\\Datasets\\Wikipedia76Languages\\languageDocumentIndex.txt");
        dataset.setPath(indexExcerpt);

        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");

        PalladianLangDetect.train(dataset, "wikipedia76Languages20ipc", "data/models/palladian/language/");
        // ////////////////////////////////////////////////////////////////

    }

}