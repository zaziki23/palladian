package ws.palladian.core.dataset;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator;

final class ImmutableFeatureInformation implements FeatureInformation {

	private final Map<String, Class<? extends Value>> nameValues;

	/** Instances are created by {@link FeatureInformationBuilder}.*/
	ImmutableFeatureInformation(Map<String, Class<? extends Value>> nameValues) {
		this.nameValues = nameValues;
	}

	@Override
	public Set<String> getFeatureNames() {
		return Collections.unmodifiableSet(nameValues.keySet());
	}

	@Override
	public Set<String> getFeatureNamesOfType(Class<? extends Value> valueType) {
		Set<String> featureNames = new LinkedHashSet<>();
		for (Entry<String, Class<? extends Value>> nameValue : nameValues.entrySet()) {
			if (valueType.isAssignableFrom(nameValue.getValue())) {
				featureNames.add(nameValue.getKey());
			}
		}
		return featureNames;
	}

	@Override
	public Iterator<FeatureInformationEntry> iterator() {
		return new AbstractIterator<FeatureInformationEntry>() {
			final Iterator<Entry<String, Class<? extends Value>>> it = nameValues.entrySet().iterator();
			@Override
			protected FeatureInformationEntry getNext() throws Finished {
				if (it.hasNext()) {
					final Entry<String, Class<? extends Value>> current = it.next();
					return new ImmutableFeatureInformationEntry(current.getKey(), current.getValue());
				}
				throw FINISHED;
			}
		};
	}
	
	@Override
	public int count() {
		return nameValues.size();
	}
	
	@Override
	public FeatureInformationEntry getFeatureInformation(String name) {
		for (FeatureInformationEntry entry : this) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FeatureInformation: ");
		boolean first = true;
		for (FeatureInformation.FeatureInformationEntry entry : this) {
			if (first) {
				first = false;
			} else {
				builder.append(", ");
			}
			builder.append(entry);
		}
		return builder.toString();
	}

}