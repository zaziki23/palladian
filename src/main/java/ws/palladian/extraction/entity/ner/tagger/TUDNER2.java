package ws.palladian.extraction.entity.ner.tagger;

import java.io.Serializable;

import org.apache.log4j.Logger;

import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalClassifier;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.numeric.NumericInstance;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.extraction.entity.ner.Annotation;
import ws.palladian.extraction.entity.ner.Annotations;
import ws.palladian.extraction.entity.ner.FileFormatParser;
import ws.palladian.extraction.entity.ner.TaggingFormat;
import ws.palladian.extraction.entity.ner.evaluation.EvaluationResult;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.math.MathHelper;

public class TUDNER2 extends TUDNER implements Serializable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TUDNER2.class);

    private static final long serialVersionUID = 4630752782811857313L;

    /** The classifier to use for classifying the annotations. */
    private UniversalClassifier universalClassifier;


    public TUDNER2() {
        setName("TUD NER 2");
        universalClassifier = new UniversalClassifier();
        universalClassifier.getTextClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);
        universalClassifier.getNumericClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);
        universalClassifier.getNominalClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);

        universalClassifier.getTextClassifier().getDictionary().setName("dictionary");
        universalClassifier.getTextClassifier().getFeatureSetting().setMinNGramLength(2);
        universalClassifier.getTextClassifier().getFeatureSetting().setMaxNGramLength(8);

        universalClassifier.switchClassifiers(true, false, false);
    }

    @Override
    public String getModelFileEnding() {
        return "model";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    private Annotations getEntityCandidates(String inputText) {

        // // every token is an entity candidate
        // List<String> entityCandidateTokens = Tokenizer.tokenize(inputText);

        Annotations annotations = new Annotations();

        String entityTag = "<CANDIDATE>$0</CANDIDATE>";

        String regexp = "([A-Z]\\.)+|([\\p{L}\\w]+)([-\\.,]([\\p{L}\\w]+))*|\\.([\\p{L}\\w]+)|</?([\\p{L}\\w]+)>|(\\$\\d+\\.\\d+)|([^\\w\\s<]+)";

        inputText = inputText.replaceAll(regexp, entityTag);

        annotations = FileFormatParser.getAnnotationsFromXMLText(inputText);

        return annotations;
    }

    private Annotations verifyAnnotationsWithUniversalClassifier_(Annotations entityCandidates) {
        Annotations annotations = new Annotations();

        int i = 0;
        Annotation lastAnnotation = null;
        for (Annotation annotation : entityCandidates) {

            universalClassifier.classify(annotation);

            // try to classify with the last annotation
            if (lastAnnotation != null && lastAnnotation.getLength() > 1 && annotation.getLength() > 1
                    && annotation.leftWhitespace) {

                String combinedEntityName = lastAnnotation.getEntity() + " " + annotation.getEntity();
                TextInstance ti = universalClassifier.getTextClassifier().classify(combinedEntityName);
                if (!ti.getMainCategoryEntry().getCategory().getName().equalsIgnoreCase("O")) {

                    annotations.remove(lastAnnotation);
                    Annotation combinedAnnotation = new Annotation(lastAnnotation.getOffset(), combinedEntityName, ti
                            .getMainCategoryEntry().getCategory().getName());
                    annotations.add(combinedAnnotation);

                } else if (!annotation.getMostLikelyTagName().equalsIgnoreCase("O") && annotation.getLength() > 1) {
                    annotations.add(annotation);
                }

            } else if (!annotation.getMostLikelyTagName().equalsIgnoreCase("O") && annotation.getLength() > 1) {
                annotations.add(annotation);
            }

            if (i % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * i / entityCandidates.size(), 0) + "%");
            }
            i++;

            lastAnnotation = annotation;
        }

        return annotations;
    }

    private Annotations verifyAnnotationsWithUniversalClassifier(Annotations entityCandidates) {
        Annotations annotations = new Annotations();

        int i = 0;
        for (Annotation annotation : entityCandidates) {

            universalClassifier.classify(annotation);
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("###NO_ENTITY###")) {
                annotations.add(annotation);
            }

            if (i % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * i / entityCandidates.size(), 0) + "%");
            }
            i++;
        }

        return annotations;
    }

    @Override
    public Annotations getAnnotations(String inputText) {

        Annotations annotations = new Annotations();

        // get the candates, every token is potentially a (part of) an entity
        Annotations entityCandidates = getEntityCandidates(inputText);

        // classify annotations with the UniversalClassifier
        annotations.addAll(verifyAnnotationsWithUniversalClassifier(entityCandidates));
        // annotations.addAll(verifyAnnotationsNDict(entityCandidates));

        // combine annotations that are right next to each other having the same tag
        Annotations combinedAnnotations = new Annotations();
        annotations.sort();
        Annotation lastAnnotation = new Annotation(-2, "", "");
        // int lastIndex = -2;
        Annotation lastCombinedAnnotation = null;
        for (Annotation annotation : annotations) {
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("o")
                    && annotation.getMostLikelyTagName().equalsIgnoreCase(lastAnnotation.getMostLikelyTagName())
                    && annotation.getOffset() == lastAnnotation.getEndIndex() + 1) {

                if (lastCombinedAnnotation == null) {
                    lastCombinedAnnotation = lastAnnotation;
                }

                Annotation combinedAnnotation = new Annotation(lastCombinedAnnotation.getOffset(),
                        lastCombinedAnnotation.getEntity() + " " + annotation.getEntity(),
                        annotation.getMostLikelyTagName(), combinedAnnotations);
                combinedAnnotations.add(combinedAnnotation);
                lastCombinedAnnotation = combinedAnnotation;
                combinedAnnotations.remove(lastCombinedAnnotation);
            } else {
                combinedAnnotations.add(annotation);
                lastCombinedAnnotation = null;
            }

            lastAnnotation = annotation;
        }

        // remove all "O"
        Annotations cleanAnnotations = new Annotations();
        for (Annotation annotation : combinedAnnotations) {
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("o") && annotation.getLength() > 1) {
                cleanAnnotations.add(annotation);
            }
        }

        FileHelper.writeToFile("data/temp/tudNER2Output.txt", tagText(inputText, cleanAnnotations));

        // weight("data/datasets/ner/conll/training_verysmall.txt");

        return cleanAnnotations;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        // get all training annotations including their features
        Annotations annotations = FileFormatParser.getAnnotationsFromColumnTokenBased(trainingFilePath);

        // create instances with nominal and numeric features
        Instances<UniversalInstance> textInstances = new Instances<UniversalInstance>();
        Instances<UniversalInstance> nominalInstances = new Instances<UniversalInstance>();
        Instances<NumericInstance> numericInstances = new Instances<NumericInstance>();

        LOGGER.info("start creating " + annotations.size() + " annotations for training");
        for (Annotation annotation : annotations) {
            UniversalInstance textInstance = new UniversalInstance(textInstances);
            textInstance.setTextFeature(annotation.getEntity());
            textInstance.setInstanceCategory(annotation.getInstanceCategory());
            textInstances.add(textInstance);

            UniversalInstance nominalInstance = new UniversalInstance(nominalInstances);
            nominalInstance.setNominalFeatures(annotation.getNominalFeatures());
            nominalInstance.setInstanceCategory(annotation.getInstanceCategory());
            nominalInstances.add(nominalInstance);

            NumericInstance numericInstance = new NumericInstance(numericInstances);
            numericInstance.setFeatures(annotation.getNumericFeatures());
            numericInstance.setInstanceCategory(annotation.getInstanceCategory());
            numericInstances.add(numericInstance);
        }

        // train the text classifier
        universalClassifier.getTextClassifier().setTrainingInstances(textInstances);

        // train the numeric classifier with numeric features from the annotations
        universalClassifier.getNumericClassifier().setTrainingInstances(numericInstances);
        universalClassifier.getNumericClassifier().getTrainingInstances().normalize();

        // train the nominal classifier with nominal features from the annotations
        universalClassifier.getNominalClassifier().setTrainingInstances(nominalInstances);

        LOGGER.info("start training classifiers now...");
        universalClassifier.trainAll();
        // universalClassifier.learnClassifierWeights(annotations);

        saveModel(modelFilePath);

        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        TUDNER2 tagger = new TUDNER2();

        // using a column training and testing file
        StopWatch stopWatch = new StopWatch();
        tagger.train("data/datasets/ner/conll/training_small.txt", "data/temp/tudner2.model");
        // System.exit(0);
        tagger.loadModel("data/temp/tudner2.model");
        // tagger.calculateRemoveAnnotatations(FileFormatParser.getText("data/datasets/ner/conll/training.txt",TaggingFormat.COLUMN));
        EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_validation.txt",
                "data/temp/tudner2.model",
                TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        System.out.println(stopWatch.getElapsedTimeString());

    }

}