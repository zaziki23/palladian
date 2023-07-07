package ws.palladian.helper.collection;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.apache.commons.lang3.Validate;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;

import java.io.Serializable;
import java.util.*;

/**
 * <p>
 * A trie data structure. This can make string-based retrieval faster and more space efficient than using e.g. a
 * HashMap. This implementations does <i>not</i> allow <code>null</code> or empty values as keys.
 * See <a href="http://en.wikipedia.org/wiki/Trie">Wikipedia: Trie</a>
 *
 * This is different from the Trie implementation as you don't have to store a set of ids on each node but by calling one node, all the children are visited and ints are collected.
 * This makes it 1000x slower than the tree but it requires much less memory.
 *
 * @author Philipp Katz
 * @author David Urbansky
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ConcurrentIdTrie implements Map.Entry<String, IntOpenHashSet>, Iterable<Map.Entry<String, IntOpenHashSet>>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final char EMPTY_CHARACTER = '\u0000';

    private static final ConcurrentIdTrie[] EMPTY_ARRAY = new ConcurrentIdTrie[0];

    protected final char character;

    protected final ConcurrentIdTrie parent;

    protected ConcurrentIdTrie[] children = EMPTY_ARRAY;

    protected IntOpenHashSet value;

    /** Store the most expensive (cost = time to retrieve) ngrams in a cache. */
    private CostAwareCache<String, IntOpenHashSet> costAwareCache;

    public static final String DELIMITERS = " ,;:!?.[]()|/<>&\"'-–—―`‘’“·•®”*_+";

    public ConcurrentIdTrie(int cacheSize) {
        this(EMPTY_CHARACTER, null);
        costAwareCache = new CostAwareCache(cacheSize);
    }

    public ConcurrentIdTrie() {
        this(EMPTY_CHARACTER, null);
    }

    private ConcurrentIdTrie(char character, ConcurrentIdTrie parent) {
        this.character = character;
        this.parent = parent;
        costAwareCache = null;
    }

    public ConcurrentIdTrie getNode(CharSequence key) {
        Validate.notEmpty(key, "key must not be empty");
        return getNode(key, false);
    }

    protected ConcurrentIdTrie getNode(CharSequence key, boolean create) {
        if (key == null || key.length() == 0) {
            return this;
        }
        char head = key.charAt(0);
        CharSequence tail = tail(key);
        for (ConcurrentIdTrie node : children) {
            if (head == node.character) {
                return node.getNode(tail, create);
            }
        }
        if (create) {
            ConcurrentIdTrie newNode = new ConcurrentIdTrie(head, this);
            if (children == EMPTY_ARRAY) {
                children = new ConcurrentIdTrie[]{newNode};
            } else {
                ConcurrentIdTrie[] newArray = new ConcurrentIdTrie[children.length + 1];
                System.arraycopy(children, 0, newArray, 0, children.length);
                newArray[children.length] = newNode;
                children = newArray;
            }
            return newNode.getNode(tail, true);
        } else {
            return null;
        }
    }

    /**
     * Add a text. First we ngramize the text and make sure we add the id only to the leaf nodes.
     * For example, in the text: "The punk made a pun", we'll add the id to "punk" but not "pun", "pu" and "p" as we would get them by child relation when asking for "p".
     *
     * @param text The text to ngramize and add.
     * @param id   The id to add to the leaf nodes.
     */
    public void add(int id, String text) {
        StringTokenizer stringTokenizer = new StringTokenizer(text, DELIMITERS);
        List<String> tokens = new ArrayList<>();
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            tokens.add(token);
        }
        tokens = new ArrayList<>(new HashSet<>(tokens));
        for (String token : tokens) {
            IntOpenHashSet integers = getValue(token);
            if (integers == null) {
                integers = new IntOpenHashSet(4, 0.8f);
                synchronized (this) {
                    put(token, integers);
                }
            }
            synchronized (this) {
                integers.add(id);
            }
        }
    }

    public void add(int id, Set<String> ngrams) {
        for (String ngram : ngrams) {
            IntOpenHashSet integers = getValue(ngram);
            if (integers == null) {
                integers = new IntOpenHashSet(4, 0.8f);
                synchronized (this) {
                    put(ngram, integers);
                }
            }
            synchronized (this) {
                integers.add(id);
            }
        }
    }

    public IntOpenHashSet put(String key, IntOpenHashSet value) {
        Validate.notEmpty(key, "key must not be empty");
        ConcurrentIdTrie node = getNode(key, true);
        IntOpenHashSet oldValue = node.value;
        node.value = value;
        return oldValue;
    }

    public IntOpenHashSet getValue(String key) {
        Validate.notEmpty(key, "key must not be empty");
        ConcurrentIdTrie node = getNode(key);
        return node != null ? node.value : null;
    }

    public IntOpenHashSet get(String key) {
        Validate.notEmpty(key, "key must not be empty");
        long startTime = 0;
        if (costAwareCache != null) {
            IntOpenHashSet integers = costAwareCache.tryGet(key);
            if (integers != null) {
                return integers;
            }
            startTime = System.nanoTime();
        }
        ConcurrentIdTrie node = getNode(key);
        if (node == null) {
            return new IntOpenHashSet();
        }
        Iterator<Map.Entry<String, IntOpenHashSet>> iterator = node.iterator();

        // XXX possibility to add cache here, the longer the path to the leaf nodes the longer it will take to collect. so if we call with a single character like "s", we might want
        // to cache the result to not iterate over the tree over and over again.

        IntArrayList list;
        if (node.hasData()) {
            list = new IntArrayList(node.getValue());
        } else {
            list = new IntArrayList();
        }
        while (iterator.hasNext()) {
            Map.Entry<String, IntOpenHashSet> entry = iterator.next();
            list.addAll(entry.getValue());
        }
        IntOpenHashSet integers = new IntOpenHashSet(list);

        if (costAwareCache != null) {
            costAwareCache.tryAdd((int) (System.nanoTime() - startTime), key, integers);
        }

        return integers;
    }

    public IntOpenHashSet getOrPut(String key, IntOpenHashSet value) {
        Validate.notEmpty(key, "key must not be empty");
        return getOrPut(key, Factories.constant(value));
    }

    public IntOpenHashSet getOrPut(String key, Factory<IntOpenHashSet> valueFactory) {
        Validate.notEmpty(key, "key must not be empty");
        Validate.notNull(valueFactory, "valueFactory must not be null");
        ConcurrentIdTrie node = getNode(key, true);
        if (node.value == null) {
            node.value = valueFactory.create();
        }
        return node.value;
    }

    @Override
    public IntOpenHashSet getValue() {
        return value;
    }

    @Override
    public IntOpenHashSet setValue(IntOpenHashSet value) {
        IntOpenHashSet oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    private CharSequence tail(CharSequence seq) {
        return seq.length() > 1 ? seq.subSequence(1, seq.length()) : null;
    }

    private Iterator<ConcurrentIdTrie> children() {
        return new ArrayIterator<>(children);
    }

    private boolean hasData() {
        return value != null;
    }

    @Override
    public String getKey() {
        StringBuilder builder = new StringBuilder().append(character);
        for (ConcurrentIdTrie current = parent; current != null; current = current.parent) {
            if (current.character != EMPTY_CHARACTER) {
                builder.append(current.character);
            }
        }
        return builder.reverse().toString();
    }

    /**
     * Remove all empty nodes which have no children (saves memory, in case terms have been removed from the trie).
     *
     * @return <code>true</code> in case this node is empty and has no children.
     */
    public boolean clean() {
        boolean clean = true;
        List<ConcurrentIdTrie> temp = new ArrayList<>();
        for (ConcurrentIdTrie child : children) {
            boolean childClean = child.clean();
            if (!childClean) {
                temp.add(child);
            }
            clean &= childClean;
        }
        int childCount = temp.size();
        children = childCount > 0 ? temp.toArray(new ConcurrentIdTrie[childCount]) : EMPTY_ARRAY;
        clean &= !hasData();
        return clean;
    }

    @Override
    public Iterator<Map.Entry<String, IntOpenHashSet>> iterator() {
        return new TrieEntryIterator(this);
    }

    public int size() {
        return CollectionHelper.count(this.iterator());
    }

    public CostAwareCache<String, IntOpenHashSet> getCostAwareCache() {
        return costAwareCache;
    }

    public void setCostAwareCache(CostAwareCache<String, IntOpenHashSet> costAwareCache) {
        this.costAwareCache = costAwareCache;
    }

    @Override
    public String toString() {
        return getKey() + '=' + getValue();
    }

    // iterator over all entries
    private static final class TrieEntryIterator extends AbstractIterator<Map.Entry<String, IntOpenHashSet>> {
        private final Deque<Iterator<ConcurrentIdTrie>> stack;
        private ConcurrentIdTrie currentNode;

        private TrieEntryIterator(ConcurrentIdTrie root) {
            stack = new ArrayDeque<>();
            stack.push(root.children());
        }

        @Override
        protected Map.Entry<String, IntOpenHashSet> getNext() throws Finished {
            for (; ; ) {
                if (stack.isEmpty()) {
                    throw FINISHED;
                }
                Iterator<ConcurrentIdTrie> current = stack.peek();
                if (!current.hasNext()) {
                    throw FINISHED;
                }
                ConcurrentIdTrie node = current.next();
                if (!current.hasNext()) {
                    stack.pop();
                }
                Iterator<ConcurrentIdTrie> children = node.children();
                if (children.hasNext()) {
                    stack.push(children);
                }
                if (node.hasData()) {
                    currentNode = node;
                    return node;
                }
            }
        }

        @Override
        public void remove() {
            if (currentNode == null) {
                throw new NoSuchElementException();
            }
            currentNode.value = null;
        }
    }
}
