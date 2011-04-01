package ws.palladian.retrieval;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.cyberneko.html.parsers.DOMParser;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.Counter;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.feeds.discovery.FeedDiscoveryCallback;

import com.sun.syndication.io.XmlReader;

/**
 * The Crawler downloads pages from the web. List of proxies can be found here: http://www.proxy-list.org/en/index.php
 * TODO handle namespace in xpath
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class DocumentRetriever {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DocumentRetriever.class);

    // ///////////// constants with default configuration ////////
    /** The user agent string that is used by the crawler. */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4";

    /** The referer that is used by the crawler. */
    private static final String REFERER = "";

    /** The default connection timeout. */
    public static final int DEFAULT_CONNECTION_TIMEOUT = (int) (10 * DateHelper.SECOND_MS);

    /** The default read timeout when retrieving pages. */
    public static final int DEFAULT_READ_TIMEOUT = (int) (16 * DateHelper.SECOND_MS);

    /** The default overall timeout (after which the connection is reset). */
    public static final int DEFAULT_OVERALL_TIMEOUT = (int) (60 * DateHelper.SECOND_MS);

    /** The default number of retries when downloading fails. */
    public static final int DEFAULT_NUM_RETRIES = 0;
    
    public enum SizeUnit {
        BYTES(1), KILOBYTES(1024), MEGABYTES(1048576), GIGABYTES(1073741824);
        private int bytes;
        SizeUnit(int bytes) {
            this.bytes = bytes;
        }
    }

    // //////////////////general settings ////////////////////

    /** the document that is created after retrieving a web page */
    private Document document = null;

    /** the connection timeout which should be used */
    private int connectionTimout = DEFAULT_CONNECTION_TIMEOUT;

    /** the read timeout which should be used */
    private int readTimeout = DEFAULT_READ_TIMEOUT;

    /** the overall timeout which should be used */
    private int overallTimeout = DEFAULT_OVERALL_TIMEOUT;

    /** the number of retries when downloading fails. */
    private int numRetries = DEFAULT_NUM_RETRIES;

    /** The stack to store URLs to be downloaded. */
    private Stack<String> urlStack = new Stack<String>();

    /** number of active threads */
    private int threadCount = 0;

    /** Accumulates the download size in bytes for this crawler. */
    private long totalDownloadSize = 0;

    /** Saves the last download size in bytes for this crawler. */
    private long lastDownloadSize = 0;

    /** Keep track of the total number of bytes downloaded by all crawler instances used. */
    private static long sessionDownloadedBytes = 0;

    private static int numberOfDownloadedPages = 0;

    /** Whether to use HTTP compression or not. */
    private boolean useCompression = true;

    /** Try to use feed auto discovery for every parsed page. */
    private boolean feedAutodiscovery = false;

    /** The response code of the last HTTP request. */
    // private int lastResponseCode = -1;

    /** The filter for the crawler. */
    private DownloadFilter downloadFilter = new DownloadFilter();

    /**
     * Guess the average compression ratio that could be reached using gzip or deflate. If set this to zero, the
     * download only interrupts after reaching the maxFileSize, otherwise already after reaching
     * (1 / compressionSaving)[%] * maxFileSize [Bytes] (only if compression can be used).
     */
    private double compressionSaving = 0.5;

    protected boolean sanitizeXml = true;

    // ////////////////// proxy settings ////////////////////
    /** the proxy to use */
    private Proxy proxy = null;

    /** number of request before switching to another proxy, -1 means never switch */
    private int switchProxyRequests = 20;

    /** list of proxies to choose from */
    private LinkedList<Proxy> proxyList = new LinkedList<Proxy>();

    /** number of requests sent with currently used proxy. */
    private int proxyRequests = 0;


    private Set<Document> downloadedDocuments = new HashSet<Document>();

    private int maxThreads = 10;

    private int maxFails = 10;

    /** The callback that is called after each crawled page. */
    private Set<CrawlerCallback> crawlerCallbacks = new HashSet<CrawlerCallback>();

    /**
     * Start downloading the supplied URLs.
     * 
     * @param callback The callback to be called for each finished download. If the callback is null, the method will
     *            save the downloaded documents and return them when finished.
     */
    public Set<Document> start(final DocumentRetrieverCallback callback) {

        LOGGER.trace(">start");

        downloadedDocuments = new HashSet<Document>();

        // to count number of running Threads
        final Counter counter = new Counter();

        final Counter errors = new Counter();

        while (urlStack.size() > 0) {
            final String url = urlStack.pop();

            LOGGER.debug("process url " + url);

            // if maximum # of Threads are already running, wait here
            while (counter.getCount() >= getMaxThreads()) {
                LOGGER.trace("max # of Threads running. waiting ...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    return downloadedDocuments;
                }
            }

            if (errors.getCount() == getMaxFails()) {
                LOGGER.warn("max. fails of " + getMaxFails() + " reached. giving up.");
                return new HashSet<Document>();
            }

            counter.increment();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.trace("start downloading " + url);
                        InputStream inputStream = downloadInputStream(url);

                        if (callback == null) {
                            Document document = getWebDocument(inputStream, url);
                            synchronized (downloadedDocuments) {
                                downloadedDocuments.add(document);
                            }
                        } else {
                            callback.finished(url, inputStream);
                        }

                        LOGGER.trace("finished downloading " + url);
                    } catch (Exception e) {
                        LOGGER.warn(e.getMessage() + " for " + url);
                        errors.increment();
                    } finally {
                        counter.decrement();
                    }
                }
            };
            new Thread(runnable, "URLDownloaderThread:" + url).start();
        }

        // keep on running until all Threads have finished and
        // the Stack is empty
        while (counter.getCount() > 0 || urlStack.size() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                return downloadedDocuments;
            }
            LOGGER.debug("waiting ... threads:" + counter.getCount() + " stack:" + urlStack.size());
        }
        LOGGER.trace("<start");

        return downloadedDocuments;
    }

    public Set<Document> start() {
        return start(null);
    }

    /**
     * Add a URL to be downloaded.
     * 
     * @param urlString
     */
    public void add(String urlString) {
        if (!urlStack.contains(urlString)) {
            urlStack.push(urlString);
        }
    }

    /**
     * Add a collection of URLs to be downloaded.
     * 
     * @param urls A collection of URLs.
     */
    public void add(Collection<String> urls) {
        for (String url : urls) {
            if (!urlStack.contains(url)) {
                urlStack.push(url);
            }
        }
    }

    /**
     * Set the maximum number of simultaneous threads for downloading.
     * 
     * @param maxThreads
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Set the maximum number of failures to stop the download.
     * 
     * @param maxFails
     */
    public void setMaxFails(int maxFails) {
        this.maxFails = maxFails;
    }

    public int getMaxFails() {
        return maxFails;
    }

    // ///////////////////// constructors ///////////////////////

    public DocumentRetriever() {
        loadConfig();
    }

    public DocumentRetriever(int connectionTimeOut, int readTimeOut, int overallTimeOut) {
        loadConfig();
        setConnectionTimout(connectionTimeOut);
        setReadTimeout(readTimeOut);
        setOverallTimeout(overallTimeOut);
    }

