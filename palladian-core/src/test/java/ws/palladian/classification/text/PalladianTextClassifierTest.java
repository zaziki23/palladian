package ws.palladian.classification.text;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.FeatureSetting;

public class PalladianTextClassifierTest {
    
    private static final String JRC_TRAIN_FILE = "/Users/pk/Desktop/data/Wikipedia76Languages/languageDocumentIndex_random1000_train.txt";
    private static final String JRC_TEST_FILE = "/Users/pk/Desktop/data/Wikipedia76Languages/languageDocumentIndex_random1000_test.txt";
    private static final String WIKIPEDIA_TRAIN_FILE = "/Users/pk/Desktop/data/Wikipedia76Languages/languageDocumentIndex_random1000_train.txt";
    private static final String WIKIPEDIA_TEST_FILE = "/Users/pk/Desktop/data/Wikipedia76Languages/languageDocumentIndex_random1000_test.txt";
    private static final String TWENTY_NEWSGROUPS_1 = "/Users/pk/Desktop/data/20newsgroups-18828/index_split1.txt";
    private static final String TWENTY_NEWSGROUPS_2 = "/Users/pk/Desktop/data/20newsgroups-18828/index_split2.txt";
//    private static final String JRC_TRAIN_FILE = "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt";
//    private static final String JRC_TEST_FILE = "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt";
//    private static final String WIKIPEDIA_TRAIN_FILE = "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt";
//    private static final String WIKIPEDIA_TEST_FILE = "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt";
//    private static final String TWENTY_NEWSGROUPS_1 = "C:\\Workspace\\data\\20newsgroups-18828\\index_split1.txt";
//    private static final String TWENTY_NEWSGROUPS_2 = "C:\\Workspace\\data\\20newsgroups-18828\\index_split2.txt";

    @Test
    public void testDictionaryClassifierCharJrc() throws IOException {

        PalladianTextClassifier dictionaryClassifier1 = new PalladianTextClassifier();

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        featureSetting.setMaxTerms(1000);
        featureSetting.setMinNGramLength(3);
        featureSetting.setMaxNGramLength(6);

        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath(JRC_TRAIN_FILE);

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath(JRC_TEST_FILE);

        DictionaryModel model = dictionaryClassifier1.train(dataset, classificationTypeSetting, featureSetting);

        // model.toDictionaryCsv(new PrintStream("dictCharJrc_ref.csv"));

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy char jrc: " + accuracy);

        assertTrue(accuracy >= 0.983);
    }

    @Test
    public void testDictionaryClassifierWordJrc() throws IOException {

        PalladianTextClassifier dictionaryClassifier1 = new PalladianTextClassifier();
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        featureSetting.setMaxTerms(10);
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(3);

        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath(WIKIPEDIA_TRAIN_FILE);

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath(WIKIPEDIA_TEST_FILE);

        DictionaryModel model = dictionaryClassifier1.train(dataset, classificationTypeSetting, featureSetting);

        // model.toDictionaryCsv(new PrintStream("dictWordJrc_ref.csv"));

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy word jrc: " + accuracy);
        assertTrue(accuracy >= 0.725);
    }

    @Test
    public void testDictionaryClassifierCharNg() throws IOException {

        PalladianTextClassifier dictionaryClassifier1 = new PalladianTextClassifier();
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        featureSetting.setMaxTerms(1000);
        featureSetting.setMinNGramLength(3);
        featureSetting.setMaxNGramLength(6);

        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath(TWENTY_NEWSGROUPS_1);

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        // dataset2.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt");
        dataset2.setPath(TWENTY_NEWSGROUPS_2);

        DictionaryModel model = dictionaryClassifier1.train(dataset, classificationTypeSetting, featureSetting);

        // model.toDictionaryCsv(new PrintStream("dictCharNg_ref.csv"));

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy char ng: " + accuracy);
        assertTrue(accuracy >= 0.8894952251023193); // 0.8882825526754585
    }

    @Test
    public void testDictionaryClassifierWordNg() throws IOException {

        PalladianTextClassifier dictionaryClassifier1 = new PalladianTextClassifier();
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        featureSetting.setMaxTerms(10);
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(3);

        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath(TWENTY_NEWSGROUPS_1);

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath(TWENTY_NEWSGROUPS_2);

        DictionaryModel model = dictionaryClassifier1.train(dataset, classificationTypeSetting, featureSetting);

        // model.toDictionaryCsv(new PrintStream("dictWordNg_ref.csv"));

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy word ng: " + accuracy);
        assertTrue(accuracy >= 0.6030013642564802); // 0.17735334242837653
    }

    private double evaluate(PalladianTextClassifier dc, DictionaryModel model, Dataset dataset) {
        int correct = 0;
        List<Instance> testInstances = dc.createInstances(dataset, model.getFeatureSetting());
        for (Instance nominalInstance : testInstances) {
            CategoryEntries categoryEntries = dc.classify(nominalInstance.featureVector, model);

            if (categoryEntries.getMostLikelyCategoryEntry().getCategory().equalsIgnoreCase(nominalInstance.targetClass)) {
                correct++;
            }

        }

        return correct / (double)testInstances.size();
    }
}
