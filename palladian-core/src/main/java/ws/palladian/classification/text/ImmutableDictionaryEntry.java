package ws.palladian.classification.text;

import org.apache.commons.lang3.Validate;
import ws.palladian.classification.text.DictionaryModel.DictionaryEntry;
import ws.palladian.core.CategoryEntries;

public final class ImmutableDictionaryEntry implements DictionaryEntry {

    private final String term;
    private final CategoryEntries categoryEntries;

    public ImmutableDictionaryEntry(String term) {
        this(term, CategoryEntries.EMPTY);
    }

    public ImmutableDictionaryEntry(String term, CategoryEntries categoryEntries) {
        Validate.notNull(term, "term must not be null");
        Validate.notNull(categoryEntries, "categoryEntries must not be null");
        this.term = term;
        this.categoryEntries = categoryEntries;
    }

    @Override
    public CategoryEntries getCategoryEntries() {
        return categoryEntries;
    }

    @Override
    public String getTerm() {
        return term;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + categoryEntries.hashCode();
        result = prime * result + term.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableDictionaryEntry other = (ImmutableDictionaryEntry) obj;
        if (!term.equals(other.term)) {
            return false;
        }
        return categoryEntries.equals(other.categoryEntries);
    }

    @Override
    public String toString() {
        return term + ":" + categoryEntries;
    }

}
