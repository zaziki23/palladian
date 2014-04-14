package ws.palladian.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.EntryConverter;

final class ImmutableFeatureVector implements FeatureVector {
    
    private static final EntryConverter<String, Value> CONVERTER = new EntryConverter<String, Value>();
    
    private final Map<String, Value> valueMap;

    ImmutableFeatureVector(Map<String, Value> valueMap) {
        this.valueMap = valueMap;
    }

    @Override
    public Iterator<VectorEntry<String, Value>> iterator() {
//        return new AbstractIterator<Vector.VectorEntry<String, Value>>() {
//
//            Iterator<Entry<String, Value>> iterator = valueMap.entrySet().iterator();
//
//            @Override
//            protected VectorEntry<String, Value> getNext() throws Finished {
//                if (iterator.hasNext()) {
//                    final Entry<String, Value> entry = iterator.next();
//                    return new VectorEntry<String, Value>() {
//                        @Override
//                        public String key() {
//                            return entry.getKey();
//                        }
//
//                        @Override
//                        public Value value() {
//                            return entry.getValue();
//                        }
//                    };
//                }
//                throw FINISHED;
//            }
//        };
        return CollectionHelper.convert(valueMap.entrySet().iterator(), CONVERTER);
    }

    @Override
    public Value get(String k) {
        Value value = valueMap.get(k);
        return value != null ? value : NullValue.NULL;
    }

    @Override
    public int size() {
        return valueMap.size();
    }

    @Override
    public Set<String> keys() {
        return valueMap.keySet();
    }
    
    @Override
    public String toString() {
        return valueMap.toString();
    }

}
