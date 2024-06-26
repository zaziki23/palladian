package ws.palladian.helper.nlp;

import org.junit.Test;

import java.util.EmptyStackException;

import static org.junit.Assert.assertEquals;

public class CharStackTest {

    @Test
    public void testCharStack() {
        CharStack charStack = new CharStack();
        charStack.push('a');
        charStack.push('b');
        charStack.push('c');
        charStack.push('d');
        assertEquals("abcd", charStack.toString());
        assertEquals(4, charStack.length());
        assertEquals('d', charStack.peek());
        assertEquals("abcd", charStack.toString());
        assertEquals('d', charStack.pop());
        assertEquals("abc", charStack.toString());
    }

    @Test(expected = EmptyStackException.class)
    public void testEmptyCharStack() {
        CharStack charStack = new CharStack();
        charStack.peek();
    }

}
