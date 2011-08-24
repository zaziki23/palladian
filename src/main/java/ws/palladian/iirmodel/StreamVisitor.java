package ws.palladian.iirmodel;

/**
 * <p>
 * Visitor interface which allows to traverse {@link StreamSource}s. Use {@link StreamSource#accept(StreamVisitor)} with
 * your visitor implementation.
 * </p>
 * 
 * @author Philipp Katz
 * @version 3.1
 * @since 3.1
 */
public interface StreamVisitor {

    void visitItemStream(ItemStream itemStream, int depth);

    void visitStreamGroup(StreamGroup streamGroup, int depth);

    void visitItem(Item item, int depth);

}
