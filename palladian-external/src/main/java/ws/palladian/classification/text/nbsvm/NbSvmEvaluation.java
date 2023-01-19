package ws.palladian.classification.text.nbsvm;

import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.vector.TextVectorClassifier;
import ws.palladian.core.*;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.TextValue;
import ws.palladian.extraction.text.vector.TextVectorizer;
import ws.palladian.extraction.text.vector.TextVectorizer.IDFStrategy;
import ws.palladian.extraction.text.vector.TextVectorizer.TFStrategy;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.math.ConfusionMatrix;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static ws.palladian.classification.text.BayesScorer.Options.*;
import static ws.palladian.extraction.text.vector.TextVectorizer.IDFStrategy.UNARY;
import static ws.palladian.extraction.text.vector.TextVectorizer.TFStrategy.BINARY;

/**
 * <p>
 * Runs an evaluation using the data generated by
 * <a href="https://github.com/mesnilgr/nbsvm">this NBSVM Python package</a>.
 *
 * <p>
 * The following files are necessary (generate them by running above's script):
 *
 * <li>test-neg.txt
 * <li>test-pos.txt
 * <li>train-neg.txt
 * <li>train-pos.txt
 * </ul>
 *
 * <p>
 * Specify the path to the directory containing above's files via command line
 * parameter.
 *
 * <p>
 * Using the following feature setting ...:
 *
 * <pre>
 * FeatureSettingBuilder.words(1, 2).termLength(1, Integer.MAX_VALUE).create();
 * </pre>
 *
 * <p>
 * ... and the default LibLinear settings for the {@link TextVectorClassifier}
 * and {@link NbSvmLearner} I achieved the following results:
 *
 * <p>
 * <table>
 * <tr>
 * <th>Classifier</th>
 * <th>Configuration</th>
 * <th>ROC-AUC</th>
 * <th>Accuracy</th>
 * </tr>
 * <tr>
 * <td>PalladianTextClassifier</td>
 * <td>BayesScorer (LAPLACE, PRIORS, FREQUENCIES)</td>
 * <td>0.9396</td>
 * <td>0.8665</td>
 * </tr>
 * <tr>
 * <td>TextVectorClassifier</td>
 * <td>DOUBLE_NORMALIZATION, IDF, alpha=0</td>
 * <td>0.9668</td>
 * <td>0.9089</td>
 * </tr>
 * <tr>
 * <td>NBSVM</td>
 * <td>DOUBLE_NORMALIZATION, UNARY, alpha=0</td>
 * <td>0.9704</td>
 * <td>0.9159</td>
 * </tr>
 * </table>
 *
 * See <a href="doc-files/evaluation_result.csv">here</a> for all details.
 *
 * @author Philipp Katz
 */
class NbSvmEvaluation {

    private static final String NEGATIVE = "0";

    private static final String POSITIVE = "1";

    private static final class DatasetReader extends AbstractDataset {
        private final List<Instance> instances = new ArrayList<>();

        public DatasetReader(File pos, File neg) {
            instances.addAll(readDocs(pos, POSITIVE));
            instances.addAll(readDocs(neg, NEGATIVE));
        }

        private static List<Instance> readDocs(File pos, final String category) {
            final List<Instance> instances = new ArrayList<>();
            FileHelper.performActionOnEveryLine(pos, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    instances.add(new InstanceBuilder().setText(line).create(category));
                }
            });
            return instances;
        }

        @Override
        public CloseableIterator<Instance> iterator() {
            return new CloseableIteratorAdapter<>(instances.iterator());
        }

        @Override
        public FeatureInformation getFeatureInformation() {
            return new FeatureInformationBuilder().set("text", TextValue.class).create();
        }

        @Override
        public long size() {
            return instances.size();
        }
    }

    private static class EvalRun<M extends Model> {

        private final Learner<M> learner;
        private final Classifier<M> classifier;

        public static <M extends Model> EvalRun<M> with(Learner<M> learner, Classifier<M> classifier) {
            return new EvalRun<>(learner, classifier);
        }

        public static <M extends Model, LC extends Learner<M> & Classifier<M>> EvalRun<M> with(LC learnerClassifier) {
            return new EvalRun<>(learnerClassifier, learnerClassifier);
        }

        private EvalRun(Learner<M> learner, Classifier<M> classifier) {
            this.learner = learner;
            this.classifier = classifier;
        }

        public void execute(Dataset train, Dataset test, File resultFile) {
            M model = learner.train(train);
            RocCurves rocCurves = new RocCurves.RocCurvesEvaluator(POSITIVE).evaluate(classifier, model, test);
            ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(classifier, model, test);
            StringBuilder csv = new StringBuilder();
            csv.append(learner).append(';');
            csv.append(rocCurves.getAreaUnderCurve()).append(';');
            csv.append(confusionMatrix.getAccuracy()).append('\n');
            FileHelper.appendFile(resultFile.getAbsolutePath(), csv);
        }

    }

    public static void main(String[] args) {
        File base = new File(args[0]);

        Dataset train = new DatasetReader(new File(base, "/train-pos.txt"), new File(base, "/train-neg.txt"));
        Dataset test = new DatasetReader(new File(base, "/test-pos.txt"), new File(base, "/test-neg.txt"));

        FeatureSetting fs = FeatureSettingBuilder.words(1, 2).termLength(1, MAX_VALUE).create();

        List<EvalRun<?>> eval = new ArrayList<>();

        // PalladianTextClassifier using different scorers
        eval.add(EvalRun.with(new PalladianTextClassifier(fs)));

        eval.add(EvalRun.with(new PalladianTextClassifier(fs, new BayesScorer(LAPLACE))));
        eval.add(EvalRun.with(new PalladianTextClassifier(fs, new BayesScorer(LAPLACE, PRIORS))));
        eval.add(EvalRun.with(new PalladianTextClassifier(fs, new BayesScorer(LAPLACE, PRIORS, FREQUENCIES))));

        // evaluate all TFStrategy x IDFStrategy combinations (5 x 4 = 20)
        // using (1) TextVectorClassifier, (2) NBSVM
        final TextVectorizer vectorizer = new TextVectorizer("text", fs, train, BINARY, UNARY, MAX_VALUE);
        for (TFStrategy tfStrategy : TFStrategy.values()) {
            for (IDFStrategy idfStrategy : IDFStrategy.values()) {
                TextVectorizer currentVectorizer = vectorizer.copyWithDifferentStrategy(tfStrategy, idfStrategy, 0);
                eval.add(EvalRun.with(TextVectorClassifier.libLinear(currentVectorizer)));
                eval.add(EvalRun.with(new NbSvmLearner(currentVectorizer), new NbSvmClassifier(currentVectorizer)));
            }
        }

        System.out.println("Performing " + eval.size() + " runs");
        ProgressMonitor progress = new ProgressMonitor(eval.size());
        File resultFile = new File(base, "evaluation_result_" + DateHelper.getCurrentDatetime() + ".csv");
        FileHelper.appendFile(resultFile.getAbsolutePath(), "classifier;roc-auc;accuracy\n");

        for (EvalRun<?> e : eval) {
            e.execute(train, test, resultFile);
            progress.increment();
        }

    }

}
