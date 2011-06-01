package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class DuplicateTokenRemover implements PipelineProcessor {

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        Set<String> tokenValues = new HashSet<String>();
        
        List<Annotation> resultTokens = new ArrayList<Annotation>();
        for (Iterator<Annotation> tokenIterator = annotations.iterator(); tokenIterator.hasNext();) {
            Annotation annotation = tokenIterator.next();
            String tokenValue = annotation.getValue().toLowerCase();
            if (tokenValues.add(tokenValue)) {
                resultTokens.add(annotation);
            }
        }
        annotationFeature.setValue(resultTokens);
        
//        for (Iterator<Annotation> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
//            Annotation token = tokenIterator.next();
//            String tokenValue = token.getValue().toLowerCase();
//            if (!tokenValues.add(tokenValue)) {
//                tokenIterator.remove();
//            }
//        }
    }

}
