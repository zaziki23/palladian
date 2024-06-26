package ws.palladian.classification.featureselection;

import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.math.NumericMatrix;
import ws.palladian.helper.math.NumericMatrix.NumericMatrixVector;

import java.util.Map;

/**
 * <p>
 * Merges the features that are ranked per class by the chi squared feature ranking strategy by averaging the scores
 * achieved by a feature for each class.
 * </p>
 *
 * @author Klemens Muthmann
 */
public final class AverageMergingStrategy implements SelectedFeatureMergingStrategy {

    @Override
    public FeatureRanking merge(NumericMatrix<String> chiSquareMatrix) {
        Map<String, Double> scores = new LazyMap<>(Factories.constant(0.));
        for (NumericMatrixVector<String> scoredValue : chiSquareMatrix.rows()) {
            String featureIdentifier = scoredValue.key().split("###")[0];
            double averageScore = scoredValue.sum() / scoredValue.size();
            scores.put(featureIdentifier, scores.get(featureIdentifier) + averageScore);
        }
        return new FeatureRanking(scores);
    }

}