//    public DocumentRetriever(String configPath) {
//        loadConfig();
//    }

    /**
     * Load the configuration file from the specified location and set the variables accordingly.
     */
    @SuppressWarnings("unchecked")
    public final void loadConfig() {

        ConfigHolder configHolder = ConfigHolder.getInstance();
        PropertiesConfiguration config = configHolder.getConfig();

        if (config != null) {
            setMaxThreads(config.getInt("documentRetriever.maxThreads"));
            setSwitchProxyRequests(config.getInt("documentRetriever.switchProxyRequests"));
            setProxyList(config.getList("documentRetriever.proxyList"));
            setFeedAutodiscovery(config.getBoolean("documentRetriever.feedAutoDiscovery"));
            setNumRetries(config.getInt("documentRetriever.numRetries", DEFAULT_NUM_RETRIES));
        } else {
            LOGGER.warn("could not load configuration, use defaults");
        }

    }

    public String getUserAgent() {
        return USER_AGENT;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setDocument(String url) {
        setDocument(url, false, true);
    }

    public void setDocument(URL url, Boolean isXML, boolean callback) {
        document = null;

        try {
            File file = new File(url.toURI());
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));

            parse(is, isXML, url.toExternalForm());

            // only call, if we actually got a Document; so we don't need to check for null within the Callback
            // implementation itself.
            if (callback && document != null) {
                callCrawlerCallback(document);
            }
        } catch (URISyntaxException e) {
            LOGGER.error(e);
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } catch (SAXException e) {
            LOGGER.error(e);
        } catch (ParserConfigurationException e) {
            LOGGER.error(e);
        }
    }

    public void setDocument(String url, boolean isXML, boolean callback) {
        setDocument(url, isXML, callback, null);
    }

    public void setDocument(String url, boolean isXML, boolean callback, HeaderInformation headerInformation) {
        document = null;

        url = url.trim();

        try {
            
            boolean isFile = false;
            if (url.indexOf("http://") == -1 && url.indexOf("https://") == -1) {
                isFile = true;
            }

            // read from file with buffered input stream
            if (isFile) {

                // InputSource is = new InputSource(new BufferedInputStream(new FileInputStream(url)));
                File file = new File(url);
                BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));

                parse(is, isXML, file.toURI().toString());
            } else {
                url = url.replaceAll("\\s", "+");
                URL urlObject = new URL(url);
                InputStream fis = getInputStream(urlObject, headerInformation);

                parse(fis, isXML, url);
            }

            // only call, if we actually got a Document; so we don't need to check for null within the Callback
            // implementation itself.
            if (callback && document != null) {
                callCrawlerCallback(document);
            }

        } catch (SAXException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (FileNotFoundException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (UnknownHostException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (DOMException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error(url + ", " + e.getClass() + " " + e.getMessage());
            // e.printStackTrace();
        } catch (Throwable t) {
            // see @FIXME in #parse
            LOGGER.error(url + ", " + t.getMessage());
        }

    }

    /**
     * <p>
     * Parses a an input stream to a document.
     * </p>
     * 
     * @param dataStream The stream to parse.
     * @param isXML {@code true} if this document is an XML document and {@code false} otherwise.
     * @param uri The URI the provided stream comes from.
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void parse(InputStream dataStream, boolean isXML, String uri) throws SAXException, IOException,
    ParserConfigurationException {
        DOMParser parser = new DOMParser();

        // experimental fix for http://redmine.effingo.de/issues/5
        // also see: tud.iir.web.CrawlerTest.testNekoWorkarounds()
        // Philipp, 2010-11-10
        //
        // FIXME 2011-01-06; this seems to cause this problem:
        // http://sourceforge.net/tracker/?func=detail&aid=3109537&group_id=195122&atid=952178
        // catching Throwable in #setDocument above; guess we have to wait for a new Neko release,
        // supposedly breaking other stuff :(
        parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);
        parser.setProperty("http://cyberneko.org/html/properties/filters", new XMLDocumentFilter[] { new TBODYFix() });
        // end fix.

        if (isXML) {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

            // added by Philipp, 2011-01-28
            docBuilderFactory.setNamespaceAware(true);

            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

            if (sanitizeXml) {
                // fix to parse XML documents with illegal characters, 2011-02-15
                // see http://java.net/projects/rome/lists/users/archive/2009-04/message/12
                // and http://info.tsachev.org/2009/05/skipping-invalid-xml-character-with.html
                // although XmlReader is from ROME, I suppose it can be used for general XML applications
                XmlReader xmlReader = new XmlReader(dataStream);
                Xml10FilterReader filterReader = new Xml10FilterReader(xmlReader);
                InputSource is = new InputSource(filterReader);
                // LOGGER.debug("encoding : " + xmlReader.getEncoding());
                // end fix.
                document = docBuilder.parse(is);
            } else {
                document = docBuilder.parse(dataStream);
            }

        } else {
            InputSource is = new InputSource(dataStream);
            parser.parse(is);
            document = parser.getDocument();
        }

        document.setDocumentURI(uri);
    }

    public Document getDocument() {
        return document;
    }

    /**
     * Get a web page ((X)HTML document).
     * 
     * @param url The URL or file path of the web page.
     * @return The W3C document.
     */
    public Document getWebDocument(String url) {
        setDocument(url, false, true);
        numberOfDownloadedPages++;
        return getDocument();
    }

    /**
     * Get a web page ((X)HTML document).
     * 
     * @param url The URL or file path of the web page.
     * @return The W3C document.
     */
    public Document getWebDocument(URL url) {
        setDocument(url.toExternalForm(), false, true);
        numberOfDownloadedPages++;
        return getDocument();
    }

    public Document getWebDocument(InputStream is, String URI) {
        try {
            parse(is, false, URI);
        } catch (SAXException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (ParserConfigurationException e) {
            LOGGER.error(e.getMessage());
        }

        return getDocument();
    }

    /**
     * Get a web page ((X)HTML document).
     * 
     * @param url The URL or file path of the web page.
     * @param callback set to <code>false</code> to disable callback for this document.
     * @return The W3C document.
     */
    public Document getWebDocument(String url, boolean callback) {
        setDocument(url, false, callback);
        return getDocument();
    }

    /**
     * Get XML document from a URL. Pure XML documents can created with the native DocumentBuilderFactory, which works
     * better with the native XPath queries.
     * 
     * @param url The URL or file path pointing to the XML document.
     * @return The XML document.
     */
    public Document getXMLDocument(String url) {
        // setDocument(url, true, true);
        setDocument(url, true, false);
        return getDocument();
    }

    /**
     * Get XML document from a URL. Pure XML documents can be created with the native DocumentBuilderFactory, which
     * works
     * better with the native XPath queries.
     * 
     * @param url The URL or file path pointing to the XML document.
     * @param callback Set to <code>false</code> to disable callback for this document.
     * @return The XML document.
     */
    public Document getXMLDocument(String url, boolean callback) {
        setDocument(url, true, callback);
        return getDocument();
    }

    public Document getXMLDocument(String url, boolean callback, HeaderInformation headerInformation) {
        setDocument(url, true, callback, headerInformation);
        return getDocument();
    }

    public Document getXMLDocument(URL url, boolean callback) {
        setDocument(url, true, callback);
        return getDocument();
    }

    /**
     * Get a json object from a URL. The retrieved contents must return a valid json object.
     * 
     * @param url The url pointing to the json string.
     * @return The json object.
     */
    public JSONObject getJSONDocument(String url) {
        String json = download(url);

        // delicous feeds return the whole JSON object wrapped in [square brackets],
        // altough this seems to be valid, our parser doesn't like this, so we remove
        // those brackets before parsing -- Philipp, 2010-07-04
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1);
        }

        JSONObject jsonOBJ = null;

        if (json.length() > 0) {
            try {
                jsonOBJ = new JSONObject(json);
            } catch (JSONException e) {
                LOGGER.error(url + ", " + e.getMessage());
            }
        }

        return jsonOBJ;
    }

    /**
     * Download the contents that are retrieved from the given URL.
     * 
     * @param urlString The URL of the desired contents.
     * @param stripTags If true, HTML tags will be stripped (but not comments, js and css tags).
     * @param stripComments If true, comment tags will be stripped.
     * @param stripJSAndCSS If true, JavaScript and CSS tags will be stripped
     * @param joinTagsAndRemoveNewlines If true, multiple blank spaces and line breaks will be removed.
     * @return The contents as a string.
     */
    public String download(String urlString, boolean stripTags, boolean stripComments, boolean stripJSAndCSS,
            boolean joinTagsAndRemoveNewlines) {

        // do not download pdf, ppt or ps files TODO try to download them as
        // well
        if (urlString.indexOf(".pdf") > -1 || urlString.indexOf(".ps") > -1 || urlString.indexOf(".ppt") > -1) {
            return "";
        }

        boolean isFile = false;
        if (urlString.indexOf("http://") == -1 && urlString.indexOf("https://") == -1) {
            isFile = true;
        } else {
            isFile = false;
        }

        String contentString = "";

        // read from file with buffered input stream
        if (isFile) {
            contentString = FileHelper.readHTMLFileToString(urlString, stripTags);
        } else {
            StringBuilder html = new StringBuilder();

            try {
                urlString = urlString.replaceAll("\\s", "+");
                URL url = new URL(urlString);
                InputStream inputStream = getInputStream(url);
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                do {
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    html.append(line).append("\n");
                } while (line != null);

                br.close();

            } catch (FileNotFoundException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (SocketTimeoutException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (MalformedURLException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (IOException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (OutOfMemoryError e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (NullPointerException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (Exception e) {
                LOGGER.error(urlString + ", " + e.getClass() + " " + e.getMessage());
            }

            contentString = html.toString();
        }

        if (stripTags || stripComments || stripJSAndCSS) {
            contentString = HTMLHelper.stripHTMLTags(contentString, stripTags, stripComments, stripJSAndCSS,
                    joinTagsAndRemoveNewlines);
        }

        return contentString;
    }

    public String download(String urlString) {
        return download(urlString, false);
    }

    public String download(String urlString, boolean stripTags) {
        return download(urlString, stripTags, false, false, false);
    }

    public void downloadAndSave(HashSet<String> urlSet) {
        Iterator<String> urlSetIterator = urlSet.iterator();
        int number = 1;
        while (urlSetIterator.hasNext()) {
            downloadAndSave(urlSetIterator.next(), "website" + number + ".html");
            ++number;
        }
    }

    public boolean downloadAndSave(String urlString, String path) {

        String content = download(urlString);

        if (content.length() == 0) {
            LOGGER.warn(urlString + " was not found, or contained no content, it is not saved in a file");
            return false;
        }

        try {
            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write(content.toString());
            fileWriter.flush();
            fileWriter.close();

            return true;

        } catch (FileNotFoundException e) {
            LOGGER.error(urlString + " " + path + ", " + e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.error(urlString + " " + path + ", " + e.getMessage());
            return false;
        }

    }

//    public void downloadImage(String url, String path) {
//        ImageHandler.downloadAndSave(url, path);
//    }

    public long getTotalDownloadSize() {
        return getTotalDownloadSize(SizeUnit.BYTES);
    }

    public long getTotalDownloadSize(SizeUnit unit) {
        return totalDownloadSize / unit.bytes; 
    }

    public void setTotalDownloadSize(long totalDownloadSize) {
        this.totalDownloadSize = totalDownloadSize;
    }

    public long getLastDownloadSize() {
        return lastDownloadSize;
    }

    public void setLastDownloadSize(long lastDownloadSize) {
        this.lastDownloadSize = lastDownloadSize;
    }

    /**
     * This method must be "synchronized", as multiple threads might use the crawler and increments are not atomic.
     * 
     * @param size The size in bytes that should be added to the download counters.
     */
    private synchronized void addDownloadSize(long size) {
        this.totalDownloadSize += size;
        this.lastDownloadSize = size;
        sessionDownloadedBytes += size;
    }
    
    public static long getSessionDownloadSize() {
        return getSessionDownloadSize(SizeUnit.BYTES);
    }

    public static long getSessionDownloadSize(SizeUnit unit) {
        return sessionDownloadedBytes / unit.bytes;
    }
    
    // XXX
    public static void setSessionDownloadedBytes(long sessionDownloadedBytes) {
        DocumentRetriever.sessionDownloadedBytes = sessionDownloadedBytes;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void increaseThreadCount() {
        this.threadCount++;
    }

    public void decreaseThreadCount() {
        this.threadCount--;
    }

    /**
     * Returns the current Proxy.
     * 
     * @return
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Sets the current Proxy.
     * 
     * @param proxy
     */
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    /**
     * Check whether to change the proxy and do it if needed. If a proxy is not working, remove it from the list. If we
     * have no working proxies left, fall back into normal mode.
     * 
     * @param force force the proxy change, no matter if the specified number of request for the switch has already been
     *            reached.
     */
    private void checkChangeProxy(boolean force) {
        if (switchProxyRequests > -1 && (force || proxyRequests == switchProxyRequests || proxy == null)) {
            if (force) {
                LOGGER.debug("force-change proxy");
            }
            boolean continueChecking = true;
            do {
                changeProxy();
                if (checkProxy()) {
                    continueChecking = false;
                } else {

                    // proxy is not working; remove it from the list
                    LOGGER.warn("proxy " + getProxy().address() + " is not working, removing from the list.");
                    proxyList.remove(getProxy());
                    LOGGER.debug("# proxies in list: " + proxyList.size() + " : " + proxyList);

                    // if there are no more proxies left, go to normal mode without proxies.
                    if (proxyList.isEmpty()) {
                        LOGGER.error("no more working proxies, falling back to normal mode.");
                        continueChecking = false;
                        proxy = null;
                        setSwitchProxyRequests(-1);
                    }
                }
            } while (continueChecking);
            proxyRequests = 0;
        }
    }

    /**
     * Number of requests after the proxy is changed.
     * 
     * @param switchProxyRequests number of requests for proxy change. Must be greater than 1 or -1 which means: change
     *            never.
     */
    public void setSwitchProxyRequests(int switchProxyRequests) {
        if (switchProxyRequests == 0) {
            throw new IllegalArgumentException();
        }
        this.switchProxyRequests = switchProxyRequests;
    }

    public int getSwitchProxyRequests() {
        return switchProxyRequests;
    }

    /**
     * Add an entry to the proxy list. The entry must be formatted as "HOST:PORT".
     * 
     * @param proxyEntry The proxy to add.
     */
    public void addToProxyList(String proxyEntry) {
        String[] proxySetting = proxyEntry.split(":");
        String host = proxySetting[0];
        int port = Integer.parseInt(proxySetting[1]);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        if (!proxyList.contains(proxy)) {
            proxyList.add(proxy);
        }
    }

    /**
     * Set a list of proxies. Each entry must be formatted as "HOST:PORT".
     * 
     * @param proxyList The list of proxies.
     */
    public void setProxyList(List<String> proxyList) {
        this.proxyList = new LinkedList<Proxy>();
        for (String proxy : proxyList) {
            addToProxyList(proxy);
        }
    }

    /*
     * public void setProxyList(List<Proxy> proxyList) {
     * this.proxyList = new LinkedList<Proxy>(proxyList);
     * }
     */

    public List<Proxy> getProxyList() {
        return proxyList;
    }

    /**
     * Cycle the proxies, taking the first item from the queue and adding it to the end.
     */
    public void changeProxy() {
        Proxy selectedProxy = proxyList.poll();
        if (selectedProxy != null) {
            setProxy(selectedProxy);
            proxyList.add(selectedProxy);
            LOGGER.debug("changed proxy to " + selectedProxy.address());
        }
    }

    /**
     * Check whether the curretly set proxy is working.
     * 
     * @return True if proxy returns result, false otherwise.
     */
    public boolean checkProxy() {
        boolean result;
        try {
            // try to download from Google, if downloading fails we get IOException
            downloadInputStream("http://www.google.com", false);
            if (getProxy() != null) {
                LOGGER.debug("proxy " + getProxy().address() + " is working.");
            }
            result = true;
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    /**
     * Get HTTP Headers of an URLConnection to pageURL.
     */
    public Map<String, List<String>> getHeaders(String pageURL) {
        URL url;
        URLConnection conn;
        Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
        try {
            url = new URL(pageURL);
            conn = url.openConnection();
            headerMap = conn.getHeaderFields();

        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return headerMap;
    }

    /**
     * Gets the redirect URL, if such exists.
     * @param urlString original URL.
     * @return redirected URL as String or null;
     */
    public String getRedirectUrl(String urlString){
        URL url = null;
        URLConnection urlCon;
        HttpURLConnection httpUrlCon;
        String location = null;
        try {
            url = new URL(urlString);
            urlCon = url.openConnection();
            httpUrlCon = (HttpURLConnection) urlCon;
            httpUrlCon.setInstanceFollowRedirects(false);
            location = httpUrlCon.getHeaderField("Location");

        } catch (IOException e) {
            LOGGER.error(e);
        }

        return location;
    }
    
    /**
     * Download a binary file from specified URL to a given path.
     * 
     * @param urlString the urlString
     * @param pathWithFileName the path where the file should be saved
     * @return the file
     * @author Martin Werner
     */
    public static File downloadBinaryFile(String urlString, String pathWithFileName) {
        File binFile = null;

        URL u;
        binFile = new File(pathWithFileName);
        try {
            u = new URL(urlString);
            BufferedInputStream in = new BufferedInputStream(u.openStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(binFile));

            byte[] buffer = new byte[4096];

            int n = 0;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            in.close();
            out.close();

            int size = (int) binFile.length();
            sessionDownloadedBytes += size;


        } catch (Exception e) {

            LOGGER.error("Error downloading the file from: " + urlString + " " + e.getMessage());
            binFile = null;

        } catch (Error e) {

            LOGGER.error("Error downloading the file from: " + urlString + " " + e.getMessage());
            binFile = null;
        }

        return binFile;
    }

    /**
     * Gets an input stream, with specified number of retries, if downloading fails (see {@link #setNumRetries(int)}.
     * After the specified number of retries without success, an IOException is thrown. After each failing attempt the
     * proxies are cycled, if switching proxies is enabled (see {@link #setSwitchProxyRequests(int)}.
     * 
     * @param url
     * @return
     * @throws IOException
     * @author Philipp Katz
     */
    private InputStream getInputStream(URL url, HeaderInformation headerInformation) throws IOException {
        InputStream result = null;
        int retry = 0;
        boolean keepTrying = true;
        do {
            try {
                result = downloadInputStream(url, true, headerInformation);
                keepTrying = false;
            } catch (IOException e) {
                if (retry >= getNumRetries()) {
                    throw new IOException("maximum retries of " + getNumRetries() + " reached for " + url, e);
                }
                retry++;
                LOGGER.warn("failed to download " + url + " : " + e.getMessage() + " re-try " + retry + " of "
                        + getNumRetries());
                checkChangeProxy(true);
            }
        } while (keepTrying);
        return result;
    }

    private InputStream getInputStream(URL url) throws IOException {
        return getInputStream(url, null);
    }

    /**
     * Download from specified URL. This method caches the incoming InputStream and blocks until all incoming data has
     * been read or the timeout has been
     * reached.
     * 
     * @param url The URL to download.
     * @return The input stream.
     * @throws IOException
     * @author Philipp Katz
     */
    public InputStream downloadInputStream(URL url) throws IOException {
        return downloadInputStream(url, true);
    }

    /**
     * Download from specified URL string. This method caches the incoming InputStream and blocks until all incoming
     * data has been read or the timeout has been reached.
     * 
     * @param urlString
     * @return
     * @throws IOException
     */
    public InputStream downloadInputStream(String urlString) throws IOException {
        return downloadInputStream(new URL(urlString));
    }

    private InputStream downloadInputStream(String urlString, boolean checkChangeProxy) throws IOException {
        return downloadInputStream(new URL(urlString), checkChangeProxy);
    }

    private InputStream downloadInputStream(URL url, boolean checkChangeProxy) throws IOException {
        return downloadInputStream(url, checkChangeProxy, null);

    }

    private InputStream downloadInputStream(URL url, boolean checkChangeProxy, HeaderInformation headerInformation)
    throws IOException {
        LOGGER.trace(">download " + url);

        // check whether we are allowed to download the file from this URL
        String fileType = FileHelper.getFileType(url.toString());
        if (!getDownloadFilter().getIncludeFileTypes().contains(fileType)
                && getDownloadFilter().getIncludeFileTypes().size() > 0
                || getDownloadFilter().getExcludeFileTypes().contains(fileType)) {
            LOGGER.debug("filtered URL: " + url);
            return null;
        }

        ConnectionTimeout timeout = null;
        InputStream result = null;

        try {

            if (checkChangeProxy) {
                checkChangeProxy(false);
                proxyRequests++;
            }

            URLConnection urlConnection;
            if (proxy != null) {
                urlConnection = url.openConnection(proxy);
            } else {
                urlConnection = url.openConnection();
            }
            urlConnection.setConnectTimeout(connectionTimout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            urlConnection.setRequestProperty("Referer", REFERER);
            if (useCompression) {
                urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            }

            if (headerInformation != null) {

                if (headerInformation.getLastModifiedSince() != null) {
                    urlConnection.setIfModifiedSince(headerInformation.getLastModifiedSince().getTime());
                }
                if (headerInformation.getETag().length() > 0) {
                    urlConnection.setRequestProperty("Etag", headerInformation.getETag());
                }

            }

            // use connection timeout from Palladian
            timeout = new ConnectionTimeout(urlConnection, overallTimeout);

            // TODO? getResponseCode seems to be extremely slow or just hangs, similar to
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4352956
            // if (urlConnection instanceof HttpURLConnection) {
            // try {
            // setLastResponseCode(((HttpURLConnection) urlConnection).getResponseCode());
            // } catch (Exception e) {
            // LOGGER.error("could not get response code for URL " + url + ", " + e.getMessage());
            // }
            // }

            // buffer incoming InputStream
            InputStream urlInputStream = urlConnection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            long cumLength = 0; // :-O
            long maxFileSize = getDownloadFilter().getMaxFileSize();
            while ((length = urlInputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, length);
                cumLength += length;
                if (maxFileSize > -1
                        && (cumLength > maxFileSize || useCompression
                                && cumLength > 1.0 / compressionSaving * maxFileSize)) {
                    addDownloadSize(cumLength);
                    LOGGER.warn("the contents of " + url + " were too big, stop downloading...");
                    throw new IOException();
                }
            }
            urlInputStream.close();
            outputStream.close();

            addDownloadSize(outputStream.size());

            result = new ByteArrayInputStream(outputStream.toByteArray());

            // XXX? getContentEncoding might block, another fix might be to get the encoding ourself by reading the
            // header
            String encoding = "";
            if (useCompression) {
                encoding = urlConnection.getContentEncoding();
            }
            LOGGER.trace("encoding " + encoding);

            // if result is compressed, wrap it accordingly
            if (encoding != null) {
                if (encoding.equalsIgnoreCase("gzip")) {
                    result = new GZIPInputStream(result);
                } else if (encoding.equalsIgnoreCase("deflate")) {
                    result = new DeflaterInputStream(result);
                }
            }

        } finally {
            if (timeout != null) {
                timeout.setActive(false);
            }
        }

        LOGGER.trace("<download");
        return result;
    }

    /**
     * Get the response code of the given url after sending a HEAD request.
     * This works only for HTTP connections.
     * 
     * @param urlString The URL.
     * @return The HTTP response Code.
     */
    public int getResponseCode(String urlString) {

        int responseCode = -1;

        URL url;
        try {

            checkChangeProxy(false);
            proxyRequests++;

            url = new URL(urlString);

            HttpURLConnection urlConnection;

            if (proxy != null) {
                urlConnection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }

            urlConnection.setRequestMethod("HEAD");

            urlConnection.setConnectTimeout(connectionTimout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            urlConnection.setRequestProperty("Referer", REFERER);
            if (useCompression) {
                urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            }

            urlConnection.connect();

            responseCode = urlConnection.getResponseCode();

        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        } catch (ProtocolException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return responseCode;
    }

    /**
     * Use to disable compression.
     * 
     * @param useCompression If true, compression will be used, if false then not.
     */
    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    public void setConnectionTimout(int connectionTimout) {
        this.connectionTimout = connectionTimout;
    }

    public int getConnectionTimout() {
        return connectionTimout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setOverallTimeout(int overallTimeout) {
        this.overallTimeout = overallTimeout;
    }

    public int getOverallTimeout() {
        return overallTimeout;
    }

    public boolean isFeedAutodiscovery() {
        return feedAutodiscovery;
    }

    public void setFeedAutodiscovery(boolean feedAutodiscovery) {
        this.feedAutodiscovery = feedAutodiscovery;
        if (feedAutodiscovery) {
            LOGGER.trace("enabled feed autodiscovery");
            addCrawlerCallback(FeedDiscoveryCallback.getInstance());
        } else {
            LOGGER.trace("disabled feed autodiscovery");
            removeCrawlerCallback(FeedDiscoveryCallback.getInstance());
        }
    }

    private void callCrawlerCallback(Document document) {
        for (CrawlerCallback crawlerCallback : crawlerCallbacks) {
            LOGGER.trace("call crawler callback " + crawlerCallback + "  for " + document.getDocumentURI());
            crawlerCallback.crawlerCallback(document);
        }
    }

    public Set<CrawlerCallback> getCrawlerCallbacks() {
        return crawlerCallbacks;
    }

    public void addCrawlerCallback(CrawlerCallback crawlerCallback) {
        crawlerCallbacks.add(crawlerCallback);
    }

    public void removeCrawlerCallback(CrawlerCallback crawlerCallback) {
        crawlerCallbacks.remove(crawlerCallback);
    }

    public void setNumRetries(int numRetries) {
        this.numRetries = numRetries;
    }

    public int getNumRetries() {
        return numRetries;
    }

    public static Document getWebDocumentFromInputStream(InputStream iStream, String url){
        DOMParser parser = new DOMParser();
        Document document = null;

        try {
            parser.parse(new InputSource(iStream));
        } catch (SAXException saxEx) {
            LOGGER.error(saxEx.getMessage());
        } catch (IOException ioEx) {
            LOGGER.error(ioEx.getMessage());
        }

        document = parser.getDocument();
        if (document != null) {
            document.setDocumentURI(url);
        }

        return document;
    }

    public void setDownloadFilter(DownloadFilter downloadFilter) {
        this.downloadFilter = downloadFilter;
    }

    public DownloadFilter getDownloadFilter() {
        return downloadFilter;
    }

    public static void performanceCheck() {

        Set<String> urls = new HashSet<String>();

        urls.add("http://www.literatura-obcojezyczna.1up.pl/mapa/1107045/informatyka/");
        urls.add("http://www.territorioscuola.com/wikipedia/en.wikipedia.php?title=Wikipedia:WikiProject_Deletion_sorting/Bands_and_musicians/archive");
        urls.add("http://www.designquote.net/directory/ny");
        urls.add("http://wikyou.info/index3.php?key=clwyd");
        urls.add("http://lashperfect.com/eyelash-salon-finder");
        urls.add("http://www.ics.heacademy.ac.uk/publications/book_reviews/books.php?status=r&ascendby=author");
        urls.add("http://www.letrs.indiana.edu/cgi/t/text/text-idx?c=wright2;cc=wright2;view=text;rgn=main;idno=wright2-0671");
        urls.add("http://justintadlock.com/archives/2007/12/09/structure-wordpress-theme");
        urls.add("http://www.editionbeauce.com/archives/photos/");
        urls.add("http://nouvellevintage.wordmess.net/20100309/hello-from-the-absense/");
        urls.add("http://xn--0tru33arqi4jn7xzda.jp/index3.php?key=machinerie");
        urls.add("http://katalog.svkul.cz/a50s.htm");
        urls.add("http://gosiqumup.fortunecity.com/2009_04_01_archive.html");
        urls.add("http://freepages.genealogy.rootsweb.ancestry.com/~sauve/indexh.htm");
        urls.add("http://www.blog-doubleclix.com/index.php?q=erix");
        urls.add("http://meltingpot.fortunecity.com/virginia/670/FichierLoiselle.htm");
        urls.add("http://canada-info.ca/directory/category/consulting/index.php");
        urls.add("http://www.infopig.com/news/07-19-2008.html");

        DocumentRetriever crawler = new DocumentRetriever();
        crawler.getDownloadFilter().setMaxFileSize(-1);

        double[] x = new double[urls.size()];
        double[] y = new double[urls.size()];
        int c = 0;
        long sumBytes = 0;
        long sumTime = 0;
        for (String url : urls) {
            StopWatch sw = new StopWatch();
            crawler.getWebDocument(url);

            LOGGER.info(sw.getElapsedTimeString() + " for " + crawler.getLastDownloadSize() + " Bytes of url "
                    + url);

            sumBytes += crawler.getLastDownloadSize();
            sumTime += sw.getElapsedTime();
            x[c] = crawler.getLastDownloadSize();
            y[c] = sw.getElapsedTime();
            c++;
        }

        double[] parameters = MathHelper.performLinearRegression(x, y);

        LOGGER.info("the linear regression formula for download and parsing time [ms] in respect to the size is: "
                + Math.round(parameters[0])
                + " * downloadSize [KB] + " + parameters[1]);
        LOGGER.info("total time [ms] and total traffic [Bytes]: " + sumTime + " / " + sumBytes);
        LOGGER.info("on average: " + MathHelper.round(sumBytes / 1024 / (sumTime / 1000), 2) + "[KB/s]");
    }

}