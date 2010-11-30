package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.Stopwords;
import tud.iir.classification.WordCorrelation;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetFilter;
import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.LineAction;

/**
 * 
 * optimum vlaues:
 * 
 * avgPr: 0.4863928631767631
 * avgRc: 0.23635926371440288
 * avgF1: 0.3181269338103561
 * 
 * @author Philipp Katz
 * 
 */
public class CandidateExtractor {

    private SnowballStemmer stemmer = new englishStemmer();
    private Stopwords stopwords = new Stopwords(Stopwords.Predefined.EN);
    private TokenizerPlus tokenizer = new TokenizerPlus();

    private Corpus corpus = new Corpus();
    private CandidateClassifier classifier = new CandidateClassifier();

    private boolean controlledMode = false; // XXX testing

    public CandidateExtractor() {
        tokenizer.setUsePosTagging(false);
        //tokenizer.setUsePosTagging(true);
    }

    public void addToCorpus(String text) {

        List<Token> tokens = tokenize(text);
        corpus.addTokens(tokens);

    }

    public void addToCorpus(String text, Set<String> tags) {

        List<Token> tokens = tokenize(text);
        corpus.addTokens(tokens);

        // corpus contains the stemmed representations!
        tags = stem(tags);
        corpus.addTags(tags);

    }

    public void saveCorpus() {
        corpus.calcRelCorrelations();
        FileHelper.serialize(corpus, "corpus.ser");
    }

    public void loadCorpus() {
        corpus = FileHelper.deserialize("corpus.ser");
        classifier.useTrainedClassifier();
    }

    public float[] evaluate(String text, Set<String> tags) {

        Set<String> stemmedTags = stem(tags);

        DocumentModel candidates = makeCandidates(text);
        List<Candidate> candidatesList = new ArrayList<Candidate>(candidates.getCandidates());

        // experimental ----- eliminate stopwords
        ListIterator<Candidate> li = candidatesList.listIterator();
        while (li.hasNext()) {
            Candidate current = li.next();
            if (stopwords.contains(current.getValue())) {
                li.remove();
            } else if (!current.getValue().matches("[a-zA-Z\\s]{3,}")) {
                li.remove();
            } else if (controlledMode && current.getPrior() == 0) {
                li.remove();
            }
        }

        for (Candidate candidate : candidatesList) {
            classifier.classify(candidate);
            // System.out.println(candidate.getValue() + " " + candidate.getRegressionValue());
        }

        Collections.sort(candidatesList, new CandidateComparator());
        // System.out.println("beforeReRanking: " + candidatesList);

        /*
         * for (Candidate c : candidatesList) {
         * System.out.println(c.getValue() + " " + c.getRegressionValue());
         * }
         */

        // / XXX experimental --- do re-raking

        Candidate[] candidateArray = candidatesList.toArray(new Candidate[0]);
        int numReRanking = candidateArray.length * (candidateArray.length - 1) / 2;

        final float correlationWeight = 90000;

        for (int i = 0; i < candidateArray.length; i++) {
            Candidate outerCand = candidateArray[i];
            for (int j = i; j < candidateArray.length; j++) {
                Candidate innerCand = candidateArray[j];

                // 2010-11-24
                String innerValue = innerCand.getStemmedValue();
                if (innerValue.contains(" ")) {
                    innerValue = innerCand.getValue().replaceAll(" ", "").toLowerCase();
                }
                String outerValue = outerCand.getStemmedValue();
                if (outerValue.contains(" ")) {
                    outerValue = outerCand.getValue().replaceAll(" ", "").toLowerCase();
                }
                // //

                WordCorrelation correlation = corpus.getCorrelation(outerValue, innerValue);
                if (correlation != null) {
                    float reRanking = (float) ((correlationWeight / numReRanking) * correlation
                            .getRelativeCorrelation());
                    innerCand.setRegressionValue(innerCand.getRegressionValue() + reRanking);
                    outerCand.setRegressionValue(outerCand.getRegressionValue() + reRanking);

                }

            }
        }

        Collections.sort(candidatesList, new CandidateComparator());

        // System.out.println("afterReRanking: " + candidatesList);

        // / end experimental

        if (candidatesList.size() > 10) {
            candidatesList.subList(10, candidatesList.size()).clear();
        }

        // for (Candidate c : candidatesList) {
        // System.out.println(c.getValue() + " " + c.getRegressionValue());
        // }
        int realCount = stemmedTags.size();

        stemmedTags.addAll(tags); // XXX

        int correctlyAssigned = 0;
        for (Candidate candidate : candidatesList) {
            for (String realTag : stemmedTags) {

                boolean isCorrectlyAssigned = false;
                isCorrectlyAssigned = isCorrectlyAssigned || realTag.equalsIgnoreCase(candidate.getStemmedValue());
                isCorrectlyAssigned = isCorrectlyAssigned || realTag.equalsIgnoreCase(candidate.getValue());
                isCorrectlyAssigned = isCorrectlyAssigned
                        || realTag.equalsIgnoreCase(candidate.getValue().replace(" ", ""));

                if (isCorrectlyAssigned) {
                    correctlyAssigned++;
                    break; // XXX
                }

            }
            System.out.println(" " + candidate.getValue());
        }

        int totalAssigned = candidatesList.size();

        float precision = (float) correctlyAssigned / totalAssigned;
        if (Float.isNaN(precision)) {
            precision = 0;
        }
        float recall = (float) correctlyAssigned / realCount;

        // System.out.println("real: " + stemmedTags);
        // System.out.println("assigned: " + candidatesList);
        System.out.println("correctlyAssigned:" + correctlyAssigned);
        System.out.println("totalAssigned:" + totalAssigned);
        System.out.println("realCount: " + realCount);
        System.out.println("pr: " + precision);
        System.out.println("rc: " + recall);
        System.out.println("------------------");

        float[] result = new float[2];
        result[0] = precision;
        result[1] = recall;
        return result;

    }

