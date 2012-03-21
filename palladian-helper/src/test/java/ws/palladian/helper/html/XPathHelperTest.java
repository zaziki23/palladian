package ws.palladian.helper.html;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

public class XPathHelperTest {

    private DocumentParser htmlParser = ParserFactory.createHtmlParser();
    private DocumentParser xmlParser = ParserFactory.createXmlParser();

    @Test
    public void testAddNamespaceToXPath() {

        // test add XMLNS
        assertEquals(XPathHelper.addXhtmlNsToXPath("//TABLE/TR/TD/A[4]"), "//xhtml:TABLE/xhtml:TR/xhtml:TD/xhtml:A[4]");
        assertEquals(XPathHelper.addXhtmlNsToXPath("/TABLE/TR/TD/A[4]"), "/xhtml:TABLE/xhtml:TR/xhtml:TD/xhtml:A[4]");
        assertEquals(XPathHelper.addXhtmlNsToXPath("/TABLE/TR[2]/TD/A"), "/xhtml:TABLE/xhtml:TR[2]/xhtml:TD/xhtml:A");
        assertEquals(XPathHelper.addXhtmlNsToXPath("/TABLE/TR[2]/TD/A/text()"),
                "/xhtml:TABLE/xhtml:TR[2]/xhtml:TD/xhtml:A/text()");

        // TODO assertEquals(XPathHelper.addXhtmlNsToXPath("//a[img]"), "//xhtml:a[xhtml:img]");

    }

    @Test
    public void testGetXhtmlChildNodes() throws FileNotFoundException, ParserException {

        Document doc = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/NekoTableTestcase1.html"));

        List<Node> rows = XPathHelper.getXhtmlNodes(doc, "//table/tr");
        assertEquals(3, rows.size());

        for (Node row : rows) {

            // iterate over TDs
            List<Node> cells = XPathHelper.getXhtmlChildNodes(row, "//td"); // does not work EDIT: now it does
            assertEquals(3, cells.size());

            cells = XPathHelper.getXhtmlChildNodes(row, "*"); // infinite loop? EDIT: yes, stupid me :) solved.
            assertEquals(3, cells.size());
        }
    }

    @Test
    public void testGetElementById() throws FileNotFoundException, ParserException {
        Document doc = xmlParser.parse(ResourceHelper.getResourceFile("/xmlDocuments/events.xml"));

        assertEquals("event", XPathHelper.getNodeByID(doc, "e01").getNodeName());
        assertEquals("events", XPathHelper.getParentNodeByID(doc, "e01").getNodeName());
    }

    @Test
    public void testNamespace() throws FileNotFoundException, ParserException {
        Document doc = xmlParser.parse(ResourceHelper.getResourceFile("/feeds/atomSample1.xml"));

        Map<String, String> mapping = new HashMap<String, String>();
        mapping.put("atom", "http://www.w3.org/2005/Atom");
        List<Node> nodes = XPathHelper.getNodes(doc, "//atom:author", mapping);
        assertEquals(2, nodes.size());
    }

}