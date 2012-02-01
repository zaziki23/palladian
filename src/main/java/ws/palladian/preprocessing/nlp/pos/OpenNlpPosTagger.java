/**
 * 
 */
package ws.palladian.preprocessing.nlp.pos;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.Cache;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.StopWatch;
import ws.palladian.preprocessing.nlp.TagAnnotation;
import ws.palladian.preprocessing.nlp.TagAnnotations;

/**
 * @author Martin Wunderwald
 */
public class OpenNlpPosTagger extends PosTagger {

    /** The tokenizer. **/
    private Tokenizer tokenizer;

    /** model file path. **/
    private final transient String MODEL;
    /** tokenizer model file path. **/
    private final transient String MODEL_TOK;

    public OpenNlpPosTagger() {
        super();
        setName("OpenNLP POS-Tagger");
        final PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

        MODEL = config.getString("models.root") + config.getString("models.opennlp.en.postag");
        MODEL_TOK = config.getString("models.root") + config.getString("models.opennlp.en.tokenize");
    }

    /**
     * @return the tokenizer
     */
    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#loadModel()
     */
    @Override
    public OpenNlpPosTagger loadModel() {
        return loadModel(MODEL).loadTokenizer(MODEL_TOK);
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#loadModel(java.lang.String)
     */
    @Override
    public OpenNlpPosTagger loadModel(final String modelFilePath) {

        POSTaggerME tagger = null;

        if (Cache.getInstance().containsDataObject(modelFilePath)) {

            tagger = (POSTaggerME) Cache.getInstance().getDataObject(modelFilePath);

        } else {
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {
                final POSModel model = new POSModel(new FileInputStream(modelFilePath));

                tagger = new POSTaggerME(model);
                Cache.getInstance().putDataObject(modelFilePath, tagger);

                stopWatch.stop();
                LOGGER.info("Reading " + getName() + " from file " + modelFilePath + " in "
                        + stopWatch.getElapsedTimeString());

            } catch (final InvalidFormatException e) {
                LOGGER.error(e);
            } catch (final FileNotFoundException e) {
                LOGGER.error(e);
            } catch (final IOException e) {
                LOGGER.error(e);
            }

        }

        setModel(tagger);

        return this;
    }

    /**
     * Loads the Tokenizer.
     * 
     * @param modelFilePath
     * @return
     */
    public OpenNlpPosTagger loadTokenizer(String modelFilePath) {

        InputStream modelIn;

        if (Cache.getInstance().containsDataObject(modelFilePath)) {

            setTokenizer((Tokenizer) Cache.getInstance().getDataObject(modelFilePath));

        } else {

            try {
                modelIn = new FileInputStream(modelFilePath);

                try {
                    final TokenizerModel model = new TokenizerModel(modelIn);
                    final Tokenizer tokenizer = new TokenizerME(model);

                    Cache.getInstance().putDataObject(modelFilePath, tokenizer);
                    setTokenizer(tokenizer);

                } catch (final IOException e) {
                    LOGGER.error(e);
                } finally {
                    if (modelIn != null) {
                        try {
                            modelIn.close();
                        } catch (final IOException e) {
                            LOGGER.error(e);
                        }
                    }
                }
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }
        return this;
    }

    /**
     * @param tokenizer
     *            the tokenizer to set
     */
    public void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public OpenNlpPosTagger tag(final String sentence) {

        final String[] tokens = getTokenizer().tokenize(sentence);

        final List<String> tokenList = new ArrayList<String>();
        for (final String token : tokens) {
            tokenList.add(token);
        }

        final List<String> tagList = ((POSTagger) getModel()).tag(tokenList);

        final TagAnnotations tagAnnotations = new TagAnnotations();
        for (int i = 0; i < tagList.size(); i++) {
            final TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(tokenList.get(i)), tagList.get(i),
                    tokenList.get(i));
            tagAnnotations.add(tagAnnotation);
        }

        setTagAnnotations(tagAnnotations);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#tag(java.lang.String,
     * java.lang.String)
     */
    @Override
    public OpenNlpPosTagger tag(final String sentence, final String modelFilePath) {
        return this.loadModel(modelFilePath).tag(sentence);
    }

}
