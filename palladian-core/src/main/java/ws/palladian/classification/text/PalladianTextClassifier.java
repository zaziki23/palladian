package ws.palladian.classification.text;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * This text classifier builds a dictionary from a pre-categorized list of text documents which can then be used to
 * categorize new, uncategorized text documents. During learning, a weighted term look up table is created, to learn how
 * probable each n-gram is for a given category. This look up table is used by during classification.
 * 
 * <p>
 * This classifier won the first Research Garden competition where the goal was to classify product descriptions into
 * eight different categories. See <a href=
 * "https://web.archive.org/web/20120122045250/http://www.research-garden.de/c/document_library/get_file?uuid=e60fa8da-4f76-4e64-a692-f74d5ffcf475&amp;groupId=10137"
 * >press release</a> (via archive.org).
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianTextClassifier implements Learner<DictionaryModel>, Classifier<DictionaryModel> {

    /**
     * <p>
     * Implementations of this interface allow to influence the scoring during classification. Usually (i.e. in cases
     * where you cannot thoroughly verify, that a different scoring formula works better, using training and test data)
     * you should stick to the provided {@link DefaultScorer}, which has proven to perform well in general
     * classification cases through our extensive experiments.
     * 
     * @author pk
     */
    public static interface Scorer {
        /**
         * Score a term-category-pair in a document which has to be classified.
         * 
         * @param term The term (this value usually has no influence on the scoring, but is provided for debugging
         *            purposes).
         * @param category The category (for debugging purposes, see above).
         * @param termCategoryProbability The probability, that the current term occurs within a document in the current
         *            category. As extracted from the dictionary model.
         * @param dictCount The absolute count of documents in the dictionary which contain the term.
         * @param docCount The absolute count of the term in the current document.
         * @param categoryProbability The probability in the dictionary for the current category.
         * @return A score for the term-category pair, greater/equal zero.
         */
        double score(String term, String category, double termCategoryProbability, int dictCount, int docCount,
                double categoryProbability);
    }

    /**
     * Default scorer implementation which scores a term-category-pair using the squared term-category probability.
     * 
     * @author pk
     */
    public static final class DefaultScorer implements Scorer {
        @Override
        public double score(String term, String category, double termCategoryProbability, int dictCount, int docCount,
                double categoryProbability) {
            return termCategoryProbability * termCategoryProbability;
        }
    }

    private final ProcessingPipeline pipeline;

    private final FeatureSetting featureSetting;

    private final Scorer scorer;

    public PalladianTextClassifier(FeatureSetting featureSetting) {
        this(featureSetting, new DefaultScorer());
    }

    public PalladianTextClassifier(FeatureSetting featureSetting, Scorer scorer) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        this.featureSetting = featureSetting;
        this.pipeline = new PreprocessingPipeline(featureSetting);
        this.scorer = scorer;
    }

    @Override
    public DictionaryModel train(Iterable<? extends Trainable> trainables) {
        DictionaryModel model = new DictionaryModel(featureSetting);
        for (Trainable trainable : trainables) {
            String targetClass = trainable.getTargetClass();
            String content = ((TextDocument)trainable).getContent();
            Iterator<String> iterator = new NGramIterator(content, featureSetting.getMinNGramLength(),
                    featureSetting.getMaxNGramLength());
            Set<String> terms = CollectionHelper.newHashSet();
            while (iterator.hasNext() && terms.size() < featureSetting.getMaxTerms()) {
                terms.add(iterator.next());
            }
            model.addDocument(terms, targetClass);
        }
        return model;
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, DictionaryModel model) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        String content = ((TextDocument)classifiable).getContent();
        Iterator<String> iterator = new NGramIterator(content, featureSetting.getMinNGramLength(),
                featureSetting.getMaxNGramLength());
        Bag<String> termCounts = Bag.create();
        while (iterator.hasNext() && termCounts.uniqueItems().size() < featureSetting.getMaxTerms()) {
            termCounts.add(iterator.next());
        }
        CategoryEntries priors = new CategoryEntriesBuilder().add(model.getPriors()).create();
        for (Entry<String, Integer> termCount : termCounts.unique()) {
            String term = termCount.getKey();
            TermCategoryEntries categoryEntries = model.getCategoryEntries(term);
            int documentCount = termCount.getValue();
            int dictionaryCount = categoryEntries.getTotalCount();
            for (Category category : categoryEntries) {
                String categoryName = category.getName();
                // XXX priors#getProbability slows down, because it requires hashmap lookup and we have two nested loops
                // here, remove completely or put outside the loops and call it on the scorer through some separate hook
                double score = scorer.score(term, categoryName, category.getProbability(), dictionaryCount,
                        documentCount, priors.getProbability(categoryName));
                builder.add(categoryName, score);
            }
        }
        // If we have a category weight by matching terms from the document, use them to create the probability
        // distribution. Else wise return the prior probability distribution of the categories.
        if (builder.getTotalScore() == 0) {
            return priors;
        }
        return builder.create();
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        return classify(new TextDocument(text), model);
    }

}
