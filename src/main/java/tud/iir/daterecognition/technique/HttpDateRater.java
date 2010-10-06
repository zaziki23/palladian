package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;

/**
 * This class rates HTTP-dates by constant and age of date.
 * 
 * @author Martin Greogr
 * 
 */
public class HttpDateRater extends TechniqueDateRater<HTTPDate> {

    @Override
    public HashMap<HTTPDate, Double> rate(ArrayList<HTTPDate> list) {
        return evaluateHTTPDate(list);
    }

    /**
     * Evaluates HTTP dates.<br>
     * Therefore, a date older then 12 hours from this point of time will be rated with 0.75.
     * If more then one date exists, a weight reduces each rating in dependency of age.
     * 
     * @param httpDates
     * @return
     */
    private HashMap<HTTPDate, Double> evaluateHTTPDate(ArrayList<HTTPDate> httpDates) {
        HTTPDate date = null;
        HashMap<HTTPDate, Double> result = new HashMap<HTTPDate, Double>();
        double rate = 0;
        for (int i = 0; i < httpDates.size(); i++) {
            date = httpDates.get(i);
            ExtractedDate current = ExtractedDateHelper.createActualDate();
            DateComparator dc = new DateComparator();
            double timedifference = dc.getDifference(httpDates.get(0), current, DateComparator.MEASURE_HOUR);

            if (timedifference > 12) {
                rate = 0.75;// 75% aller Webseiten haben richtigen last modified tag, aber bei dif. von 3h ist dies zu
                // nah an
                // expiere
            } else {
                rate = 0;
            }
            result.put(date, rate);
        }

        DateComparator dc = new DateComparator();
        ArrayList<HTTPDate> dates = dc.orderDates(result);
        HTTPDate oldest = dc.getOldestDate(DateArrayHelper.getExactestMap(result));

        double diff;
        double oldRate;
        double newRate;

        for (int i = 0; i < dates.size(); i++) {
            diff = dc.getDifference(oldest, dates.get(i), DateComparator.MEASURE_HOUR);
            if (diff > 24) {
                diff = 24;
            }
            date = dates.get(i);
            oldRate = result.get(date);
            newRate = oldRate - (oldRate * (diff / 24.0));
            result.put(date, Math.round(newRate * 10000) / 10000.0);
        }

        return result;
    }

}
