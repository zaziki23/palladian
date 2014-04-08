package ws.palladian.classification;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.Validate;

/**
 * @author pk
 */
public final class ImmutableCategoryEntries extends AbstractCategoryEntries {

    /** The map with all {@link Category} entries, for quick access by category name. */
    private final Map<String, Category> entryMap;

    /** The most likely {@link Category}; determined and cached upon creation for quick access. */
    private final Category mostLikely;

    /** An empty instance. */
    public static final CategoryEntries EMPTY = new ImmutableCategoryEntries();

//    /**
//     * To be created by {@link CategoryEntriesBuilder} only.
//     * 
//     * @param probabilityMap The map with the entries.
//     */
//    ImmutableCategoryEntries(Map<String, Double> probabilityMap) {
//        Map<String, Category> entryMap = CollectionHelper.newHashMap();
//        Category mostLikely = null;
//        for (Entry<String, Double> entry : probabilityMap.entrySet()) {
//            String name = entry.getKey();
//            Double probability = entry.getValue();
//            Category category = new ImmutableCategory(name, probability);
//            entryMap.put(name, category);
//            if (mostLikely == null || mostLikely.getProbability() < probability) {
//                mostLikely = category;
//            }
//        }
//        this.entryMap = Collections.unmodifiableMap(entryMap);
//        this.mostLikely = mostLikely;
//    }

//    public ImmutableCategoryEntries(Iterable<? extends ImmutableCategory> categories) {
//        Map<String, Category> entryMap = CollectionHelper.newHashMap();
//        Category mostLikely = null;
//        for (ImmutableCategory category : categories) {
//            entryMap.put(category.getName(), category);
//            if (mostLikely == null || mostLikely.getProbability() < category.getProbability()) {
//                mostLikely = category;
//            }
//        }
//        this.entryMap = Collections.unmodifiableMap(entryMap);
//        this.mostLikely = mostLikely;
//    }
    
    /** Empty ImmutableCategoryEntries; use the constant {@link #EMPTY} */
    private ImmutableCategoryEntries() {
        this.entryMap = Collections.<String, Category> emptyMap();
        this.mostLikely = null;
    }

    public ImmutableCategoryEntries(Map<String, Category> entryMap, Category mostLikely) {
        Validate.notNull(entryMap, "entryMap must not be null");
        this.entryMap = entryMap;
        this.mostLikely = mostLikely;
    }

    @Override
    public Iterator<Category> iterator() {
        return entryMap.values().iterator();
    }

    @Override
    public Category getMostLikely() {
        return mostLikely;
    }

    @Override
    public Category getCategory(String categoryName) {
        return entryMap.get(categoryName);
    }

    @Override
    public int size() {
        return entryMap.size();
    }

    @Override
    public int hashCode() {
        return entryMap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableCategoryEntries other = (ImmutableCategoryEntries)obj;
        return entryMap.equals(other.entryMap);
    }

}
