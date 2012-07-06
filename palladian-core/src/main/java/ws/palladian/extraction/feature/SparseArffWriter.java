/**
 * Created on: 16.06.2012 19:27:56
 */
package ws.palladian.extraction.feature;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import weka.core.FastVector;
import weka.core.Instances;
import ws.palladian.extraction.patterns.SequentialPattern;
import ws.palladian.extraction.patterns.SequentialPatternsFeature;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.Port;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.BooleanFeature;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class SparseArffWriter extends AbstractPipelineProcessor<Object> {

    private static final Logger LOGGER = Logger.getLogger(SparseArffWriter.class);
    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -8674006178227544037L;
    /**
     * <p>
     * 
     * </p>
     */
    private final File targetFile;
    private final List<FeatureDescriptor<? extends Feature<?>>> featureDescriptors;
    private Instances model;

    private final BidiMap<String, Integer> featureTypes;
    private final List<List<Pair<Integer, String>>> instances;
    private Integer featuresAdded;

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code fileName}, creating that file if it does not exist and overwriting it if it already
     * exists.
     * </p>
     * 
     * @param fileName
     * @param featureDescriptors
     */
    public SparseArffWriter(final String fileName, final FeatureDescriptor<? extends Feature<?>>... featureDescriptors) {
        this(fileName, 1, featureDescriptors);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code fileName}, creating that file if it does not exist and overwriting it if it already
     * exists.
     * </p>
     * 
     * @param fileName
     * @param batchSize
     * @param featureDescriptors
     */
    public SparseArffWriter(final String fileName, final Integer batchSize,
            final FeatureDescriptor<? extends Feature<?>>... featureDescriptors) {
        super(Arrays.asList(new Port<?>[] {new Port<Object>(DEFAULT_INPUT_PORT_IDENTIFIER)}), new ArrayList<Port<?>>());

        Validate.notNull(fileName, "fileName must not be null");
        Validate.notEmpty(featureDescriptors, "featureDescriptors must not be empty");

        this.targetFile = new File(fileName);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        this.featureDescriptors = Arrays.asList(featureDescriptors);
        FastVector schema = new FastVector();
        this.model = new Instances("model", schema, batchSize);
        featureTypes = new DualHashBidiMap<String, Integer>();
        instances = new LinkedList<List<Pair<Integer, String>>>();
        featuresAdded = 0;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<Object> document = getDefaultInput();
        List<Pair<Integer, String>> newInstance = new LinkedList<Pair<Integer, String>>();
        for (Feature<?> feature : document.getFeatureVector()) {
            handleFeature(feature, newInstance);
        }
        instances.add(newInstance);
    }

    private void saveModel() throws IOException {
        LOGGER.info("Saving attributes:");
        FileOutputStream arffFileStream = new FileOutputStream(targetFile);
        IOUtils.write("@relation model\n\n ", arffFileStream);
        try {
            for (Integer i = 0; i < featuresAdded; i++) {
                String featureType = featureTypes.getKey(i);
                if (featureType == null) {
                    throw new IllegalStateException("No feature type at index: " + i + " expected to write "
                            + (featuresAdded - 1) + " feature types.");
                }
                IOUtils.write("@attribute " + featureType + "\n", arffFileStream);

                ProgressHelper.showProgress(i, featuresAdded, 5, LOGGER);
            }

            IOUtils.write("\n@data\n", arffFileStream);

            LOGGER.info("Saving instances:");
            int instanceCounter = 0;
            for (List<Pair<Integer, String>> instance : instances) {
                StringBuilder instanceBuilder = new StringBuilder("{");
                Collections.sort(instance);
                boolean isStart = true;
                for (Pair<Integer, String> feature : instance) {
                    // prepend a comma only if this is not the first feature.
                    if (!isStart) {
                        instanceBuilder.append(",");
                    }
                    isStart = false;
                    instanceBuilder.append(feature.getLeft());
                    instanceBuilder.append(" ");
                    instanceBuilder.append(feature.getRight());
                }
                instanceBuilder.append("}\n");
                IOUtils.write(instanceBuilder.toString(), arffFileStream);

                ProgressHelper.showProgress(instanceCounter, instances.size(), 5, LOGGER);
                instanceCounter++;
            }
        } finally {
            IOUtils.closeQuietly(arffFileStream);
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     */
    private void handleFeature(final Feature<?> feature, final List<Pair<Integer, String>> newInstance) {
        FeatureDescriptor descriptor = feature.getDescriptor();
        if (feature instanceof AnnotationFeature) {
            AnnotationFeature annotationFeature = (AnnotationFeature)feature;
            for (Annotation annotation : annotationFeature.getValue()) {
                for (Feature<?> subFeature : annotation.getFeatureVector()) {
                    handleFeature(subFeature, newInstance);
                }
            }
        }

        if (!featureDescriptors.contains(descriptor)) {
            return;
        }

        if (feature instanceof NumericFeature) {
            handleNumericFeature((NumericFeature)feature, newInstance);
        } else if (feature instanceof AnnotationFeature) {
            AnnotationFeature annotationFeature = (AnnotationFeature)feature;
            handleAnnotationFeature(annotationFeature, newInstance);
        } else if (feature instanceof BooleanFeature) {
            handleBooleanFeature((BooleanFeature)feature, newInstance);
        } else if (feature instanceof NominalFeature) {
            handleNominalFeature((NominalFeature)feature, newInstance);
        } else if (feature instanceof SequentialPatternsFeature) {
            handleSequentialPatterns((SequentialPatternsFeature)feature, newInstance);
        }
    }

    /**
     * <p>
     * Adds all sequential patterns from a {@code SequentialPatternsFeature} to the created Arff file.
     * </p>
     * 
     * @param feature The {@code Feature} to add.
     * @param newInstance The Weka {@code Instance} to add the {@code Feature} to
     * @param model
     */
    private void handleSequentialPatterns(final SequentialPatternsFeature feature,
            final List<Pair<Integer, String>> newInstance) {
        List<SequentialPattern> sequentialPatterns = feature.getValue();
        for (SequentialPattern pattern : sequentialPatterns) {
            String featureType = "\"" + pattern.getStringValue() + "\" numeric";

            Integer featureTypeIndex = featureTypes.get(featureType);
            if (featureTypeIndex == null) {
                featureTypes.put(featureType, featuresAdded);
                featureTypeIndex = featuresAdded;
                featuresAdded++;
            }

            ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex, "1.0");
            if (!newInstance.contains(featureValue)) {
                newInstance.add(featureValue);
            }
        }

    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     * @param newInstance
     * @param schema
     */
    private void handleNominalFeature(final NominalFeature feature, final List<Pair<Integer, String>> newInstance) {
        StringBuilder featureTypeBuilder = new StringBuilder("\"" + feature.getName() + "\" {dummy");

        for (String value : feature.getPossibleValues()) {
            featureTypeBuilder.append(",");
            featureTypeBuilder.append(value);
        }
        featureTypeBuilder.append("}");
        String featureType = featureTypeBuilder.toString();

        Integer featureTypeIndex = featureTypes.get(featureType);
        if (featureTypeIndex == null) {
            featureTypeIndex = featuresAdded;
            featureTypes.put(featureType, featureTypeIndex);
            featuresAdded++;
        }

        ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex,
                feature.getValue());
        if (!newInstance.contains(featureValue)) {
            newInstance.add(featureValue);
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     * @param newInstance
     * @param schema
     */
    private void handleBooleanFeature(BooleanFeature feature, List<Pair<Integer, String>> newInstance) {
        String featureType = "\"" + feature.getName() + "\" {dummy,true,false}";

        Integer featureTypeIndex = featureTypes.get(featureType);
        if (featureTypeIndex == null) {
            featureTypes.put(featureType, featuresAdded);
            featureTypeIndex = featuresAdded;
            featuresAdded++;
        }

        ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex, feature
                .getValue().toString());
        if (!newInstance.contains(featureValue)) {
            newInstance.add(featureValue);
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     * @param model
     * @param schema
     */
    private void handleAnnotationFeature(AnnotationFeature feature, List<Pair<Integer, String>> newInstance) {
        for (Annotation annotation : feature.getValue()) {
            String featureType = "\"" + annotation.getValue() + "\" numeric";

            Integer featureTypeIndex = featureTypes.get(featureType);
            if (featureTypeIndex == null) {
                featureTypes.put(featureType, featuresAdded);
                featureTypeIndex = featuresAdded;
                featuresAdded++;
            }

            ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex, "1.0");
            if (!newInstance.contains(featureValue)) {
                newInstance.add(featureValue);
            }
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     * @param model
     * @param schema
     */
    private void handleNumericFeature(NumericFeature feature, List<Pair<Integer, String>> newInstance) {
        String featureType = "\"" + feature.getName() + "\" numeric";

        Integer featureTypeIndex = featureTypes.get(featureType);
        if (featureTypeIndex == null) {
            featureTypes.put(featureType, featuresAdded);
            featureTypeIndex = featuresAdded;
            featuresAdded++;
        }

        ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex, feature
                .getValue().toString());
        if (!newInstance.contains(featureValue)) {
            newInstance.add(featureValue);
        }
    }

    @Override
    public void processingFinished() {
        try {
            saveModel();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
