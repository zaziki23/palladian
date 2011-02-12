package tud.iir.classification.numeric;

import java.util.HashMap;
import java.util.Map;

public class MinMaxNormalization {

    private Map<Integer, Double> normalizationMap = new HashMap<Integer, Double>();
    private Map<Integer, Double> minValueMap = new HashMap<Integer, Double>();

    public Map<Integer, Double> getNormalizationMap() {
        return normalizationMap;
    }

    public void setNormalizationMap(Map<Integer, Double> normalizationMap) {
        this.normalizationMap = normalizationMap;
    }

    public void setMinValueMap(Map<Integer, Double> minValueMap) {
        this.minValueMap = minValueMap;
    }

    public Map<Integer, Double> getMinValueMap() {
        return minValueMap;
    }

}