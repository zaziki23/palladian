/**
 * Created on: 14.08.2012 14:41:48
 */
package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * A feature collecting multiple values as a list.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 3.0.0
 */
public final class ListFeature extends Feature<List<Object>> {

    /**
     * {@inheritDoc #ListFeature(FeatureDescriptor, List)}
     */
    public ListFeature(FeatureDescriptor<? extends Feature<List<Object>>> descriptor, List<Object> value) {
        super(descriptor, value);
    }

    /**
     * <p>
     * Creates a new {@code ListFeature} using a descriptor and all the values from the provided array.
     * </p>
     * 
     * @param descriptor The descriptor identifying the new {@code Feature} within a {@link FeatureVector}.
     * @param value The array containing all the values for the new {@code Feature}.
     */
    public ListFeature(FeatureDescriptor<? extends Feature<List<Object>>> descriptor, Object[] value) {
        super(descriptor, Arrays.asList(value));
    }

    /**
     * {@inheritDoc #ListFeature(String, List)}
     */
    public ListFeature(String name, List<Object> value) {
        super(name, value);
    }

    /**
     * @return the list backing this {@code Feature}.
     */
    public List<Object> getList() {
        return new ArrayList<Object>(getValue());
    }

}