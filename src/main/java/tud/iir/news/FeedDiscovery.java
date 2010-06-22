package tud.iir.news;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.DateHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.ThreadHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * FeedDiscovery works like the following:
 * <ol>
 * <li>Query search engine with some terms (I use Yahoo, as I can get large
 * amounts of results)
 * <li>Get root URLs for hits
 * <li>Check page for feeds using RSS/Atom autodiscovery feature
 * </ol>
 * 
 * @author Philipp Katz
 * 
 */
public class FeedDiscovery {
	
	private static final Logger logger = Logger.getLogger(FeedDiscovery.class);
	
	private static final int MAX_NUMBER_OF_THREADS = 10;
	
	private boolean debugDump = false;

	private int maxThreads = MAX_NUMBER_OF_THREADS;	
	
	
	/** store all sites for which we will do the autodiscovery */
	private Set<String> sites = Collections.synchronizedSet(new HashSet<String>());
	
	/** store all discovered feeds urls */
	private Set<String> feeds = Collections.synchronizedSet(new HashSet<String>());
	
	/** limit results for each query */
	private int resultLimit = 10;
	
	/** ignore list, will be matched with URLs */
	private Set<String> ignoreList = new HashSet<String>();
	
	/** some statistics */
	
	/** total # of sites we checked */
	private Counter totalSites = new Counter();
	/** # of sites containing a feed */
	private Counter feedSites = new Counter();
	/** # of sites with Atom feed */
	private Counter atomFeeds = new Counter();
	/** # of sites with RSS feed */
	private Counter rssFeeds = new Counter();
	/** # of sites with multiple feeds */
	private Counter multipleFeeds = new Counter();
	/** # of errors */
	private Counter errors = new Counter();
	/** # of ignored feeds */
	private Counter ignoredFeeds = new Counter();
	
	/** total time spent for searching */
	private long searchTime = 0;
	/** total time spent for discovery process */
	private long discoveryTime = 0;
	
	/** traffic counter */
	private Counter traffic = new Counter();
	
	@SuppressWarnings("unchecked")
	public FeedDiscovery() {
		logger.trace(">FeedDiscovery");
		try {
			PropertiesConfiguration config = new PropertiesConfiguration("config/feeds.conf");
			setMaxThreads(config.getInt("maxDiscoveryThreads", MAX_NUMBER_OF_THREADS));
			setIgnores(config.getList("discoveryIgnoreList"));
		} catch (ConfigurationException e) {
			logger.error("error loading configuration " + e.getMessage());
		}
		logger.trace("<FeedDiscovery");
	}

	/**
	 * Add a query for the search engine.
	 * @param query
	 */
	public void addQuery(String query) {
		logger.trace(">addQuery " + query);
		Set<String> foundSites = searchSites(query, resultLimit);
		sites.addAll(foundSites);
		logger.trace("<addQuery");
	}
	
	/**
	 * Add queries for the search engine.
	 * @param queries
	 */
	public void addQueries(Collection<String> queries) {
		logger.trace(">addQueries");
		for (String query : queries) {
			this.addQuery(query);
		}
		logger.trace("<addQueries");
	}

	/**
	 * Search for Sites by specified query.
	 * 
	 * @param query
	 * @param totalResults
	 * @return
	 */
	private Set<String> searchSites(String query, int totalResults) {
		logger.trace(">searchSites " + query + " " + totalResults);
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		logger.info("querying for " + query + " num results " + totalResults);

		// create source retriever object
		SourceRetriever sourceRetriever = new SourceRetriever();

		// set maximum number of expected results
		sourceRetriever.setResultCount(totalResults);

		// set search result language to english
		sourceRetriever.setLanguage(SourceRetriever.LANGUAGE_ENGLISH);

		// set the query source to the Bing search engine
		//sourceRetriever.setSource(SourceRetrieverManager.GOOGLE_BLOGS);
		sourceRetriever.setSource(SourceRetrieverManager.YAHOO_BOSS);

		// search for "Jim Carrey" in exact match mode (second parameter = true)
		ArrayList<String> resultURLs = sourceRetriever.getURLs(query, true);

		// print the results
		// CollectionHelper.print(resultURLs);

		Set<String> sites = new HashSet<String>();
		for (String resultUrl : resultURLs) {
			sites.add(Helper.getRootUrl(resultUrl));
		}
		
		stopWatch.stop();
		searchTime += stopWatch.getElapsedTime();
		
		logger.trace("<searchSites");
		return sites;
	}