    public DocumentModel makeCandidates(String text) {

        DocumentModel model = new DocumentModel(corpus);
        List<Token> tokens = tokenize(text);

        for (Token token : tokens) {
            model.addToken(token);
        }

        model.createCandidates();

        return model;

    }

    public List<Token> tokenize(String text) {

        List<Token> tokens = new ArrayList<Token>();
        List<Token> uniGrams = tokenizer.tokenize(text);
        List<Token> collocations = tokenizer.makeCollocations(uniGrams, 5);

        tokens.addAll(uniGrams);
        tokens.addAll(collocations);

        return tokens;

    }

    public String stem(String unstemmed) {
        stemmer.setCurrent(unstemmed.toLowerCase());
        stemmer.stem();
        return stemmer.getCurrent();
    }

    public Set<String> stem(Set<String> unstemmed) {
        Set<String> result = new HashSet<String>();
        for (String unstemmedTag : unstemmed) {
            String stem = stem(unstemmedTag);
            result.add(stem);
        }
        return result;
    }

    public static void main(String[] args) {

        final CandidateExtractor extractor = new CandidateExtractor();

        // String text3 =
        // "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin. Beijing Duck is delicious. Beijing Duck is expensive.";
        //
        // DocumentModel cnd2 = extractor.makeCandidates(text3);
        // System.out.println(cnd2);
        //
        // System.exit(0);

        // Crawler crawler = new Crawler();
        //
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/The_Garden_of_Earthly_Delights");
        // String text = HTMLHelper.htmlToString(doc);
        //
        // DocumentModel cnd = extractor.makeCandidates(text);
        // cnd.cleanCandidates(5); // remove candidates which occur less than 5
        // System.out.println(cnd);
        //
        // System.exit(0);

        // //////////////////////////////////////////////
        // CORPUS CREATION
        // //////////////////////////////////////////////
        // createCorpus(extractor);

        // //////////////////////////////////////////////
        // FEATURE SET FOR TRAINING CREATION
        // //////////////////////////////////////////////
        // createTrainData(extractor);

        // //////////////////////////////////////////////
        // EVALUATION
        // //////////////////////////////////////////////
        evaluate(extractor);

        System.exit(0);

        String d1 = "If it walks like a duck and quacks like a duck, it must be a duck.";
        String d2 = "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.";
        String d3 = "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.";
        String d4 = "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.";
        // String d5 =
        // "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.";
        String d5 = "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipe for Jiaozi.";

        extractor.addToCorpus(d1);
        extractor.addToCorpus(d2);
        extractor.addToCorpus(d3);
        extractor.addToCorpus(d4);
        extractor.addToCorpus(d5);

        // System.out.println(". -> " + extractor.corpus.getInverseDocumentFrequency("."));

        DocumentModel candidates = extractor.makeCandidates(d2); // (, 1);
        System.out.println(candidates);
        System.exit(0);

        String text2 = "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // String text = "apple apple apples apples";
        // String text = "Apple sells phones called iPhones. The iPhone is a smart phone. Smart phones are great!";
        // String text = "iPhones iPhone iPhones";

        DocumentModel makeCandidates = extractor.makeCandidates(text2); // , 1);
        // System.out.println(makeCandidates);
        System.out.println(makeCandidates.toCSV());
        System.exit(0);

        // List<Token> tokens = extractor.tokenize(text, -1);
        // System.out.println(tokens);
        // DocumentModel model = extractor.tokenize(text, 2);
        // System.out.println(model);
        // List<Token> tokenize2 = extractor.tokenize2(text, 3);
        // CollectionHelper.print(tokenize2);
        // DocumentModel c = extractor.makeCandidates(text, 3);
        // System.out.println(c);
        //
        // System.exit(0);

        // String x = FileHelper.readFileToString("tokenizerProblem.txt");
        // List<String> t = Tokenizer.tokenize(x);
        // System.out.println(t.size());
        //
        // DocumentModel tokenize = extractor.tokenize(x, 3);
        // Collection<Candidate> candidates = tokenize.getCandidates(2);
        // for (Candidate candidate : candidates) {
        // System.out.println(candidate);
        // }
        // //System.out.println(tokenize);
        // System.exit(0);

        // StopWatch sw = new StopWatch();
        // extractor.extractFromFile("dataset_10000.txt");
        // System.out.println(sw.getElapsedTimeString());

        System.exit(1);
        //
        // String text =
        // "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // // List<Token> tokens = extractor.tokenize(text, -1);
        // // System.out.println(tokens);
        // DocumentModel model = extractor.extract(text, 2);
        // System.out.println(model);

    }

