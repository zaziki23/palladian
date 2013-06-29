package ws.palladian.helper.html;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Some HTML and XML/DOM specific helper methods.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class HtmlHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlHelper.class);

    /** HTML block level elements. */
    private static final List<String> BLOCK_ELEMENTS = Arrays.asList("address", "blockquote", "div", "dl", "fieldset",
            "form", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "noscript", "ol", "p", "pre", "table", "ul", "dd", "dt",
            "li", "tbody", "td", "tfoot", "th", "thead", "tr", "button", "del", "ins", "map", "object", "script", "br");

    /** "Junk" elements which do not contain relevant content. */
    private static final List<String> IGNORE_INSIDE = Arrays.asList("script", "style");

    private static final Pattern HTML_TO_READABLE_TEXT = Pattern.compile("\\<br\\s?\\/?\\>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TO_READABLE_TEXT2 = Pattern.compile("\\<\\/p\\>", Pattern.CASE_INSENSITIVE);
    private static final Pattern NORMALIZE_LINES = Pattern.compile("^\\s+$|^[ \t]+|[ \t]+$", Pattern.MULTILINE);
    private static final Pattern STRIP_ALL_TAGS = Pattern
            .compile("<!--.*?-->|<script.*?>.*?</script>|<style.*?>.*?</style>|<.*?>", Pattern.DOTALL
                    | Pattern.CASE_INSENSITIVE);

    /** Thread local caching of TransformerFactories which are not thread-safe, but expensive to create. */
    private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORIES = new ThreadLocal<TransformerFactory>() {
        @Override
        protected TransformerFactory initialValue() {
            return TransformerFactory.newInstance();
        };
    };

    /** prevent instantiation. */
    private HtmlHelper() {
    }

    /**
     * <p>
     * Count the tags.
     * </p>
     * 
     * @param htmlText
     *            The html text.
     * @return The number of tags.
     */
    public static int countTags(String htmlText) {
        return countTags(htmlText, false);
    }

    /**
     * <p>
     * Count the number of characters used for tags in the given string. For example, &lt;PHONE&gt;iphone
     * 4&lt;/PHONE&gt; => 15
     * </p>
     * 
     * @param taggedText
     *            The text with tags.
     * @return The cumulated number of characters used for tags in the given
     *         text.
     */
    public static int countTagLength(String taggedText) {
        int totalTagLength = 0;

        Pattern pattern = Pattern.compile("<(.*?)>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(taggedText);
        while (matcher.find()) {
            totalTagLength += matcher.group(1).length() + 2;
        }

        return totalTagLength;
    }

    /**
     * <p>
     * Count tags.
     * </p>
     * 
     * @param htmlText
     *            The html text.
     * @param distinct
     *            If true, count multiple occurrences of the same tag only once.
     * @return The number of tags.
     */
    public static int countTags(String htmlText, boolean distinct) {
        Set<String> tags = new HashSet<String>();

        int tagCount = 0;

        Pattern pattern = Pattern.compile("(\\<.*?>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlText);

        while (matcher.find()) {
            tagCount++;
            tags.add(matcher.group());
        }

        if (distinct) {
            tagCount = tags.size();
        }

        return tagCount;
    }

    /**
     * <p>
     * Remove all style and script tags including their content (CSS, JavaScript). Remove all other tags as well. Close
     * gaps. The text might not be readable since all format hints are discarded. Consider using
     * {@link HtmlHelper.htmlToReableText} in case you need formatting.
     * </p>
     * 
     * @param htmlText The html content for which tags should be removed.
     */
    public static String stripHtmlTags(String htmlText) {
        return STRIP_ALL_TAGS.matcher(htmlText).replaceAll("");
    }

    public static String stripHtmlTags(String htmlText, EnumSet<HtmlElement> htmlElements) {
        if (htmlText == null) {
            return htmlText;
        }

        StringBuilder regExp = new StringBuilder();

        if (htmlElements.contains(HtmlElement.COMMENTS)) {
            regExp.append("<!--.*?-->|");
        }

        if (htmlElements.contains(HtmlElement.SCRIPT)) {
            regExp.append("<script.*?>.*?</script>|");
        }

        if (htmlElements.contains(HtmlElement.CSS)) {
            regExp.append("<style.*?>.*?</style>|");
        }

        if (htmlElements.contains(HtmlElement.TAG)) {
            regExp.append("<.*?>");
        }

        String r = regExp.toString();
        if (r.isEmpty()) {
            return htmlText;
        }

        if (r.endsWith("|")) {
            r = r.substring(0, regExp.length() - 1);
        }

        Pattern p = Pattern.compile(r, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        return p.matcher(htmlText).replaceAll("");
    }

    public static String joinTagsAndRemoveNewLines(String htmlText) {
        if (htmlText == null) {
            return htmlText;
        }

        htmlText = htmlText.replaceAll(">\\s*?<", "><");
        htmlText = htmlText.replaceAll("\n", "");

        return htmlText;
    }

    /**
     * <p>
     * Removes the concrete HTML tag.
     * </p>
     * 
     * @param pageContent
     *            The html text.
     * @param tag
     *            The tag that should be removed.
     * @return The html text without the tag.
     */
    public static String removeConcreteHtmlTag(String pageString, String tag) {
        return removeConcreteHtmlTag(pageString, tag, tag);
    }

    /**
     * <p>
     * Remove concrete HTML tags from a string; this version is for special-tags like <!-- -->.
     * </p>
     * 
     * @param pageContent
     *            The html text.
     * @param beginTag
     *            The begin tag.
     * @param endTag
     *            The end tag.
     * @return The string without the specified html tag.
     */
    public static String removeConcreteHtmlTag(String pageContent, String beginTag, String endTag) {
        String pageString = pageContent;
        List<String> removeList;
        removeList = getConcreteTags(pageString, beginTag, endTag);
        for (String removeTag : removeList) {
            pageString = pageString.replace(removeTag, "");
        }
        return pageString;
    }

    /**
     * <p>
     * Get a list of concrete HTML tags; begin- and endtag are not different.
     * </p>
     * 
     * @param pageContent
     *            The html text.
     * @param tag
     *            The tag.
     * @return A list of concrete tags.
     */
    public static List<String> getConcreteTags(String pageString, String tag) {
        return getConcreteTags(pageString, tag, tag);
    }

    /**
     * <p>
     * Get a list of concrete HTML tags; its possible that begin- and endtag are different like <!-- -->.
     * </p>
     * 
     * @param pageString
     *            The html text.
     * @param beginTag
     *            The begin tag.
     * @param endTag
     *            The end tag.
     * @return A list of concrete tag names.
     */
    public static List<String> getConcreteTags(String pageString, String beginTag, String endTag) {

        List<String> tagList = new ArrayList<String>();
        String regExp = "";
        if (beginTag.equals(endTag)) {
            // regExp = "<"+beginTag+".*?>.*?</"+endTag+">";
            regExp = "<" + beginTag + ".*?>(.*?</" + endTag + ">)?";

        } else {
            regExp = beginTag + ".*?" + endTag;
        }

        Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(pageString);
        while (matcher.find()) {
            tagList.add(matcher.group(0));
        }

        // LOGGER.info("get concrete tags took " + sw.getElapsedTimeString() +
        // " for a string of length "+
        // pageString.length());

        return tagList;
    }

    /**
     * <p>
     * Converts HTML markup to a more or less human readable string. For example we insert line breaks for HTML block
     * level elements, filter out comments, scripts and stylesheets, remove unnecessary white space and so on.
     * </p>
     * 
     * <p>
     * In contrast to {@link #stripHtmlTags(String, boolean, boolean, boolean, boolean)}, which works on Strings and
     * just strips out all tags via RegExes, this approach tries to keep some structure for displaying HTML content in
     * text mode in a readable form.
     * </p>
     * 
     * FIXME: "namespace not declared" errors pop up too often
     * 
     * @param node
     * @return
     * @author Philipp Katz
     */
    public static String documentToReadableText(Node node) {
        final StringBuilder builder = new StringBuilder();
        try {
            TransformerFactory factory = TRANSFORMER_FACTORIES.get();
            Transformer trans = factory.newTransformer();
            trans.transform(new DOMSource(node), new SAXResult(new DefaultHandler() {
                boolean ignoreCharacters = false;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    String tag = localName.toLowerCase();
                    if (IGNORE_INSIDE.contains(tag)) {
                        ignoreCharacters = true;
                        return;
                    }
                    if (BLOCK_ELEMENTS.contains(tag) || localName.equalsIgnoreCase("br")) {
                        builder.append("\n");
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    String tag = localName.toLowerCase();
                    if (IGNORE_INSIDE.contains(tag)) {
                        ignoreCharacters = false;
                        return;
                    }
                    if (BLOCK_ELEMENTS.contains(tag)) {
                        builder.append("\n");
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (ignoreCharacters) {
                        return;
                    }
                    builder.append(ch, start, length);
                }
            }));
        } catch (TransformerConfigurationException e) {
            LOGGER.error("htmlDocToString:TransformerConfigurationException", e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error("htmlDocToString:TransformerFactoryConfigurationError", e);
        } catch (TransformerException e) {
            LOGGER.error("htmlDocToString:TransformerException", e);
        }
        String result = builder.toString();
        // result = result.replaceAll("[ \t]*?\n", "\n");
        result = NORMALIZE_LINES.matcher(result).replaceAll("");
        result = result.replaceAll("\n{3,}", "\n\n");
        result = result.replaceAll(" {2,}", " ");

        // experimental added 2010-06-30
        // remove multi line comments
        // result = result.replaceAll("<!--(.|\n)*?-->", "");

        result = result.trim();
        return result;
    }

    public static String htmlToReadableText(String htmlString) {
        htmlString = HTML_TO_READABLE_TEXT.matcher(htmlString).replaceAll("\n");
        htmlString = HTML_TO_READABLE_TEXT2.matcher(htmlString).replaceAll("\n");
        htmlString = stripHtmlTags(htmlString);
        return htmlString;
    }

    /**
     * <p>
     * Extract values e.g for: src=, href= or title=
     * </p>
     * 
     * @param pattern
     *            the pattern
     * @param content
     *            the content
     * @param removeTerm
     *            the term which should be removed e.g. " or '
     * @return the string
     */
    public static String extractTagElement(final String pattern, final String content, final String removeTerm) {
        String element = "";
        final Pattern elementPattern = Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher elementMatcher = elementPattern.matcher(content);
        while (elementMatcher.find()) {
            String result = elementMatcher.group(0);
            if (!"".equals(removeTerm)) {
                // remove the remove term
                result = result.replaceFirst(removeTerm, "");
                result = result.replaceFirst(removeTerm.toUpperCase(Locale.ENGLISH), "");
                result = result.replaceFirst(removeTerm.toLowerCase(Locale.ENGLISH), "");
            }
            // remove the quotation-marks
            result = result.replaceAll("\"", "");
            result = result.replaceAll("'", "");

            element = result;
        }
        return element;
    }

    /**
     * <p>
     * Checks, if a node is simple like &ltu&gt,&ltb&gt,&lti&gt,...
     * </p>
     * 
     * @param node
     * @return true if simple, else false.
     */
    public static boolean isSimpleElement(Node node) {
        List<String> simpleElements = Arrays.asList("b", "i", "em", "ins", "del", "s", "small", "big", "strong", "u");
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String name = node.getNodeName().toLowerCase();
            return simpleElements.contains(name);
        }
        return false;
    }

    /**
     * <p>
     * Checks, if tag is a headline.
     * </p>
     * 
     * @param tag
     * @return
     */
    public static boolean isHeadlineTag(String tag) {
        return Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6").contains(tag.toLowerCase());
    }

    public static boolean isHeadlineTag(Node tag) {
        return isHeadlineTag(tag.getNodeName());
    }

    /**
     * <p>
     * Remove unnecessary whitespace from DOM nodes.
     * </p>
     * 
     * @see http
     *      ://stackoverflow.com/questions/978810/how-to-strip-whitespace-only
     *      -text-nodes-from-a-dom-before- serialization
     * 
     * @param node
     * @return
     */
    public static Node removeWhitespace(Node node) {

        Node result = node.cloneNode(true);

        try {
            XPathFactory xpathFactory = XPathFactory.newInstance();
            // XPath to find empty text nodes.
            XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
            NodeList emptyTextNodes = (NodeList)xpathExp.evaluate(result, XPathConstants.NODESET);

            // Remove each empty text node from document.
            for (int i = 0; i < emptyTextNodes.getLength(); i++) {
                Node emptyTextNode = emptyTextNodes.item(i);
                emptyTextNode.getParentNode().removeChild(emptyTextNode);
            }
        } catch (XPathExpressionException e) {
            LOGGER.error("Exception while removing whitespace", e);
        } catch (DOMException e) {
            LOGGER.error("Exception while removing whitespace", e);
        }

        return result;
    }

    public static boolean writeToFile(Node node, File file) {
        boolean success = false;
        Source source = new DOMSource(node);
        Result result = new StreamResult(file);
        try {
            TransformerFactory factory = TRANSFORMER_FACTORIES.get();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            success = true;
        } catch (TransformerConfigurationException e) {
            LOGGER.error("Exception while writing to file", e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error("Exception while writing to file", e);
        } catch (TransformerException e) {
            LOGGER.error("Exception while writing to file", e);
        }
        return success;
    }

    /**
     * <p>
     * Returns a String representation of the supplied Node, excluding the Node itself, like innerHTML in
     * JavaScript/DOM.
     * </p>
     * 
     * FIXME David: the JavaDoc is not correct, the root node of the page is not excluded, rename method?
     * 
     * @param node
     * @return
     */
    public static String getInnerXml(Node node) {
        StringBuilder sb = new StringBuilder();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            String outerXml = xmlToString(children.item(i), true);
            if (outerXml != null) {
                sb.append(outerXml);
            }
        }
        return sb.toString();
    }

    /**
     * <p>
     * Creates a new, empty DOM Document.
     * </p>
     * 
     * @return
     */
    public static Document createDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.newDocument();
        } catch (ParserConfigurationException e) {
            LOGGER.error("createDocument:ParserConfigurationException, throwing RuntimeException", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Removes all nodes with specified type from Node.
     * </p>
     * 
     * @param node
     * @param nodeType
     *            for example <code>Node.COMMENT_NODE</code>
     */
    public static void removeAll(Node node, short nodeType) {
        HtmlHelper.removeAll(node, nodeType, null);
    }

    /**
     * <p>
     * Removes all nodes with specified type and name from Node.
     * </p>
     * 
     * @param node
     * @param nodeType
     *            for example <code>Node.COMMENT_NODE</code>
     * @param name
     */
    public static void removeAll(Node node, short nodeType, String name) {
        if (node.getNodeType() == nodeType && (name == null || node.getNodeName().equals(name))) {
            node.getParentNode().removeChild(node);
        } else {
            NodeList list = node.getChildNodes();
            for (int i = list.getLength() - 1; i >= 0; i--) {
                removeAll(list.item(i), nodeType, name);
            }
        }
    }

    /**
     * <p>
     * Creates a copy of a DOM Document.
     * </p>
     * 
     * @see <a
     *      href="http://stackoverflow.com/questions/279154/how-can-i-clone-an-entire-document-using-the-java-dom">How
     *      can I clone an entire Document using the Java DOM?</a>
     * @param document
     * @return the cloned Document or <code>null</code> if cloning failed.
     */
    public static Document cloneDocument(Document document) {
        Document result = null;
        try {
            Transformer transformer = TRANSFORMER_FACTORIES.get().newTransformer();
            DOMSource source = new DOMSource(document);
            DOMResult target = new DOMResult();
            transformer.transform(source, target);
            result = (Document)target.getNode();
        } catch (TransformerConfigurationException e) {
            LOGGER.error("cloneDocument:TransformerConfigurationException " + e.getMessage());
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error("cloneDocument:TransformerFactoryConfigurationError " + e.getMessage());
        } catch (TransformerException e) {
            LOGGER.error("cloneDocument:TransformerException " + e.getMessage());
            // this happens when document is ill-formed, we could also throw
            // this exception. This way we have to check if this method returns
            // null;
        } catch (DOMException e) {
            LOGGER.error("cloneDocument:DOMException " + e.getMessage());
        }
        return result;
    }

    /**
     * <p>
     * Get a string representation of the supplied DOM {@link Node}.
     * </p>
     * 
     * @param node
     *            The {@link Node} (or {@link Document}) for which to get the
     *            string value, not <code>null</code>.
     * @param omitXmlDeclaration
     *            <code>true</code> to exclude the XML declaration in the
     *            generated string.
     * @return The string representation of the {@link Node}, or <code>null</code> in case of an error.
     */
    // rem: Some of the other implementations of this method returned an empty string, when an error was encountered.
    // Although some existing code might rely on that, please do *not* change this back, as the null return signify an
    // error. Change dependent code to handle errors accordingly. -- Philipp, 2012-07-01
    public static String xmlToString(Node node, boolean omitXmlDeclaration) {
        Validate.notNull(node, "node must not be null.");

        String ret = null;
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TRANSFORMER_FACTORIES.get();
            Transformer transformer = factory.newTransformer();
            if (omitXmlDeclaration) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }
            // http://stackoverflow.com/questions/1409091/how-do-i-prevent-the-java-xml-transformer-using-html-method-from-adding-meta
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.transform(source, result);
            ret = stringWriter.toString();
        } catch (TransformerConfigurationException e) {
            LOGGER.error("Encountered TransformerConfigurationException while transforming Node: " + e.getMessage());
        } catch (TransformerException e) {
            LOGGER.error("Encountered TransformerException while transforming Node: " + e.getMessage());
        }
        return ret;
    }

    /**
     * <p>
     * Get a string representation of the supplied DOM {@link Node}.
     * </p>
     * 
     * @param node
     *            The {@link Node} (or {@link Document}) for which to get the
     *            string value.
     * @return The string representation of the {@link Node}, or <code>null</code> in case of an error.
     */
    public static String xmlToString(Node node) {
        return xmlToString(node, false);
    }

    /**
     * <p>
     * Print DOM tree for diagnostic purposes. The output includes all Nodes and their Attributes (prefixed with @).
     * </p>
     * 
     * @param node
     */
    public static void printDom(Node node) {
        printDom(node, 0);
    }

    private static void printDom(Node node, int indent) {
        String indentString = StringUtils.repeat(" ", indent);

        String nodeName = node.getNodeName();
        String prefix = node.getPrefix();
        String namespaceURI = node.getNamespaceURI();
        System.out.println(indentString + nodeName + "(" + prefix + " : " + namespaceURI + ")");

        if (node.getAttributes() != null) {
            for (int i = 0; i < node.getAttributes().getLength(); i++) {
                System.out.println(indentString + "@" + node.getAttributes().item(i));
            }
        }

        Node child = node.getFirstChild();
        while (child != null) {
            printDom(child, indent + 1);
            child = child.getNextSibling();
        }
    }

    /**
     * <p>
     * Get the sub tree of the document or node as text without tags. You could also use {@link documentToHTMLString}
     * and {@link htmlToReadableText} to achieve similar results.
     * </p>
     * 
     * @param node
     *            The node from where to start.
     * @return A text representation of the node and its sub nodes without tags.
     */
    public static String documentToText(Node node) {

        // ignore css and script nodes
        if (node == null || node.getNodeName() == null || node.getNodeName().equalsIgnoreCase("script")
                || node.getNodeName().equalsIgnoreCase("style") || node.getNodeName().equalsIgnoreCase("#comment")
                || node.getNodeName().equalsIgnoreCase("option") || node.getNodeName().equalsIgnoreCase("meta")
                || node.getNodeName().equalsIgnoreCase("head")) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // System.out.println(node.getNodeName()+node.getTextContent());
        String textContent = node.getTextContent();
        if (textContent != null) {

            if (node.getNodeName().equalsIgnoreCase("#text")) {
                sb.append(textContent);
            }

        }
        if (isWrappingNode(node)) {
            sb.append("\n");
        }

        try {
            Node child = node.getFirstChild();
            while (child != null) {
                sb.append(documentToText(child));
                child = child.getNextSibling();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return sb.toString().replaceAll("[ ]{2,}", "");
    }

    // TODO doesn't this belong to PageAnalyzer (actually it was there in the
    // past)
    public static Set<String> getLinks(Document document, boolean inDomain, boolean outDomain, String prefix) {

        Set<String> pageLinks = new HashSet<String>();

        if (document == null) {
            return pageLinks;
        }

        // remove anchors from url
        String url = document.getDocumentURI();
        url = UrlHelper.removeAnchors(url);
        String domain = UrlHelper.getDomain(url, false);

        // get value of base element, if present
        Node baseNode = XPathHelper.getXhtmlNode(document, "//head/base/@href");
        String baseHref = null;
        if (baseNode != null) {
            baseHref = baseNode.getTextContent();
        }

        // get all internal domain links
        // List<Node> linkNodes = XPathHelper.getNodes(document, "//@href");
        List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, "//a/@href");
        for (int i = 0; i < linkNodes.size(); i++) {
            String currentLink = linkNodes.get(i).getTextContent();
            currentLink = currentLink.trim();

            // remove anchors from link
            currentLink = UrlHelper.removeAnchors(currentLink);

            // normalize relative and absolute links
            // currentLink = makeFullURL(url, currentLink);
            currentLink = UrlHelper.makeFullUrl(url, baseHref, currentLink);

            if (currentLink.length() == 0) {
                continue;
            }

            String currentDomain = UrlHelper.getDomain(currentLink, false);
            currentDomain = currentDomain.replaceFirst("[a-zA-Z-_]+\\.(?=[a-z]+\\.)", "");

            boolean inDomainLink = currentDomain.equalsIgnoreCase(domain);

            if ((inDomainLink && inDomain || !inDomainLink && outDomain) && currentLink.startsWith(prefix)) {
                pageLinks.add(currentLink);
            }
        }

        return pageLinks;
    }

    /**
     * Get a set of links from the source page.
     * 
     * @param inDomain
     *            If true all links that point to other pages within the same
     *            domain of the source page are added.
     * @param outDomain
     *            If true all links that point to other pages outside the domain
     *            of the source page are added.
     * @return A set of urls.
     */
    // TODO doesn't this belong to PageAnalyzer (actually it was there in the
    // past)
    public static Set<String> getLinks(Document document, boolean inDomain, boolean outDomain) {
        return getLinks(document, inDomain, outDomain, "");
    }

    /**
     * @param document
     * @return
     */
    public static String getDocumentTextDump(Document document) {
        if (document != null && document.getLastChild() != null) {
            return document.getLastChild().getTextContent();
        }
        return "";
    }

    private static boolean isWrappingNode(Node node) {
        String nodeName = node.getNodeName().toLowerCase();
        return BLOCK_ELEMENTS.contains(nodeName);
    }

    /**
     * <p>
     * Get all siblings of a {@link Node}, depth-first.
     * </p>
     * 
     * @param node The node, not <code>null</code>.
     * @return A list with all siblings in depth-first order, or an empty {@link List}, but never <code>null</code>.
     */
    public static List<Node> getAllSiblings(Node node) {
        Validate.notNull(node, "node must not be null");
        List<Node> result = CollectionHelper.newArrayList();
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            result.add(childNode);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                result.addAll(getAllSiblings(childNode));
            }
        }
        return result;
    }

}