	/**
	 * Discovers feed links in supplied page URL.
	 * 
	 * @param pageUrl
	 * @return list of discovered feed URLs, empty list if no feeds are
	 *         available, <code>null</code> if page could not be parsed.
	 */
	List<String> getFeedsViaAutodiscovery(String pageUrl) {
		
		logger.trace(">getFeedsViaAutodiscovery " + pageUrl);
		
		//logger.debug("-------------");
		//logger.debug("pageUrl: " + pageUrl);
		
		List<String> result = null;

		try {
			
			Crawler crawler = new Crawler();

			Document doc = crawler.getWebDocument(pageUrl, false);
			result = getFeedsViaAutodiscovery(doc);
			traffic.increment((int) crawler.getTotalDownloadSize());

		} catch (Throwable t) {
			// NekoHTML produces various types of Exceptions, just catch them
			// all here and log them.
			logger.error("error at " + pageUrl + " " + t.toString() + " : " + t.getMessage());
			errors.increment();
		}
		
		//logger.debug("-------------");
		logger.trace("<getFeedsViaAutodiscovery");

		return result;
	}

	/**
	 * Uses autodiscovery feature with MIME types "application/atom+xml" and
	 * "application/rss+xml" to find linked feeds in the specified Document. If
	 * Atom and RSS feeds are present, we prefer Atom :)
	 * 
	 * @param document
	 * @return list of discovered feed URLs or empty list.
	 */
	List<String> getFeedsViaAutodiscovery(Document document) {
		
		List<String> result = new LinkedList<String>();
		
		totalSites.increment();
			
		String pageUrl = document.getDocumentURI();
		// fix for testing purposes
		if (!pageUrl.startsWith("http://") && !pageUrl.startsWith("https://") && !pageUrl.startsWith("file:")) {
			pageUrl = "file:"+pageUrl;
		}
		
		//////// for debugging
		if (debugDump) {
			Helper.writeXmlDump(document, "dumps/"+pageUrl.replaceAll("https?://", "")+".xml");
		}

		Node baseNode = XPathHelper.getNode(document, "//HEAD/BASE/@href");
		String baseHref = null;
		if (baseNode != null) {
			 baseHref = baseNode.getTextContent();
		}
		
		
		//
		// TODO maybe we should think this over again ... according to ...:
		// http://diveintomark.org/archives/2003/12/19/atom-autodiscovery
		// a) attributes contents are case insensitive, e.g. 'ApPlIkAtIoN/AtOm+xMl' is valid
		// b) if multiple feeds are present, the first one indicates the 'prefered' feed
		//
		List<Node> atomNodes = XPathHelper.getNodes(document, "//LINK[@rel='alternate' and @type='application/atom+xml']/@href");
		List<Node> rssNodes = XPathHelper.getNodes(document, "//LINK[@rel='alternate' and @type='application/rss+xml']/@href");
		
		boolean hasAtom = atomNodes.size() > 0;
		if (hasAtom) {
			atomFeeds.increment();
		}
		boolean hasRss = rssNodes.size() > 0;
		if (hasRss) {
			rssFeeds.increment();
		}
		
		List<Node> feedNodes = null;
		if (hasAtom) {
			logger.trace("taking Atom feed");
			feedNodes = atomNodes;
		} 
		else if (hasRss) {
			logger.trace("taking RSS feed");
			feedNodes = rssNodes;
		} 
		else {
			logger.trace("no feed available");
		}
		
		if (feedNodes != null) {
			int numFeeds = feedNodes.size();
			feedSites.increment();
			if (numFeeds > 1) {
				multipleFeeds.increment();
				logger.trace("found multiple feeds");
			}
			for (Node feedNode : feedNodes) {
				
				String feedHref = feedNode.getNodeValue();

				// there are actualy some pages with an empty href attribute! so ignore them here.
				if (feedHref == null || feedHref.length() == 0) {
					continue;
				}
				
				// few sites use a "Feed URI Scheme" which can look like
				// feed://example.com/entries.atom
				// feed:https://example.com/entries.atom
				// See ---> http://en.wikipedia.org/wiki/Feed_URI_scheme
				feedHref = feedHref.replace("feed://", "http://");
				feedHref = feedHref.replace("feed:", "");
				
				// make full URL
				String feedUrl = Helper.getFullUrl(pageUrl, baseHref, feedHref);
				
				// validate URL
				// there are few URLs where validation does not work correctly,
				// so we skip the validation for now, as the aggregator will encounter
				// an error anyway when fed with an incorrect URL ...
				//UrlValidator urlValidator = new UrlValidator(new String[]{"http","https"});
				//boolean isValidUrl = urlValidator.isValid(feedHref);
				
				//if (feedHref != null && feedHref.length() > 0 && isValidUrl) {
				//if (isValidUrl) {
					logger.debug("found feed: "+feedUrl);
					if (!isIgnored(feedUrl)) {
						result.add(feedUrl);
					} else {
						logger.info("ignoring " + feedUrl);
						ignoredFeeds.increment();
					}
				//}
			}
			// some statistical information
			logger.info(pageUrl + " has atom:"+hasAtom + ", rss:"+hasRss + ", count:"+numFeeds);
		} else {
			logger.info(pageUrl + " has no feed");
		}
		
		return result;
		
	}


