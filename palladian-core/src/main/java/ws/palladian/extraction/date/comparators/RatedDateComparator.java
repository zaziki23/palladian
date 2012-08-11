package ws.palladian.extraction.date.comparators;

import java.util.Comparator;

import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.ExtractedDate;

/**
 * Compare rated dates.<br>
 * First parameter are rates of dates. <br>
 * If these are equal. Contentdates will be compared by position in document. <br>
 * All other dates will be compared by technique. For order check out static TECH properties of {@link ExtractedDate}.<br>
 * If these are equal too, last comparison is age.<br>
 * <br>
 * Be careful to set rates before using this comparator. If no rates are set, the all will be equal with -1.
 * 
 * 
 * @author Martin Gregor
 * 
 * @param <T>
 */
public class RatedDateComparator implements Comparator<ExtractedDate> {

    @Override
    public int compare(ExtractedDate date1, ExtractedDate date2) {
        int result = compareRate(date1, date2);
        if (result == 0) {
            // if (date1.getType().equals(DateType.ContentDate) && date2.getType().equals(DateType.ContentDate)) {
            if (date1 instanceof ContentDate && date2 instanceof ContentDate) {
                result = comparePosInDoc((ContentDate)date1, (ContentDate)date2);
            } else {
                result = compareTechnique(date1, date2);
            }
        }
        if (result == 0) {
            result = compareAge(date1, date2);
        }
        return result;
    }

    /**
     * <p>
     * Compare by rate.
     * </p>
     * 
     * @param date1
     * @param date2
     * @return
     */
    private int compareRate(ExtractedDate date1, ExtractedDate date2) {
        double rate1 = date1.getRate();
        double rate2 = date2.getRate();
        return rate1 < rate2 ? 1 : rate1 > rate2 ? -1 : 0;
    }

    /**
     * <p>
     * Compare {@link ContentDate}s by position in document.
     * </p>
     * 
     * @param date1
     * @param date2
     * @return
     */
    private int comparePosInDoc(ContentDate date1, ContentDate date2) {
        int pos1 = date1.get(ContentDate.DATEPOS_IN_DOC);
        int pos2 = date2.get(ContentDate.DATEPOS_IN_DOC);
        return pos1 > pos2 ? 1 : pos1 < pos2 ? -1 : 0;
    }

    /**
     * <p>
     * Compare by technique.
     * </p>
     * 
     * @param date1
     * @param date2
     * @return
     */
    private int compareTechnique(ExtractedDate date1, ExtractedDate date2) {
//        int tech1 = date1.getType().getIntType();
//        int tech2 = date2.getType().getIntType();
//        if (tech1 == 0) {
//            tech1 = 99;
//        }
//        if (tech2 == 0) {
//            tech2 = 99;
//        }
//        return tech1 > tech2 ? 1 : tech1 < tech2 ? -1 : 0;
        int tech1 = getTypeValue(date1);
        int tech2 = getTypeValue(date2);
        return Integer.valueOf(tech1).compareTo(tech2);
    }

    private int getTypeValue(ExtractedDate date) {
        if (date instanceof StructureDate) {
            return 4;
        }
        if (date instanceof MetaDate) {
            return 3;
        }
        if (date instanceof ContentDate) {
            return 2;
        }
        if (date instanceof UrlDate) {
            return 1;
        }
        return 99;
//        // TODO Auto-generated method stub
//        return 0;
    }

    /**
     * <p>
     * Compare by age.
     * </p>
     * 
     * @param date1
     * @param date2
     * @return
     */
    private int compareAge(ExtractedDate date1, ExtractedDate date2) {
        // int stopFlag = Math.min(date1.getExactness(), date2.getExactness());
        // DateExactness compareDepth = DateExactness.byValue(stopFlag);
        DateExactness compareDepth = DateExactness.getCommonExactness(date1.getExactness(), date2.getExactness());
        DateComparator dc = new DateComparator(compareDepth);
        return dc.compare(date1, date2);

    }
}
