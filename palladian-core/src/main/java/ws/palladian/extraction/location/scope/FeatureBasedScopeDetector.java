package ws.palladian.extraction.location.scope;

import static ws.palladian.extraction.location.LocationExtractorUtils.ANNOTATION_LOCATION_FUNCTION;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.Learner;
import ws.palladian.classification.Model;
import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.featureselection.BackwardFeatureElimination;
import ws.palladian.classification.featureselection.BackwardFeatureElimination.FMeasureScorer;
import ws.palladian.classification.featureselection.FeatureRanking;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.ClassifiableLocation;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.evaluation.TudLoc2013DatasetIterable;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.RegexFilter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.math.Stats;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVector;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A {@link ScopeDetector} which uses various features to train a model, which is then used for predicting the scope.
 * The features are mainly influenced from the rule-based {@link ScopeDetector}s (see implementations).
 * </p>
 * 
 * @author pk
 * @param <M> Type of the model, depending on the actual classifier.
 */
public final class FeatureBasedScopeDetector implements ScopeDetector {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedScopeDetector.class);

    /** Threshold between candidate and actual scope, under which a training instance is considered positive. */
    private static final int POSITIVE_DISTANCE_THRESHOLD = 50;

    private static final String NAME = "FeatureBased";

    private final QuickDtModel scopeModel;

    private final QuickDtClassifier classifier = new QuickDtClassifier();

    public FeatureBasedScopeDetector(QuickDtModel scopeModel) {
        Validate.notNull(scopeModel, "scopeModel must not be null");
        Validate.notNull(classifier, "classifier must not be null");
        this.scopeModel = scopeModel;
    }

    @Override
    public Location getScope(Collection<LocationAnnotation> annotations) {
        Validate.notNull(annotations, "locations must not be null");
        if (annotations.isEmpty()) {
            return null;
        }

        Set<ClassifiableLocation> classifiableLocations = extractFeatures(annotations);
        double maximumScore = Double.MIN_VALUE;
        Location selectedLocation = null;

        for (ClassifiableLocation location : classifiableLocations) {
            CategoryEntries classificationResult = classifier.classify(location, scopeModel);
            double score = classificationResult.getProbability("true");
            LOGGER.trace("{} : {}", location.getPrimaryName(), score);
            if (selectedLocation == null || score > maximumScore) {
                maximumScore = score;
                selectedLocation = location;
            }
        }

        return selectedLocation;
    }

    private static Set<ClassifiableLocation> extractFeatures(Collection<LocationAnnotation> annotations) {

        List<Location> locationList = CollectionHelper.convertList(annotations, ANNOTATION_LOCATION_FUNCTION);
        LocationStats stats = new LocationStats(locationList);
        Set<Location> locationSet = new HashSet<Location>(stats.getLocationsWithCoordinates());

        List<GeoCoordinate> coordinates = stats.getCoordinates();
        if (coordinates.isEmpty()) {
            return Collections.emptySet();
        }
        GeoCoordinate midpoint = stats.getMidpoint();
        GeoCoordinate centerpoint = stats.getCenterOfMinimumDistance();
        double maxDistanceMidpoint = stats.getMaxMidpointDistance();
        double maxDistanceCenterpoint = stats.getMaxCenterDistance();
        int maxHierarchyDepth = stats.getMaxHierarchyDepth();
        long biggestLocationPopulation = stats.getBiggestPopulation();
        int maxOffset = 1;
        for (LocationAnnotation annotation : annotations) {
            maxOffset = Math.max(maxOffset, annotation.getStartPosition());
        }
        double overallMaxDist = Math.max(1, stats.getLargestDistance());

        Set<ClassifiableLocation> instances = CollectionHelper.newHashSet();
        for (Location location : locationSet) {
            double midpointDistance = midpoint.distance(location.getCoordinate());
            double normalizedMidpointDistance = midpointDistance / maxDistanceMidpoint;
            double centerpointDistance = centerpoint.distance(location.getCoordinate());
            double normalizeCenterpointDistance = centerpointDistance / maxDistanceCenterpoint;
            int occurrenceCount = Collections.frequency(locationList, location);
            int descendantCount = stats.countDescendants(location);
            int ancestorCount = stats.countAncestors(location);
            double occurenceFrequency = (double)occurrenceCount / annotations.size();
            double childPercentage = (double)descendantCount / annotations.size();
            double ancestorPercentage = (double)ancestorCount / annotations.size();
            int hierarchyDepth = location.getAncestorIds().size();
            double normalizedHierarchyDepth = (double)hierarchyDepth / maxHierarchyDepth;
            long maxPopulation = Math.max(1, biggestLocationPopulation);
            long population = location.getPopulation() != null ? location.getPopulation() : 0;
            double maxDisambiguationTrust = 0;
            int firstPosition = Integer.MAX_VALUE;
            int lastPosition = Integer.MIN_VALUE;
            for (LocationAnnotation annotation : annotations) {
                if (annotation.getLocation().equals(location)) {
                    maxDisambiguationTrust = Math.max(maxDisambiguationTrust, annotation.getTrust());
                    firstPosition = Math.min(firstPosition, annotation.getStartPosition());
                    lastPosition = Math.max(lastPosition, annotation.getStartPosition());
                }
            }
            Stats distances = stats.getDistanceStats(location);

            FeatureVector featureVector = new BasicFeatureVector();
            featureVector.add(new NumericFeature("midpointDistance", midpointDistance));
            featureVector.add(new NumericFeature("normalizedMidpointDistance", normalizedMidpointDistance));
            featureVector.add(new NumericFeature("centerpointDistance", centerpointDistance));
            featureVector.add(new NumericFeature("normalizedCenterpointDistance", normalizeCenterpointDistance));
            featureVector.add(new NumericFeature("occurrenceCount", occurrenceCount));
            featureVector.add(new NumericFeature("childCount", descendantCount));
            featureVector.add(new NumericFeature("ancestorCount", ancestorCount));
            featureVector.add(new NumericFeature("occurenceFrequency", occurenceFrequency));
            featureVector.add(new NumericFeature("childPercentage", childPercentage));
            featureVector.add(new NumericFeature("ancestorPercentage", ancestorPercentage));
            featureVector.add(new NumericFeature("hierarchyDepth", hierarchyDepth));
            featureVector.add(new NumericFeature("normalizedHierarchyDepth", normalizedHierarchyDepth));
            featureVector.add(new NumericFeature("populationNorm", population / maxPopulation));
            featureVector.add(new NumericFeature("populationMagnitude", MathHelper.getOrderOfMagnitude(population)));
            featureVector.add(new NominalFeature("locationType", location.getType().toString()));
            featureVector.add(new NumericFeature("disambiguationTrust", maxDisambiguationTrust));
            featureVector.add(new NumericFeature("offsetFirst", (double)firstPosition / maxOffset));
            featureVector.add(new NumericFeature("offsetLast", (double)lastPosition / maxOffset));
            featureVector.add(new NumericFeature("offsetSpread", (double)(lastPosition - firstPosition) / maxOffset));
            double minDistance = Double.isNaN(distances.getMin()) ? 0 : distances.getMin();
            double maxDistance = Double.isNaN(distances.getMax()) ? 0 : distances.getMax();
            double meanDistance = Double.isNaN(distances.getMean()) ? 0 : distances.getMean();
            double medianDistance = Double.isNaN(distances.getMedian()) ? 0 : distances.getMedian();
            featureVector.add(new NumericFeature("minDistanceToOthers", minDistance));
            featureVector.add(new NumericFeature("maxDistanceToOthers", maxDistance));
            featureVector.add(new NumericFeature("meanDistanceToOthers", meanDistance));
            featureVector.add(new NumericFeature("medianDistanceToOthers", medianDistance));
            featureVector.add(new NumericFeature("normalizedMinDistanceToOthers", minDistance / overallMaxDist));
            featureVector.add(new NumericFeature("normalizedMaxDistanceToOthers", maxDistance / overallMaxDist));
            featureVector.add(new NumericFeature("normalizedMeanDistanceToOthers", meanDistance / overallMaxDist));
            featureVector.add(new NumericFeature("normalizedMedianDistanceToOthers", medianDistance / overallMaxDist));

            instances.add(new ClassifiableLocation(location, featureVector));
        }
        return instances;
    }

    @Override
    public String toString() {
        return NAME + ":" + classifier.getClass().getSimpleName();
    }

    /**
     * <p>
     * Train a new model for location scope detection. The dataset is represented by the {@link Iterator}.
     * </p>
     * 
     * @param documentIterator The iterator representing the dataset, not <code>null</code>.
     * @param extractor The {@link LocationExtractor}, not <code>null</code>.
     * @param modelFile The file path where the created model is to be stored, <code>null</code> to train no model.
     * @param csvFile The file path where to write the CSV with the instances, <code>null</code> to write no CSV file.
     * @param learner The {@link Learner}, in case a model is to be trained, <code>null</code> otherwise.
     * @param featureFilter A filter to exclude specific features, not <code>null</code>. Set to {@link Filter#ACCEPT}
     *            to remove no features.
     * @throws IOException In case of an I/O exception when writing the model.
     */
    public static void train(Iterable<LocationDocument> documentIterator, LocationExtractor extractor, File modelFile,
            File csvFile, Learner<? extends Model> learner, Filter<? super String> featureFilter) throws IOException {
        Validate.notNull(documentIterator, "documentIterator must not be null");

        Collection<Trainable> instances = CollectionHelper.newHashSet();
        for (LocationDocument trainDocument : documentIterator) {
            List<LocationAnnotation> annotations = extractor.getAnnotations(trainDocument.getText());
            Location mainLocation = trainDocument.getMainLocation();
            if (annotations.isEmpty() || mainLocation == null) {
                continue;
            }

            Set<ClassifiableLocation> classifiableLocations = extractFeatures(annotations);

            // 1) determine closest location to actual scope
            ClassifiableLocation positiveCandidate = null;
            double minDistance = Double.MAX_VALUE;
            for (ClassifiableLocation location : classifiableLocations) {
                double currentDistance = mainLocation.getCoordinate().distance(location.getCoordinate());
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    positiveCandidate = location;
                }
            }

            // 2) check, if we are in range
            if (minDistance > POSITIVE_DISTANCE_THRESHOLD) {
                positiveCandidate = null;
                LOGGER.warn("Could not determine positive candidate, distance to closest is {}", minDistance);
                // XXX maybe it would make more sense, to drop all training examples in this case?
            } else {
                LOGGER.trace("Distance between actual and training candidate is {}", minDistance);
            }

            // 3) create positive and negative training examples
            for (ClassifiableLocation location : classifiableLocations) {
                boolean positive = location == positiveCandidate;
                instances.add(new Instance(positive, location));
            }
        }

        // maybe filter (some) features
        if (featureFilter != null) {
            instances = ClassificationUtils.filterFeatures(instances, featureFilter);
        }
        // write CSV file if requested
        if (csvFile != null) {
            ClassificationUtils.writeCsv(instances, csvFile);
        }

        // build the model
        if (modelFile != null) {
            StopWatch stopWatch = new StopWatch();
            Model scopeModel = learner.train(instances);
            FileHelper.serialize(scopeModel, modelFile.getPath());
            LOGGER.info("Trained model with {} in {} and wrote to {}", learner, stopWatch.getElapsedTimeString(),
                    modelFile);
        }
    }

    /**
     * <p>
     * Run a backward feature elimination.
     * </p>
     * 
     * @param trainingCsv The CSV file with the training data, not <code>null</code>.
     * @param validationCsv The CSV file with the validation data, not <code>null</code>.
     * @param learner The learner, not <code>null</code>.
     * @param predictor The predictor, not <code>null</code>.
     */
    public static <M extends Model> void runFeatureElimination(File trainingCsv, File validationCsv,
            Learner<M> learner, Classifier<M> predictor) {
        List<Trainable> trainSet = new CsvDatasetReader(trainingCsv).readAll();
        List<Trainable> validationSet = new CsvDatasetReader(validationCsv).readAll();

        FMeasureScorer scorer = new FMeasureScorer("true");
        BackwardFeatureElimination<M> featureElimination = new BackwardFeatureElimination<M>(learner, predictor, scorer);
        FeatureRanking featureRanking = featureElimination.rankFeatures(trainSet, validationSet);
        CollectionHelper.print(featureRanking.getAll());
    }

    public static void main(String[] args) throws IOException {
        QuickDtModel model = FileHelper.deserialize("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/Models/location_disambiguation_all_train_1377442726898.model");
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // note that we are using a zero confidence threshold here; experiments showed, that it's better to go for high
        // recall here, and let the classifier scope detection's classifier decide about each candidate (this is at
        // least the case in the Wikipedia dataset; on the TUD-Loc-2013, it actually harms performance, but we have much
        // less data here for making a definite statement).
        FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(model, 0, 1000);
        LocationExtractor extractor = new PalladianLocationExtractor(database, disambiguation);

        // Wikipedia scope dataset //////////////////////////////////////////////////////////////////////////////
        // File trainingDirectory = new File("/Users/pk/Desktop/WikipediaScopeDataset-2014/split-1");
        // File trainingCsv = new File("/Users/pk/Desktop/scopeFeaturesWikipediaTraining.csv");
        // File validationDirectory = new File("/Users/pk/Desktop/WikipediaScopeDataset-2014/split-2");
        // File validationCsv = new File("/Users/pk/Desktop/scopeFeaturesWikipediaValidation.csv");
        // Iterable<LocationDocument> trainingSet = new WikipediaLocationScopeIterator(trainingDirectory);
        // Iterable<LocationDocument> validationSet = new WikipediaLocationScopeIterator(validationDirectory);

        // TUD-Loc-2013 dataset //////////////////////////////////////////////////////////////////////////////
        File trainingDirectory = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/1-training");
        // File trainingCsv = new File("/Users/pk/Desktop/scopeFeaturesTudLocTraining.csv");
        // File validationDirectory = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/2-validation");
        // File validationCsv = new File("/Users/pk/Desktop/scopeFeaturesTudLocValidation.csv");
        Iterable<LocationDocument> trainingSet = new TudLoc2013DatasetIterable(trainingDirectory);
        // Iterable<LocationDocument> validationSet = new TudLoc2013DatasetIterable(validationDirectory);

        // train(trainingSet, extractor, null, trainingCsv, null, null);
        // train(validationSet, extractor, null, validationCsv, null, null);
        // System.exit(0);

        // runFeatureElimination(trainingCsv, validationCsv, QuickDtLearner.randomForest(), new QuickDtClassifier());

        // feature set was determined using backward feature elimination, using train + validation set on Wikipedia set

        // QuickDt model
        Filter<String> featureFilter = new RegexFilter(
                "normalizedMidpointDistance|normalizedMedianDistanceToOthers|normalizedMinDistanceToOthers|normalizedCenterpointDistance|"
                        + "disambiguationTrust|ancestorCount|normalizedMeanDistanceToOthers|occurenceFrequency|minDistanceToOthers|"
                        + "populationMagnitude|offsetFirst|locationType|hierarchyDepth");
        File modelOutput = new File("scopeDetection_tud-loc_quickDt.model");
        train(trainingSet, extractor, modelOutput, null, QuickDtLearner.randomForest(), featureFilter);
    }

}
