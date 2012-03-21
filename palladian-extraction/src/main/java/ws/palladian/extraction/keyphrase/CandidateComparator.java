package ws.palladian.extraction.keyphrase;

import java.util.Comparator;


public class CandidateComparator implements Comparator<Candidate> {

    @Override
    public int compare(Candidate cand0, Candidate cand1) {
        return new Double(cand1.getRegressionValue()).compareTo(cand0.getRegressionValue());
    }

}