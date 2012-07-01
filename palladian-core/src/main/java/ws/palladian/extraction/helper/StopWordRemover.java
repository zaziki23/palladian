package ws.palladian.extraction.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.PipelineDocument;

public class StopWordRemover extends StringDocumentPipelineProcessor {

    private static final long serialVersionUID = 5014188120999997379L;

    private List<String> stopWords;

    public StopWordRemover() {
        InputStream stream = this.getClass().getResourceAsStream("/stopwords_en.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        stopWords = FileHelper.readFileToArray(br);
    }

    public String removeStopWords(String text) {

        for (String stopWord : stopWords) {

            // skip comment lines
            if (stopWord.startsWith("#")) {
                continue;
            }

            text = StringHelper.removeWord(stopWord, text);
        }

        return text;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        String content = document.getContent();

        content = removeStopWords(content);

        document.setContent(content);
    }

    public static void main(String[] args) {
        StopWordRemover stopWordRemover = new StopWordRemover();
        System.out.println(stopWordRemover.removeStopWords("is the"));
    }
}
