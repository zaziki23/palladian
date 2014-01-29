package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * This class provides some helper methods for working with collections. <b>Important:</b> If you are looking for a
 * functionality which is not provided here, look in {@link Collections}, {@link Arrays} first, before adding new,
 * redundant methods here!
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class CollectionHelper {

    /**
     * <p>
     * Indicate sorting order.
     * </p>
     */
    public enum Order {
        ASCENDING, DESCENDING
    }

    /**
     * <p>
     * Iterator, which stops after the specified limit.
     * </p>
     * 
     * @author pk
     * 
     * @param <T>
     */
    private static final class LimitIterator<T> implements Iterator<T> {

        final Iterator<T> iterator;
        final int limit;
        int counter = 0;

        LimitIterator(Iterator<T> iterator, int limit) {
            this.iterator = iterator;
            this.limit = limit;
        }

        @Override
        public boolean hasNext() {
            if (counter >= limit) {
                return false;
            }
            return iterator.hasNext();
        }

        @Override
        public T next() {
            if (counter >= limit) {
                throw new NoSuchElementException();
            }
            T temp = iterator.next();
            counter++;
            return temp;
        }

        @Override
        public void remove() {
            iterator.remove();
        }

    }

    private CollectionHelper() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Sort a {@link Map} by value.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The {@link Map} to sort.
     * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
     * @return A sorted map.
     *         XXX {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *         {@link Pair}s, sorted as required.
     */
    public static <K, V extends Comparable<V>> Map<K, V> sortByValue(Map<K, V> map, final Order order) {

        LinkedList<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new EntryValueComparator<V>(order));

        LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * <p>
     * Sort a {@link Map} by value.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The {@link Map} to sort.
     * @return A sorted map, in ascending order.
     */
    public static <K, V extends Comparable<V>> Map<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map, Order.ASCENDING);
    }

    /**
     * <p>
     * Sort a {@link HashMap} by length of the key string.
     * </p>
     * 
     * @param <K> Type of the keys.
     * @param <V> Type of the values.
     * @param map The entry set.
     * @param ascending {@link CollectionHelper#ASCENDING} or {@link CollectionHelper#DESCENDING}.
     * @return A sorted map.
     * @deprecated {@link Map}s are <b>not</b> meant for this use case. Prefer using a {@link List} populated with
     *             {@link Pair}s, sorted as required.
     */
    @Deprecated
    public static <V extends Comparable<V>> LinkedHashMap<String, V> sortByStringKeyLength(Map<String, V> map,
            final Order order) {

        LinkedList<Map.Entry<String, V>> list = new LinkedList<Map.Entry<String, V>>(map.entrySet());

        Comparator<Map.Entry<String, V>> comparator = new Comparator<Map.Entry<String, V>>() {
            @Override
            public int compare(Map.Entry<String, V> o1, Map.Entry<String, V> o2) {
                int ret = new Integer(o1.getKey().length()).compareTo(o2.getKey().length());
                return order == Order.ASCENDING ? ret : -ret;
            }
        };
        Collections.sort(list, comparator);

        LinkedHashMap<String, V> result = new LinkedHashMap<String, V>();
        for (Entry<String, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * <p>
     * Get a key given for a value (1 to 1 {@link Map}s).
     * </p>
     * 
     * @param value The value.
     * @return The key that matches the given value, or <code>null</code> if no such value.
     */
    public static <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Entry<K, V> mapEntry : map.entrySet()) {
            if (mapEntry.getValue().equals(value)) {
                return mapEntry.getKey();
            }
        }
        return null;
    }

    /**
     * <p>
     * Print a human readable, line separated output of an Array.
     * </p>
     * 
     * @param array
     */
    public static void print(Object[] array) {
        for (Object o : array) {
            System.out.println(o);
        }
        System.out.println("#Entries: " + array.length);
    }

    /**
     * <p>
     * Print a human readable, line separated output of a {@link Map}.
     * </p>
     * 
     * @param <K>
     * @param <V>
     * @param map
     */
    public static <K, V> void print(Map<K, V> map) {
        print(map, -1);
    }

    public static <K, V> void print(Map<K, V> map, int limit) {
        int c = 0;
        Iterator<Map.Entry<K, V>> mapIterator = map.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<K, V> entry = mapIterator.next();
            System.out.println(entry.getKey() + " : " + entry.getValue());
            c++;
            if (c >= limit && limit > -1) {
                break;
            }
        }
        System.out.println("#Entries: " + map.entrySet().size());
    }

    /**
     * <p>
     * Get a human readable, line separated output of an {@link Iterable}.
     * </p>
     * 
     * @param iterable
     * @return
     */
    public static String getPrint(Iterable<?> iterable) {
        StringBuilder print = new StringBuilder();
        int count = 0;
        for (Object entry : iterable) {
            print.append(entry).append("\n");
            count++;
        }
        print.append("#Entries: ").append(count).append("\n");
        return print.toString();
    }

    /**
     * <p>
     * Print a human readable, line separated output of an {@link Iterable}.
     * </p>
     * 
     * @param iterable
     */
    public static void print(Iterable<?> iterable) {
        System.out.println(getPrint(iterable));
    }

    /**
     * <p>
     * Concatenate two String arrays.
     * </p>
     * 
     * @param array1
     * @param array2
     * @return The concatenated String array consisting of the first, then the second array's items.
     */
    public static String[] concat(String[] array1, String[] array2) {
        String[] helpArray = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, helpArray, 0, array1.length);
        System.arraycopy(array2, 0, helpArray, array1.length, array2.length);
        return helpArray;
    }

    /**
     * <p>
     * Create a new {@link HashMap}. This method allows omitting the type parameter when creating the HashMap:
     * <code>Map&lt;String, Integer&gt; map = CollectionHelper.newHashMap();</code>.
     * </p>
     * 
     * @return A new {@link HashMap}.
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<K, V>();
    }

    /**
     * <p>
     * Create a new {@link TreeMap}. This method allows omitting the type parameter when creating the TreeMap:
     * <code>Map&lt;String, Integer&gt; map = CollectionHelper.newTreeMap();</code>.
     * </p>
     * 
     * @return A new {@link TreeMap}.
     */
    public static <K, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<K, V>();
    }

    /**
     * <p>
     * Create a new {@link LinkedHashMap}. This method allows omitting the type parameter when creating the
     * LinkedHashMap: <code>Map&lt;String, Integer&gt; map = CollectionHelper.newLinkedHashMap();</code>.
     * </p>
     * 
     * @return A new {@link LinkedHashMap}.
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<K, V>();
    }

    /**
     * <p>
     * Create a new {@link ArrayList}. This method allows omitting the type parameter when creating the ArrayList:
     * <code>List&lt;String&gt; list = CollectionHelper.newArrayList();</code>.
     * </p>
     * 
     * @return A new {@link ArrayList}.
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<E>();
    }

    /**
     * <p>
     * Create a new {@link ArrayList} and fill it with the contents of the given {@link Iterable}.
     * </p>
     * 
     * @param iterable The {@link Iterable} providing the content for the {@link List}.
     * @return The {@link List} with items from the {@link Iterable}.
     */
    public static <E> List<E> newArrayList(Iterable<? extends E> iterable) {
        Validate.notNull(iterable, "iterable must not be null");
        return newArrayList(iterable.iterator());
    }

    /**
     * <p>
     * Create a new {@link ArrayList} and fill it with the content of the given {@link Iterator}.
     * </p>
     * 
     * @param iterator The {@link Iterator} providing the content for the {@link List}, not <code>null</code>.
     * @return The {@link List} with items from the {@link Iterator}.
     */
    public static <E> List<E> newArrayList(Iterator<? extends E> iterator) {
        Validate.notNull(iterator, "iterator must not be null");
        List<E> list = new ArrayList<E>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    /**
     * <p>
     * Create a new {@link LinkedList}. This method allows omitting the type parameter when creating the LinkedList:
     * <code>List&lt;String&gt; list = CollectionHelper.newLinkedList();</code>.
     * </p>
     * 
     * @return A new {@link LinkedList}.
     */
    public static <E> LinkedList<E> newLinkedList() {
        return new LinkedList<E>();
    }

    /**
     * <p>
     * Create a new {@link HashSet}. This method allows omitting the type parameter when creating the HashSet:
     * <code>Set&lt;String&gt; set = CollectionHelper.newHashSet();</code>.
     * </p>
     * 
     * @return A new {@link HashSet}.
     */
    public static <E> HashSet<E> newHashSet() {
        return new HashSet<E>();
    }

    /**
     * <p>
     * Create a new {@link TreeSet}. This method allows omitting the type parameter when creating the TreeSet:
     * <code>Set&lt;String&gt; set = CollectionHelper.newTreeSet();</code>.
     * </p>
     * 
     * @return A new {@link TreeSet}.
     */
    public static <E> TreeSet<E> newTreeSet() {
        return new TreeSet<E>();
    }

    /**
     * <p>
     * Create a new {@link LinkedHashSet}. This method allows omitting the type parameter when creating the
     * LinkedHashSet: <code>Set&lt;String&gt; set = CollectionHelper.newLinkedHashSet();</code>.
     * </p>
     * 
     * @return A new {@link LinkedHashSet}.
     */
    public static <E> LinkedHashSet<E> newLinkedHashSet() {
        return new LinkedHashSet<E>();
    }

    /**
     * <p>
     * Remove all <code>null</code> elements in the supplied {@link Iterable}.
     * </p>
     * 
     * @param collection The iterable from which to remove <code>null</code> elements.
     * @return <code>true</code> if any elements were removed, else <code>false</code>.
     */
    public static <T> boolean removeNulls(Iterable<T> iterable) {
        Validate.notNull(iterable, "iterable must not be null");
        return remove(iterable, Filter.NULL_FILTER);
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable} and remove non-matching items; after applying this method, the
     * Iterable only contains the items which matched the filter.
     * </p>
     * 
     * @param iterable The Iterable to filter, not <code>null</code>.
     * @param filter The Filter to apply, not <code>null</code>.
     * @return <code>true</code> if any items were removed, else <code>false</code>.
     */
    public static <T> boolean remove(Iterable<T> iterable, Filter<? super T> filter) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(filter, "filter must not be null");

        boolean modified = false;
        Iterator<T> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (!filter.accept(item)) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable} and return the filtered result as new {@link Collection}.
     * </p>
     * 
     * @param iterable The Iterable to filter, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     * @param output The output {@link Collection} in which to put the result. Usually an {@link ArrayList} or
     *            {@link HashSet}, not <code>null</code>.
     * @return The supplied output Collection with the items that passed the filter.
     */
    public static <T, C extends Collection<T>> C filter(Iterable<T> iterable, Filter<? super T> filter, C output) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(filter, "filter must not be null");
        Validate.notNull(output, "output must not be null");

        for (T item : iterable) {
            if (filter.accept(item)) {
                output.add(item);
            }
        }
        return output;
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable} and return the filtered result as new {@link List}.
     * </p>
     * 
     * @param list The Iterable to filter, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     * @return A List with the items that passed the filter.
     * @see #filter(Iterable, Filter, Collection)
     */
    public static <T> List<T> filterList(Iterable<T> iterable, Filter<? super T> filter) {
        return filter(iterable, filter, CollectionHelper.<T> newArrayList());
    }

    /**
     * <p>
     * Apply a {@link Filter} to an {@link Iterable} and return the filtered result as new {@link Set}.
     * </p>
     * 
     * @param list The Iterable to filter, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     * @return A Set with the items that passed the filter.
     * @see #filter(Iterable, Filter, Collection)
     */
    public static <T> Set<T> filterSet(Iterable<T> iterable, Filter<? super T> filter) {
        return filter(iterable, filter, CollectionHelper.<T> newHashSet());
    }

    /**
     * <p>
     * Apply a type filter to an {@link Iterable} and return the filtered result as new {@link Collection}. An example
     * scenario for this method might be a Collection of {@link Number}s, from which you only want to obtain
     * {@link Double} values.
     * </p>
     * 
     * @param iterable The Iterable to filter, not <code>null</code>.
     * @param type The type which should be filtered, not <code>null</code>.
     * @param output The output {@link Collection} in which to put the result. Usually an {@link ArrayList} or
     *            {@link HashSet}, not <code>null</code>.
     * @return The supplied output Collection with the items that passed the type filter.
     */
    public static <O, C extends Collection<O>> C filter(Iterable<?> iterable, Class<O> type, C output) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(type, "type must not be null");
        Validate.notNull(output, "output must not be null");

        for (Object item : iterable) {
            if (type.isInstance(item)) {
                output.add(type.cast(item));
            }
        }
        return output;
    }

    /**
     * <p>
     * Get the first element in an {@link Iterable}.
     * </p>
     * 
     * @param list The Iterable from which to get the element, not <code>null</code>.
     * @return The first element, or <code>null</code> if the iterable was empty.
     */
    public static <T> T getFirst(Iterable<T> iterable) {
        Validate.notNull(iterable, "iterable must not be null");
        Iterator<T> iterator = iterable.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * <p>
     * Get the first X elements in an {@link Iterable}.
     * </p>
     * 
     * @param list The Iterable from which to get the element, not <code>null</code>.
     * @param num The number of elements to retrieve. If the collection has less entries it will return only those.
     * @return The first X elements, or an empty list if the iterable was empty.
     */
    public static <T> List<T> getFirst(Iterable<T> iterable, int num) {
        List<T> result = CollectionHelper.newArrayList();
        for (T t : iterable) {
            result.add(t);
            if (result.size() == num) {
                break;
            }
        }
        return result;
    }

    /**
     * <p>
     * Get a sublist of elements of a {@link List}.
     * </p>
     * 
     * @param list The list from which to get the element, not <code>null</code>.
     * @param offset The number of elements to skip.
     * @param num The number of elements to retrieve. If the collection has less entries it will return only those.
     * @return The sublist.
     */
    public static <T> List<T> getSublist(List<T> list, int offset, int num) {
        Validate.notNull(list);
        int o = Math.min(list.size(), offset);
        int n = Math.min(num, list.size() - o);
        return list.subList(o, o + n);
    }

    /**
     * <p>
     * Get the last element in a {@link List}.
     * </p>
     * 
     * @param list The List from which to get the element, not <code>null</code>.
     * @return The last element, or <code>null</code> if the list was empty.
     */
    public static <T> T getLast(List<T> list) {
        Validate.notNull(list, "list must not be null");
        if (list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /**
     * <p>
     * SQL like group by functionality. The returned {@link MultiMap} contains the groups, a specified {@link Function}
     * supplies the values for grouping.
     * </p>
     * 
     * @param iterable The Iterable to group, not <code>null</code>.
     * @param function The Function which returns the value which is used for grouping, not <code>null</code>.
     * @return A MultiMap representing the groups.
     */
    public static <I, V> MultiMap<V, I> groupBy(Iterable<I> iterable, Function<? super I, V> function) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");

        MultiMap<V, I> result = DefaultMultiMap.createWithList();
        for (I item : iterable) {
            result.add(function.compute(item), item);
        }
        return result;
    }

    /**
     * <p>
     * Convert contents of {@link Iterable}s to a different type. For example if, you have a {@link List} of Numbers and
     * want to convert them to Strings, supply a {@link Function} which applies the <code>toString()</code> method to
     * the Numbers (a predefined Function for this specific use case is available as {@link Function#TO_STRING_FUNCTION}
     * ).
     * </p>
     * 
     * <pre>
     * // list with numbers
     * List&lt;Integer&gt; numbers = Arrays.asList(0, 1, 1, 2, 3, 5);
     * // convert them to strings using the specified Function
     * List&lt;String&gt; strings = convert(numbers, new Function&lt;Number, String&gt;() {
     *     &#064;Override
     *     public String compute(Number input) {
     *         return input.toString();
     *     }
     * }, new ArrayList&lt;String&gt;());
     * </pre>
     * 
     * @param iterable The Iterable supplying the data to be converted, not <code>null</code>.
     * @param function The Function which converts the values in the iterable, not <code>null</code>.
     * @param output The output {@link Collection} in which to put the result. Usually an {@link ArrayList} or
     *            {@link HashSet}, not <code>null</code>.
     * @return The supplied output Collection with the converted items.
     * @see #convertList(Iterable, Function)
     * @see #convertSet(Iterable, Function)
     */
    public static <I, O, C extends Collection<O>> C convert(Iterable<I> iterable, Function<? super I, O> function,
            C output) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");
        Validate.notNull(output, "output must not be null");

        for (I item : iterable) {
            output.add(function.compute(item));
        }
        return output;
    }

    /**
     * <p>
     * Convert contents of {@link Iterable}s to a different type and put them into a {@link Set}. For example if, you
     * have a {@link List} of Numbers and want to convert them to Strings, supply a {@link Function} which applies the
     * <code>toString()</code> method to the Numbers (a predefined Function for this specific use case is available as
     * {@link Function#TO_STRING_FUNCTION}).
     * </p>
     * 
     * @param iterable The Iterable supplying the data to be converted, not <code>null</code>.
     * @param function The Function which converts the values in the iterable, not <code>null</code>.
     * @return A {@link Set} with the field elements from the given objects.
     */
    public static <I, O> Set<O> convertSet(Iterable<I> iterable, Function<? super I, O> function) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");
        return convert(iterable, function, new HashSet<O>());
    }

    /**
     * <p>
     * Convert contents of {@link Iterable}s to a different type and put them into a {@link List}. For example if, you
     * have a {@link List} of Numbers and want to convert them to Strings, supply a {@link Function} which applies the
     * <code>toString()</code> method to the Numbers (a predefined Function for this specific use case is available as
     * {@link Function#TO_STRING_FUNCTION}).
     * </p>
     * 
     * @param iterable The Iterable supplying the data to be converted, not <code>null</code>.
     * @param function The Function which converts the values in the iterable, not <code>null</code>.
     * @return A {@link List} with the field elements from the given objects.
     */
    public static <I, O> List<O> convertList(Iterable<I> iterable, Function<? super I, O> function) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");
        return convert(iterable, function, new ArrayList<O>());
    }

    /**
     * <p>
     * Create a wrapper for a given {@link Iterator} which converts the iterator's items using a provided
     * {@link Function}.
     * </p>
     * 
     * @param iterator The iterator to wrap, not <code>null</code>.
     * @param function The {@link Function} which performs the conversion, not <code>null</code>.
     * @return An iterator wrapping the given iterator.
     */
    public static <I, O> Iterator<O> convert(final Iterator<I> iterator, final Function<? super I, O> function) {
        Validate.notNull(iterator, "iterator must not be null");
        Validate.notNull(function, "function must not be null");
        return new Iterator<O>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public O next() {
                return function.compute(iterator.next());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    /**
     * <p>
     * Create a wrapper for a given {@link Iterable} which converts the iterable's items using a provided
     * {@link Function}.
     * </p>
     * 
     * @param iterator The iterator to wrap, not <code>null</code>.
     * @param function The {@link Function} which performs the conversion, not <code>null</code>.
     * @return An iterable wrapping the given iterable.
     */
    public static <I, O> Iterable<O> convert(final Iterable<I> iterable, final Function<? super I, O> function) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.notNull(function, "function must not be null");
        return new Iterable<O>() {
            @Override
            public Iterator<O> iterator() {
                return convert(iterable.iterator(), function);
            }
        };
    }

    /**
     * <p>
     * Join elements of a collection in a readable form.
     * </p>
     * 
     * @param entries The entries that should be joined.
     * @return The joined string.
     */
    public static String joinReadable(Collection<?> entries) {
        return joinReadable(entries, entries.size());
    }

    public static String joinReadable(Collection<?> entries, int numEntries) {
        String joinedText = StringUtils.join(entries.toArray(), ", ", 0, Math.min(entries.size(), numEntries));
        int lastIndex = joinedText.lastIndexOf(",");
        if (lastIndex > -1) {
            String joinedTextNew = joinedText.substring(0, lastIndex);
            if (entries.size() > 2) {
                joinedTextNew += ",";
            }
            joinedTextNew += " and" + joinedText.substring(lastIndex + 1);
            joinedText = joinedTextNew;
        }
        return joinedText;
    }

    /**
     * <p>
     * Get a value from a {@link Map} by trying multiple keys.
     * </p>
     * 
     * @param map The map, not <code>null</code>.
     * @param keys The keys.
     * @return The value if any of the keys matches, or <code>null</code>.
     */
    public static <K, V> V getTrying(Map<K, V> map, K... keys) {
        Validate.notNull(map, "map must not be null");
        Validate.notNull(keys, "keys must not be null");
        for (K key : keys) {
            V value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * <p>
     * Get the first non-null value from the given items.
     * </p>
     * 
     * @param items The items.
     * @return The first non-null item from the given, or <code>null</code> in case the only <code>null</code> or no
     *         values were given.
     */
    public static <T> T coalesce(T... items) {
        for (T item : items) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    /**
     * <p>
     * Limit the specified {@link Iterable} to the specified size, i.e. effectively get the first specified elements,
     * then stop.
     * </p>
     * 
     * @param iterable The iterable, not <code>null</code>.
     * @param limit The number of elements which can be retrieved from the given iterable, greater/equal zero.
     * @return An iterable which limits to the specified number of items, in case it contains more.
     */
    public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limit) {
        Validate.notNull(iterable, "iterable must not be null");
        Validate.isTrue(limit >= 0, "limit must be greater/equal zero");
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return limit(iterable.iterator(), limit);
            }
        };
    }

    /**
     * <p>
     * Limit the specified {@link Iterator} to the specified size, i.e. effectively get the first specified elements,
     * then stop.
     * </p>
     * 
     * @param iterator The iterator, not <code>null</code>.
     * @param limit The number of elements which can be retrieved from the given iterator, greater/equal zero.
     * @return An iterator which limits to the specified number of items, in case it contains more.
     */
    public static <T> Iterator<T> limit(Iterator<T> iterator, int limit) {
        Validate.notNull(iterator, "iterator must not be null");
        Validate.isTrue(limit >= 0, "limit must be greater/equal zero");
        return new LimitIterator<T>(iterator, limit);
    }

    /**
     * <p>
     * Get a set with distinct values from all given collections.
     * </p>
     * 
     * @param collections The collections, not <code>null</code>.
     * @return A {@link Set} with distinct values from the given collections.
     */
    public static <T> Set<T> distinct(Collection<T>... collections) {
        Validate.notNull(collections, "collections must not be null");
        Set<T> distinct = newHashSet();
        for (Collection<T> collection : collections) {
            distinct.addAll(collection);
        }
        return distinct;
    }

    /**
     * <p>
     * Create a list where the given item is inserted n times.
     * </p>
     * 
     * @param n The number of times the item is added, greater/equal zero.
     * @param item The item to add.
     * @return A list of the size n.
     */
    public static <T> List<T> repeat(int n, T item) {
        Validate.isTrue(n >= 0, "n must be greater/equal zero");
        List<T> result = CollectionHelper.newArrayList();
        for (int i = 0; i < n; i++) {
            result.add(item);
        }
        return result;
    }

    /**
     * <p>
     * Shuffle the content of the given array.
     * </p>
     * 
     * @param array The array to shuffle, not <code>null</code>.
     */
    public static void shuffle(Object[] array) {
        Validate.notNull(array, "array must not be null");
        // http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
        Random rnd = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            Object item = array[index];
            array[index] = array[i];
            array[i] = item;
        }
    }

}