	/**
	 * Find feeds in all pages on the sites tack. We use threading here which
	 * yields in much faster results.
	 */
	public void findFeeds() {
		
		logger.trace(">findFeeds");
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		// to count number of running Threads 
		final Counter counter = new Counter();
		
		while (sites.size() > 0) {	
			final String site = getSiteFromStack();
			
			// if maximum # of Threads are already running, wait here
			while (counter.getCount() >= maxThreads) {
				logger.trace("max # of Threads running. waiting ...");
				ThreadHelper.sleep(1000);
			}
						
			counter.increment();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						List<String> discoveredFeeds = getFeedsViaAutodiscovery(site);
						if (discoveredFeeds != null) {
							feeds.addAll(discoveredFeeds);
						}
					/*} catch (Exception e) {
					//	logger.error("exception " + e.getMessage());*/
					} finally {
						counter.decrement();
					}
				}
			};
			new Thread(runnable).start();
		}
		
		// keep on running until all Threads have finished and
		// the Stack is empty 
		while (counter.getCount() > 0 || sites.size() > 0) {
			ThreadHelper.sleep(1000);
			logger.trace("waiting ... threads:" + counter.getCount() + " stack:" + sites.size());
		}
		logger.trace("done");
		
		stopWatch.stop();
		discoveryTime += stopWatch.getElapsedTime();
		
		logger.debug("found " + feeds.size() + " feeds");
		logger.trace("<findFeeds");
	}
	
	/**
	 * Returns URLs of discovered feeds.
	 * @return
	 */
	public Collection<String> getFeeds() {
		logger.trace(">getFeeds");
		logger.trace("<getFeeds " + feeds.size());
		return feeds;
	}
	
	/**
	 * Dump all XML files.
	 * @param debugDump
	 */
	public void setDebugDump(boolean debugDump) {
		this.debugDump = debugDump;
	}
	
	/**
	 * Set max number of concurrent autodiscovery requests.
	 * @param maxThreads
	 */
	public void setMaxThreads(int maxThreads) {
		this.maxThreads  = maxThreads;
	}

	/**
	 * Limit the number of results for each query.
	 * 
	 * @param resultLimit
	 *            The number of websites to query. This does not neccesarily
	 *            mean that we get totalResults of feeds per query, as some
	 *            sites do not offer a feed and some offer multiple feeds.
	 */
	public void setResultLimit(int resultLimit) {
		this.resultLimit = resultLimit;
	}

	/**
	 * Add to entry ignore list. Any feed url containing this string will be
	 * ignored.
	 * 
	 * @param ignore
	 * @return
	 */
	public boolean addIgnore(String ignore) {
		return this.ignoreList.add(ignore);
	}
	
	public void setIgnores(Collection<String> ignores) {
		logger.trace("setting ignores to " + ignores);
		this.ignoreList = new HashSet<String>(ignores);
	}
	
	/**
	 * Returns some statistics about the dicovery process.
	 * @return
	 */
	public String getStatistics() {
		StringBuilder sb = new StringBuilder();
		String newLine = System.getProperty("line.separator");
		sb.append("----------------------------------------------").append(newLine);
		sb.append("    # sites checked:   " + totalSites).append(newLine);
		sb.append("    # sites with feed: " + feedSites).append(newLine);
		sb.append("             # atom: " + atomFeeds).append(newLine);
		sb.append("             # rss:  " + rssFeeds).append(newLine);
		sb.append("    # sites with multiple feeds: " + multipleFeeds).append(newLine);
		sb.append("    # ignored feeds: " + ignoredFeeds).append(newLine);
		sb.append("    # errors: " + errors).append(newLine).append(newLine);
		if (searchTime != 0) {
			sb.append("    total time for searching: " + DateHelper.getTimeString(searchTime)).append(newLine);
		}
		if (discoveryTime != 0) {
			sb.append("    total time for discovery: " + DateHelper.getTimeString(discoveryTime)).append(newLine).append(newLine);
		}
		if (traffic.getCount() != 0) {
			sb.append("    traffic for discovery: " + ((float) traffic.getCount()/(1024*1024)) + " MB").append(newLine);
		}
		sb.append("----------------------------------------------");
		return sb.toString();
	}
	
	private synchronized String getSiteFromStack() {
		String result = null;
		if (sites.iterator().hasNext()) {
			result = sites.iterator().next();
			sites.remove(result);
		}
		return result;
	}
	
	private boolean isIgnored(String feedUrl) {
		for (String ignore : ignoreList) {
			if (feedUrl.contains(ignore)) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		
		FeedDiscovery discovery = new FeedDiscovery();
		String resultOpml = null;
		
		int i = 0;
		while (i < args.length && args[i].startsWith("-")) {
			String arg = args[i++];
			if (arg.equals("-resultLimit")) {
				if (i < args.length) {
					discovery.setResultLimit(Integer.valueOf(args[i++]));
				} else {
					System.err.println("-resultLimit requires # of max. results/query");
				}
			}
			else if (arg.equals("-threads")) {
				if (i < args.length) {
					discovery.setMaxThreads(Integer.valueOf(args[i++]));
				} else {
					System.err.println("-threads requires # of threads");
				}
			}
			else if (arg.equals("-dump")) {
				discovery.setDebugDump(true);
			}
			else if (arg.equals("-output")) {
				if (i < args.length) {
					resultOpml = args[i++];
				} else {
					System.err.println("-output requires a filename");
				}
			}
		}
		if (i == args.length) {
			System.err.println("CLI usage: FeedDiscovery [-resultLimit nn] [-threads nn] [-dump] [-output feeds.opml] query1[,query2,...]");
			System.err.println("Example:   FeedDiscovery -resultLimit 1000 -output temp/cities.opml New+York,London,Paris,Tokio,Hongkong");
			return;
		}
		
		// split to individual query strings, replace + with spaces
		List<String> queries = Arrays.asList(args[i].replace("+"," ").split(","));
		discovery.addQueries(queries);
		
		discovery.findFeeds();
		
		Collection<String> discoveredFeeds = discovery.getFeeds();
		CollectionHelper.print(discoveredFeeds);
		System.out.println(discovery.getStatistics());
		
		// write result to OPML file
		if (resultOpml != null) {
			OPMLHelper.writeOPMLFileFromStrings(discoveredFeeds, new File(resultOpml));
			System.out.println("wrote result to " + resultOpml);
		}
	}


	
	



}
