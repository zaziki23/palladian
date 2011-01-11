/**
 * 
 */
package tud.iir.extraction.event;

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

import tud.iir.helper.ConfigHolder;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public class OpenNLPPOSTagger extends AbstractPOSTagger {

    /** The tokenizer. **/
    private Tokenizer tokenizer;

    /** model file path. **/
    private final String MODEL;
    /** tokenizer model file path. **/
    private final String MODEL_TOK;

    public OpenNLPPOSTagger() {
        super();
        setName("OpenNLP POS-Tagger");
        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            MODEL = config.getString("models.opennlp.en.postag");
            MODEL_TOK = config.getString("models.opennlp.en.tokenize");
        } else {
            MODEL = "";
            MODEL_TOK = "";
        }
    }

    /**
     * Loads the Tokenizer.
     * 
     * @param configModelFilePath
     * @return
     */
    public boolean loadTokenizer(String configModelFilePath) {

        InputStream modelIn;

        if (DataHolder.getInstance().containsDataObject(configModelFilePath)) {

            setTokenizer((Tokenizer) DataHolder.getInstance().getDataObject(
                    configModelFilePath));
            return true;
        } else {

            try {
                modelIn = new FileInputStream(configModelFilePath);

                try {
                    final TokenizerModel model = new TokenizerModel(modelIn);
                    final Tokenizer tokenizer = new TokenizerME(model);

                    DataHolder.getInstance().putDataObject(configModelFilePath,
                            tokenizer);
                    setTokenizer(tokenizer);

                    return true;
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
        return false;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#loadModel(java.lang.String)
     */
    @Override
    public boolean loadModel(final String configModelFilePath) {

        POSTaggerME tagger = null;

        if (DataHolder.getInstance().containsDataObject(configModelFilePath)) {

            tagger = (POSTaggerME) DataHolder.getInstance().getDataObject(
                    configModelFilePath);

        } else {
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {
                final POSModel model = new POSModel(new FileInputStream(
                        configModelFilePath));

                tagger = new POSTaggerME(model);
                DataHolder.getInstance().putDataObject(configModelFilePath,
                        tagger);

                stopWatch.stop();
                LOGGER.info("Reading " + this.getName() + " from file "
                        + configModelFilePath + " in "
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

        return true;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.POSTagger#tag(java.lang.String)
     */
    @Override
    public void tag(final String sentence) {

        final String[] tokens = getTokenizer().tokenize(sentence);

        final List<String> tokenList = new ArrayList<String>();
        for (String token : tokens) {
            tokenList.add(token);
        }

        final List<String> tagList = ((POSTagger) getModel()).tag(tokenList);

        final TagAnnotations tagAnnotations = new TagAnnotations();
        for (int i = 0; i < tagList.size(); i++) {
            final TagAnnotation tagAnnotation = new TagAnnotation(sentence
                    .indexOf(tokenList.get(i)), tagList.get(i), tokenList
                    .get(i));
            tagAnnotations.add(tagAnnotation);
        }

        this.setTagAnnotations(tagAnnotations);

    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#tag(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void tag(final String sentence, final String configModelFilePath) {
        this.loadModel(configModelFilePath);
        this.tag(sentence);
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractPOSTagger#loadModel()
     */
    @Override
    public boolean loadModel() {
        this.loadModel(MODEL);
        this.loadTokenizer(MODEL_TOK);
        return false;
    }

    /**
     * @return the tokenizer
     */
    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    /**
     * @param tokenizer
     *            the tokenizer to set
     */
    public void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

}
