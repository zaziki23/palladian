package ws.palladian.retrieval.parser;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * <p>
 * {@link FilterReader} to skip invalid xml version 1.0 characters. Valid Unicode chars for xml version 1.0 according to
 * http://www.w3.org/TR/xml are #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD], [#x10000-#x10FFFF] . In other words -
 * any Unicode character, excluding the surrogate blocks, FFFE, and FFFF.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://info.tsachev.org/2009/05/skipping-invalid-xml-character-with.html">Skipping Invalid XML
 * Character with ReaderFilter</a>
 */
class Xml10FilterReader extends FilterReader {

    private boolean ignoreCharacter = true;

    /**
     * Creates filter reader which skips invalid xml characters.
     *
     * @param in original reader
     */
    public Xml10FilterReader(Reader in) {
        super(in);
    }

    /**
     * Every overload of {@link Reader#read()} method delegates to this one so it is enough to override only this one. <br />
     * To skip invalid characters this method shifts only valid chars to left and returns decreased value of the
     * original read method. So after last valid character there will be some unused chars in the buffer.
     *
     * @return Number of read valid characters or <code>-1</code> if end of the underling reader was reached.
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = super.read(cbuf, off, len);
        // If read chars are -1 then we have reach the end of the reader.
        if (read == -1) {
            return -1;
        }
        // pos will show the index where chars should be moved if there are gaps from invalid characters.
        int pos = off - 1;

        for (int readPos = off; readPos < off + read; readPos++) {

            if (!Character.isWhitespace(cbuf[readPos]) && ignoreCharacter) {
                ignoreCharacter = false;
            }

            if (isValidXmlChar(cbuf[readPos]) && !ignoreCharacter) {
                pos++;
            } else {
                continue;
            }
            // If there is gap(s) move current char to its position.
            if (pos < readPos) {
                cbuf[pos] = cbuf[readPos];
            }
        }
        // Number of read valid characters.
        return pos - off + 1;
    }

    /**
     * <p>
     * Checks, whether the supplied character is a valid XML character.
     * </p>
     *
     * @param c The char to check.
     * @return <code>true</code> if char is allowed XML character, <code>false</code> otherwise.
     */
    // TODO move this method to HtmlHelper.
    private static boolean isValidXmlChar(char c) {
        return c == 0x9 || c == 0xA || c == 0xD || c >= 0x20 && c <= 0xD7FF || c >= 0xE000 && c <= 0xFFFD || c >= 0x10000 && c <= 0x10FFFF;
    }

}