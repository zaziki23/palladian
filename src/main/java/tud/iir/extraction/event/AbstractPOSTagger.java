/**
 * 
 */
package tud.iir.extraction.event;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public abstract class AbstractPOSTagger {

    /** the logger for this class */
    protected static final Logger LOGGER = Logger
            .getLogger(AbstractPOSTagger.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";

    /** model for open nlp pos-tagging */
    private Object model;

    /** name for the POS Tagger */
    private String name = "unknown";

    /** dict file for opennlp pos tagging */
    protected static final String MODEL_POS_OPENNLP_DICT = MODEL_PATH
            + "opennlp/postag/tagdict.txt";

    /** dict file for opennlp pos tagging */
    protected static final String MODEL_POS_OPENNLP = MODEL_PATH
            + "opennlp/postag/tag.bin.gz";

    /** model for opennlp tokenization */
    protected static final String MODEL_TOK_OPENNLP = MODEL_PATH
            + "opennlp/tokenize/EnglishTok.bin.gz";

    /** brown hidden markov model for lingpipe chunker */
    protected static final String MODEL_LINGPIPE_BROWN_HMM = MODEL_PATH
            + "lingpipe/pos-en-general-brown.HiddenMarkovModel";

    /** holds the tagged string. */
    private String taggedString;

    /** The Annotations. */
    private TagAnnotations tagAnnotations;

    /**
     * tags a string and writes the tags into @see {@link #tags} and @see
     * {@link #tokens}.
     * 
     * @param sentence
     */
    public abstract void tag(String sentence);

    /**
     * tags a string and writes the tags into @see {@link #tags} and @see
     * {@link #tokens}.
     * 
     * @param sentence
     * @param configModelFilePath
     */
    public abstract void tag(String sentence, String configModelFilePath);

    /**
     * loads model into @see {@link #model}
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract boolean loadModel(String configModelFilePath);

    /**
     * loads the default model into @see {@link #model}
     * 
     * @param configModelFilePath
     * @return
     */
    public abstract boolean loadModel();

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * also tags a sentence and returns the @see {@link #tags}
     * 
     * @param sentence
     * @return
     */
    public TagAnnotations getTags(String sentence) {
        this.tag(sentence);
        return this.getTagAnnotations();
    }

    /**
     * @return the tagAnnotations
     */
    public TagAnnotations getTagAnnotations() {
        return tagAnnotations;
    }

    /**
     * @param tagAnnotations
     *            the tagAnnotations to set
     */
    public void setTagAnnotations(TagAnnotations tagAnnotations) {
        this.tagAnnotations = tagAnnotations;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        final OpenNLPPOSTagger lppt = new OpenNLPPOSTagger();
        lppt.loadModel();

        final StopWatch sw = new StopWatch();
        sw.start();

        lppt.tag("Death toll rises after Indonesia tsunami.");
        LOGGER.info(lppt.getTagAnnotations().getTaggedString());

        sw.stop();
        LOGGER.info("time elapsed: " + sw.getElapsedTimeString());

    }

}
