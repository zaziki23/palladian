package ws.palladian.core;

import java.util.Map;

import ws.palladian.helper.collection.CollectionHelper;

public class FeatureVectorBuilder {

    private final Map<String, Value> valueMap = CollectionHelper.newHashMap();

    public FeatureVectorBuilder set(String name, double value) {
        valueMap.put(name, new ImmutableDoubleValue(value));
        return this;
    }

    public FeatureVectorBuilder set(String name, String value) {
        valueMap.put(name, new ImmutableStringValue(value));
        return this;
    }

    public FeatureVectorBuilder set(String name, boolean value) {
        valueMap.put(name, ImmutableBooleanValue.create(value));
        return this;
    }

    public FeatureVectorBuilder set(String name, Value value) {
        valueMap.put(name, value);
        return this;
    }

    public FeatureVector create() {
        return new ImmutableFeatureVector(valueMap);
    }

    public Instance create(String category) {
        return new ImmutableInstance(create(), category);
    }

    public Instance create(boolean category) {
        return new ImmutableInstance(create(), category);
    }

}