    @SuppressWarnings("unused")
    private static void evaluate(final CandidateExtractor extractor) {
        extractor.loadCorpus();
        final DescriptiveStatistics prStats = new DescriptiveStatistics();
        final DescriptiveStatistics rcStats = new DescriptiveStatistics();
        final Counter counter = new Counter();

        DeliciousDatasetReader reader = new DeliciousDatasetReader();

        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        filter.setMaxFileSize(600000);
        reader.setFilter(filter);

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());
                content = HTMLHelper.htmlToString(content, true);

                float[] prRc = extractor.evaluate(content, entry.getTags().uniqueSet());
                System.out.println("pr:" + prRc[0] + " rc:" + prRc[1]);
                counter.increment();

                prStats.addValue(prRc[0]);
                rcStats.addValue(prRc[1]);

            }
        };
        reader.read(callback, 1000);

        double meanPr = prStats.getMean();
        double meanRc = rcStats.getMean();
        double meanF1 = 2 * meanPr * meanRc / (meanPr + meanRc);

        System.out.println("avgPr: " + meanPr);
        System.out.println("avgRc: " + meanRc);
        System.out.println("avgF1: " + meanF1);

    }

    @SuppressWarnings("unused")
    private static void createCorpus(final CandidateExtractor extractor) {
        final Counter counter = new Counter();

        FileHelper.performActionOnEveryLine("data/tag_dataset_10000.txt", new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("#");

                Set<String> tags = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    tags.add(split[i]);
                }

                counter.increment();

                if (split.length > 2) {
                    extractor.addToCorpus(split[0], tags);
                }

                if (counter.getCount() % 10 == 0) {
                    System.out.println(counter);
                }

            }
        });

        extractor.saveCorpus();
    }

    @SuppressWarnings("unused")
    private static void createTrainData(final CandidateExtractor extractor) {
        extractor.loadCorpus();

        final Counter counter = new Counter();
        final StringBuilder data = new StringBuilder();
        counter.reset();

        FileHelper.performActionOnEveryLine("data/tag_dataset_10000.txt", new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("#");

                if (split.length > 2) {
                    DocumentModel candidates = extractor.makeCandidates(split[0]);

                    Set<String> tags = new HashSet<String>();
                    for (int i = 1; i < split.length; i++) {
                        tags.add(split[i].toLowerCase());
                    }

                    // ?
                    Set<String> stemmedTags = extractor.stem(tags);
                    tags.addAll(stemmedTags);

                    for (Candidate candidate : candidates.getCandidates()) {

                        boolean isCandidate = false;
                        isCandidate = isCandidate || tags.contains(candidate.getStemmedValue());
                        isCandidate = isCandidate || tags.contains(candidate.getValue());
                        isCandidate = isCandidate || tags.contains(candidate.getValue().replace(" ", ""));

                        candidate.setPositive(isCandidate);

                    }

                    if (counter.getCount() == 0) {
                        data.append("#")
                                .append(StringUtils.join(candidates.getCandidates().iterator().next().getFeatures()
                                        .keySet(), ";")).append("\n");
                    }

                    data.append(candidates.toCSV());

                }

                counter.increment();
                if (counter.getCount() % 10 == 0) {
                    System.out.println(counter);
                }
                if (counter.getCount() == 1000) {
                    breakLineLoop();
                }

            }
        });

        FileHelper.writeToFile("train_1000_new.csv", data);

    }

}
