package tud.iir.classification.language.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.classification.language.AlchemyLangDetect;
import tud.iir.classification.language.GoogleLangDetect;
import tud.iir.classification.language.JLangDetect;
import tud.iir.classification.language.LanguageClassifier;
import tud.iir.classification.language.PalladianLangDetect;
import tud.iir.classification.page.evaluation.Dataset;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;

public class LanguageDetectionEvaluation {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(LanguageDetectionEvaluation.class);

    /**
     * Evaluate two language detectors on a set of strings.
     * The evaluation file must have the following structure for each line:<br>
     * string to classify ### language
     * 
     * @param evaluationFilePath The file with the evaluation data.
     */
    public void evaluate(Dataset dataset, Set<String> possibleClasses, Integer documentLength) {

        LOGGER.info("evaluate JLangDetect vs. Google vs. Alchemy vs. Palladian");
        StopWatch sw = new StopWatch();

        LanguageClassifier jLangDetectClassifier = new JLangDetect();
        LanguageClassifier googleLanguageClassifier = new GoogleLangDetect();
        LanguageClassifier alchemyLanguageClassifier = new AlchemyLangDetect();
        LanguageClassifier palladianClassifier = new PalladianLangDetect();

        // we tell Palladian that only a subset of the learned languages is allowed for this evaluation, otherwise
        // jLangDetect has an advantage
        // ((PalladianLangDetect) palladianClassifier).setPossibleClasses(possibleClasses);

        List<String> lines = FileHelper.readFileToArray(dataset.getPath());

        int totalDocuments = lines.size();
        int jLangCorrect = 0;
        int jLangClassified = 0;
        int googleCorrect = 0;
        int googleClassified = 0;
        int alchemyCorrect = 0;
        int alchemyClassified = 0;
        int palladianCorrect = 0;
        int palladianClassified = 0;

        int lineCount = 1;
        int totalLines = lines.size();
        for (String line : lines) {
            String[] parts = line.split(dataset.getSeparationString());
            String document = parts[0];
            String correctLanguage = parts[1];

            if (dataset.isFirstFieldLink()) {
                document = FileHelper.readFileToString(dataset.getRootPath() + document);
            }

            if (documentLength != null) {
                document = document.substring(0, Math.min(documentLength, document.length()));
            }

            boolean jlang = false;
            boolean google = false;
            boolean alchemy = false;
            boolean palladian = false;

            // jlang
            String jLangClass = jLangDetectClassifier.classify(document);
            if (correctLanguage.equals(jLangClass)) {
                jLangCorrect++;
                jlang = true;
            }
            if (jLangClass.length() > 0) {
                jLangClassified++;
            }

            // google
            String googleClass = googleLanguageClassifier.classify(document.substring(0,
                    Math.min(100, document.length())));
            if (correctLanguage.equals(googleClass)) {
                googleCorrect++;
                google = true;
            }
            if (googleClass.length() > 0) {
                googleClassified++;
            }

            // alchemy
            String alchemyClass = alchemyLanguageClassifier.classify(document.substring(0,
                    Math.min(100, document.length())));
            if (correctLanguage.equals(alchemyClass)) {
                alchemyCorrect++;
                alchemy = true;
            }
            if (alchemyClass.length() > 0) {
                alchemyClassified++;
            }

            // palladian
            String palladianClass = palladianClassifier.classify(document);
            if (correctLanguage.equals(palladianClass)) {
                palladianCorrect++;
                palladian = true;
            }
            if (palladianClass.length() > 0) {
                palladianClassified++;
            }

            double percent = 100.0 * MathHelper.round(lineCount / (double) totalLines, 2);
            LOGGER.info("line " + lineCount + ", " + percent + "% ("
                    + jLangDetectClassifier.mapLanguageCode(correctLanguage)
                    + ") -> jlang: " + jlang + " | google: " + google + " | alchemy: " + alchemy + " | palladian: "
                    + palladian);

            lineCount++;
        }

        LOGGER.info("evaluated over " + totalDocuments + " strings in " + sw.getElapsedTimeString());
        LOGGER.info("Accuracy JLangDetect: " + MathHelper.round(100 * jLangCorrect / (double) jLangClassified, 2)
                + "% (" + jLangClassified + " classified)");
        LOGGER.info("Accuracy Google     : " + MathHelper.round(100 * googleCorrect / (double) googleClassified, 2)
                + "% (" + googleClassified + " classified)");
        LOGGER.info("Accuracy Alchemy    : " + MathHelper.round(100 * alchemyCorrect / (double) alchemyClassified, 2)
                + "% (" + alchemyClassified + " classified)");
        LOGGER.info("Accuracy Palladian  : "
                + MathHelper.round(100 * palladianCorrect / (double) palladianClassified, 2) + "% ("
                + palladianClassified + " classified)");
    }


    /**
     * @param args
     */
    public static void main(String[] args) {

        // LanguageClassifier glangd = new AlchemyLangDetect();
        // System.out.println(glangd.classify("hello world, how are you today?"));
        // System.exit(1);

        LanguageDetectionEvaluation evaluator = new LanguageDetectionEvaluation();
        Set<String> possibleClasses = new HashSet<String>();
        possibleClasses.add("da");
        possibleClasses.add("de");
        possibleClasses.add("el");
        possibleClasses.add("en");
        possibleClasses.add("es");
        possibleClasses.add("fi");
        possibleClasses.add("fr");
        possibleClasses.add("it");
        possibleClasses.add("nl");
        possibleClasses.add("pt");
        possibleClasses.add("sv");
        // evaluator.evaluate("data/evaluation/LanguageDetection11Languages_small.txt", possibleClasses);

        // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // set the path to the dataset, the first field is a link, and columns are separated with a space
        dataset.setPath("C:\\Safe\\Datasets\\jrc language data converted\\indexAll22Languages_ipc100_split2.txt");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");

        evaluator.evaluate(dataset, possibleClasses, null);
    }

}
