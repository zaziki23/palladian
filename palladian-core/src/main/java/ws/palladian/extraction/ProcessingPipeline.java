package ws.palladian.extraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * A pipeline handling information processing components implemented by {@link PipelineProcessor}s to process
 * {@link PipelineDocument}s.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class ProcessingPipeline implements Serializable {

    /**
     * <p>
     * Unique number used to identify serialized versions of this object. This value change only but each time the
     * serialized schema of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -6173687204106619909L;

    /**
     * <p>
     * The processors this pipeline will execute as ordered by this list from the first to the last.
     * </p>
     */
    private final List<PipelineProcessor> pipelineProcessors;

    /**
     * <p>
     * Creates a new {@code ProcessingPipeline} without any {@code PipelineProcessor}s. Add processors using
     * {@link #add(PipelineProcessor)} to get a functional {@code ProcessingPipeline}.
     * </p>
     */
    public ProcessingPipeline() {
        pipelineProcessors = new ArrayList<PipelineProcessor>();
    }
    
    /**
     * <p>
     * Creates a new {@link ProcessingPipeline} with the {@link PipelineProcessor}s from the supplied
     * {@link ProcessingPipeline}. The newly created {@link ProcessingPipeline} will use the instances of the
     * {@link PipelineProcessor}s from the supplied {@link ProcessingPipeline}. In other words, a <i>shallow copy</i> of
     * the workflow is created, where {@link PipelineProcessor}s share their states.
     * </p>
     * 
     * @param processingPipeline The {@link ProcessingPipeline} from which the {@link PipelineProcessor}s will be added
     *            to the newly created instance.
     */
    public ProcessingPipeline(ProcessingPipeline processingPipeline) {
        pipelineProcessors = new ArrayList<PipelineProcessor>(processingPipeline.getPipelineProcessors());
    }

    /**
     * <p>
     * Adds a new processor for execution to this pipeline. The processor is appended as last step.
     * </p>
     * 
     * @param pipelineProcessor The new processor to add.
     */
    public final void add(PipelineProcessor pipelineProcessor) {
        pipelineProcessors.add(pipelineProcessor);
    }

    /**
     * <p>
     * Provides the list of all {@code PipelineProcessor}s currently registered at this pipeline. The list is in the
     * same order as the processors are executed beginning from the first and ending with the last.
     * </p>
     * 
     * @return The list of registered {@code PipelineProcessor}s.
     */
    public final List<PipelineProcessor> getPipelineProcessors() {
        return Collections.unmodifiableList(pipelineProcessors);
    }

    /**
     * <p>
     * Starts processing of the provided document in this pipeline, running it through all currently registered
     * processors.
     * </p>
     * 
     * @param document The document to process.
     * @return The processed document is returned. This should be the same instance as provided. However this is not
     *         guaranteed. The returned document contains all features and modified representations created by the
     *         pipeline.
     */
    public PipelineDocument process(PipelineDocument document) throws DocumentUnprocessableException {
        for (PipelineProcessor processor : pipelineProcessors) {
            processor.process(document);
        }
        return document;
    }
    
    @Override
    public final String toString() {
        return pipelineProcessors.toString();
    }

}
