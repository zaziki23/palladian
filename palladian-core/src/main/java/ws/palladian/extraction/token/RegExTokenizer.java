package ws.palladian.extraction.token;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link BaseTokenizer} implementation based on regular expressions. Tokens are matched against the specified regular
 * expressions.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.7
 */
public final class RegExTokenizer extends BaseTokenizer {
    
    /**
     * <p>
     * The pattern that needs to match for a token to be extracted as a new {@code Annotation}.
     * </p>
     */
    private final Pattern pattern;
    
    
    private final String featureName;
//    /**
//     * <p>
//     * The descriptor for the {@code Feature} this {@code PipelineProcessor} creates.
//     * </p>
//     */
//    private final FeatureDescriptor<TextAnnotationFeature> featureDescriptor;

//    /**
//     * <p>
//     * Creates a new {@code RegExTokenizer} creating token {@code Annotation}s with the provided
//     * {@link FeatureDescriptor} and annotating token matching the provided {@code pattern}.
//     * </p>
//     * 
//     * @param featureDescriptor The {@code FeatureDescriptor} identifying the annotated token.
//     * @param pattern The pattern that needs to match for a token to be extracted as a new {@code Annotation}.
//     */
//    public RegExTokenizer(final FeatureDescriptor<TextAnnotationFeature> featureDescriptor, final String pattern) {
//        this(featureDescriptor, Pattern.compile(pattern));
//    }

//    /**
//     * <p>
//     * Creates a new {@code RegExTokenizer} creating token {@code Annotation}s with the provided
//     * {@link FeatureDescriptor} and annotating token matching {@link Tokenizer#SPLIT_PATTERN}.
//     * </p>
//     * 
//     * @param featureDescriptor The {@code FeatureDescriptor} identifying the annotated token.
//     */
//    public RegExTokenizer(final FeatureDescriptor<TextAnnotationFeature> featureDescriptor) {
//        // The default case to keep compatibility to old code.
//        this(featureDescriptor, Tokenizer.SPLIT_PATTERN);
//    }

    /**
     * <p>
     * The no argument constructor using {@link Tokenizer#SPLIT_PATTERN} to annotate token and saving them as
     * {@link Feature} with the {@link FeatureDescriptor} {@link BaseTokenizer#PROVIDED_FEATURE_DESCRIPTOR}.
     * </p>
     * 
     */
    public RegExTokenizer() {
        this(PROVIDED_FEATURE, Tokenizer.SPLIT_PATTERN);
    }

    /**
     * <p>
     * Creates a new {@code RegExTokenizer} creating token {@code Annotation}s with the provided
     * {@link FeatureDescriptor} and annotating token matching the provided {@code pattern}.
     * </p>
     * 
     * @param featureDescriptor The {@code FeatureDescriptor} identifying the annotated token.
     * @param pattern The pattern that needs to match for a token to be extracted as a new {@code Annotation}.
     */
    public RegExTokenizer(String featureName, Pattern pattern) {
        super();

        Validate.notNull(featureName, "featureName must not be null");
        Validate.notNull(pattern, "pattern must not be null");

        this.pattern = pattern;
        this.featureName = featureName;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        Validate.notNull(document, "document must not be null");

        String text = document.getContent();
        FeatureVector featureVector = document.getFeatureVector();
        Matcher matcher = pattern.matcher(text);
        int index = 0;
        while (matcher.find()) {
            int startPosition = matcher.start();
            int endPosition = matcher.end();
            String value = text.substring(startPosition, endPosition);
            PositionAnnotation annotation = new PositionAnnotation(featureName, startPosition, endPosition, index++, value);
            featureVector.add(annotation);
        }
    }

//    /**
//     * @return the {@link FeatureDescriptor} used to identify the extracted {@code AnnotationFeature}.
//     */
//    public FeatureDescriptor<TextAnnotationFeature> getFeatureDescriptor() {
//        return featureDescriptor;
//    }

}
