package ws.palladian.extraction.date.technique;

import java.util.ArrayList;
import java.util.HashMap;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.dates.URLDate;
import ws.palladian.helper.RegExp;

/**
 * 
 * This class evaluates an url-date and rates it in dependency of found format.<br>
 * 
 * @author Martin Gregor
 * 
 */
public class UrlDateRater extends TechniqueDateRater<URLDate> {

    public UrlDateRater(PageDateType dateType) {
		super(dateType);
	}

	@Override
    public HashMap<URLDate, Double> rate(ArrayList<URLDate> list) {
        return evaluateURLDate(list);
    }

    /**
     * Evaluates the URL dates.<br>
     * Evaluated rate depends on format of date.<br>
     * 
     * @param dates
     * @return
     */
    private HashMap<URLDate, Double> evaluateURLDate(ArrayList<URLDate> dates) {
        HashMap<URLDate, Double> evaluate = new HashMap<URLDate, Double>();
        for (int i = 0; i < dates.size(); i++) {
            double rate = 0;
            URLDate date = dates.get(i);
            if (date != null && DateRaterHelper.isDateInRange(date)) {
                String format = date.getFormat();
                if (format != null) {
                    if (format.equalsIgnoreCase(RegExp.DATE_URL_D[1])) {
                        rate = 0.95;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
                        rate = 0.98;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL[1])) {
                        rate = 0.99;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D[1])) {
                        rate = 1.0;
                    } else {
                        rate = 0.88; // TODO: rate genau bestimmen.
                    }
                }
            }
            evaluate.put(date, rate);
        }
        this.ratedDates = evaluate;
        return evaluate;
    }

}
