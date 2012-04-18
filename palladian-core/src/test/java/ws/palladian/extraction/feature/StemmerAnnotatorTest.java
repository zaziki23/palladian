package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.feature.LengthTokenRemover;
import ws.palladian.extraction.feature.StemmerAnnotator;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.helper.constants.Language;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;

public class StemmerAnnotatorTest {
    
    private final PipelineDocument document = new PipelineDocument("Let's try to stem some tokens in English language.");
    
    @Test(expected = IllegalStateException.class)
    public void testMissingTokenAnnotations() {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new StemmerAnnotator(Language.ENGLISH));
        pipeline.process(document);
    }
    
    @Test
    public void testStemmerAnnotator() {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StemmerAnnotator(Language.ENGLISH));
        pipeline.process(document);
        
        AnnotationFeature annotationFeature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();

        assertEquals(12, annotations.size());
        assertEquals("tri", annotations.get(3).getFeatureVector().get(StemmerAnnotator.PROVIDED_FEATURE_DESCRIPTOR).getValue());
        assertEquals("token", annotations.get(7).getFeatureVector().get(StemmerAnnotator.PROVIDED_FEATURE_DESCRIPTOR).getValue());
        assertEquals("languag", annotations.get(10).getFeatureVector().get(StemmerAnnotator.PROVIDED_FEATURE_DESCRIPTOR).getValue());
        
    }
    
    @Test
    public void testStopTokenRemover() {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StopTokenRemover(Language.ENGLISH));
        pipeline.process(document);
        
        AnnotationFeature annotationFeature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();
        
        assertEquals(7, annotations.size());
    }
    
    @Test
    public void testTokenLengthRemover() {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new LengthTokenRemover(2));
        pipeline.process(document);
        
        AnnotationFeature annotationFeature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();
        
        assertEquals(9, annotations.size());
    }

}
