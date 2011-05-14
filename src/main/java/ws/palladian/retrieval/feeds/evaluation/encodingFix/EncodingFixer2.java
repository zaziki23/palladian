/**
 * 
 */
package ws.palladian.retrieval.feeds.evaluation.encodingFix;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;

/**
 * Quick'n'dirty
 * 
 * Required for CIKM feed dataset paper using TUDCS2 dataset. On one machine, the encoding has temporarily been changed,
 * so non-ASCII characters have been written as "?" to *.csv and *.gz files.
 * Detects duplicate items like
 * orig: "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
 * bad_: "?????hk????????????? ???? nputSemicolonHere??l?"
 * 
 * 
 * @author Sandro Reichert
 * 
 */
public class EncodingFixer2 extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EncodingFixer2.class);

    private Feed feed;

    /** all line numbers containing a MISS-line */
    private List<Integer> linesContainingMISS = new ArrayList<Integer>();

    private int windowSize = 0;

    private BoundedFifoBuffer windowBuffer = null;

    List<String[]> deduplicatedItems = new ArrayList<String[]>();

    public EncodingFixer2(Feed feed) {
        this.feed = feed;
    }

    @Override
    public void run() {
        try {

            String csvPath = "";
            // FIXME: debug hack
            if (feed == null) {
                feed = new Feed();
                csvPath = "data/datasets/feedPosts/411_http___19614155_at_webry_info_.csv";
            } else {
                // get path to csv, taken from DatasetCreator
                String safeFeedName = StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "")
                        .replaceFirst("www.", ""), 30);

                int slice = (int) Math.floor(feed.getId() / 1000.0);

                String folderPath = DatasetCreator.DATASET_PATH + slice + "/" + feed.getId() + "/";
                csvPath = folderPath + feed.getId() + "_" + safeFeedName + ".csv";
            }

            // read csv
            LOGGER.debug("processing: " + csvPath);
            if (!FileHelper.fileExists(csvPath)) {
                LOGGER.fatal("No csv file found for feed id " + feed.getId() + ", tried to get file " + csvPath
                        + ". Nothing to do for this feed.");
                return;
            }
            File originalCSV = new File(csvPath);
            List<String> items = FileHelper.readFileToArray(originalCSV);

            boolean feedContainsMiss = false;
            String[] missLine = null;
            List<String[]> splitItems = new ArrayList<String[]>();

            int lineCount = 0;
            for (String item : items) {
                lineCount++;
                String[] split = item.split(";");
                if (split[0].startsWith("MISS")) {
                    feedContainsMiss = true;
                    missLine = split;
                    linesContainingMISS.add(lineCount);
                } else if (windowSize != Integer.parseInt(split[5])) {
                    if (windowSize == 0) {
                        windowSize = Integer.parseInt(split[5]);
                    } else {
                        LOGGER.fatal("Window size not stable, ignoring this feed!");
                        return;
                    }
                }
                splitItems.add(split);
            }

            // buffer size = window size + 1 to store 1 MISS in buffer
            windowBuffer = new BoundedFifoBuffer(windowSize + 1);

            if (feedContainsMiss) {

                boolean recentLineWasMiss = false;
                // System.out.println(splitItems.size());
                for (int currentLineNr = splitItems.size(); currentLineNr >= 1; currentLineNr--) {
                    // System.out.println(line);
                    String[] currentItem = splitItems.get(currentLineNr - 1);

                    // in the beginning, store first window
                    if (windowBuffer.size() < windowSize) {
                        if (currentItem[0].equals("MISS") && windowBuffer.isEmpty()) {
                            LOGGER.debug("last line in csv was MISS - removed it.");
                            // we do not need to store this miss to recentLineWasMiss!
                            continue;
                        }
                        addToBuffer(currentItem);
                    } else {

                        // keep MISS in mind
                        if (currentItem[0].equals("MISS")) {
                            recentLineWasMiss = true;
                            continue;
                        }

                        // does buffer contain currentItem?
                        boolean encodingDuplicate = false;
                        Iterator iterator = windowBuffer.iterator();
                        while (iterator.hasNext()) {
                            String[] bufferdItem = (String[]) (iterator.next());
                            if (currentItem[0].equals(bufferdItem[0])
                                    && currentItem[1].length() == bufferdItem[1].length()
                                    && isDuplicate(currentItem[1], bufferdItem[1])
                                    && currentItem[2].equals(bufferdItem[2])) {
                                encodingDuplicate = true;
                                // we know, that the feed has been checked before, so the item in the buffer is the
                                // original and the current line is the encoding duplicate. Therefore, we do not
                                // put the duplicate to buffer but discard it.
                                LOGGER.debug("found duplicate lines in file " + csvPath + "\n" + "original:  "
                                        + restoreCSVString(bufferdItem) + " -> added\n" + "duplicate: "
                                        + restoreCSVString(currentItem));
                                break;
                            }
                        }
                        if (!encodingDuplicate) {
                            // remember the miss - since we did not found an encoding duplicate, it is a real miss
                            // and we need to write it to the buffer
                            if (recentLineWasMiss) {
                                addToBuffer(missLine);
                                recentLineWasMiss = false;
                            }
                            addToBuffer(currentItem);
                        } else if (recentLineWasMiss) {
                            recentLineWasMiss = false;
                        }
                    }

                }

                // get remaining Elements from buffer.
                Iterator iterator = windowBuffer.iterator();
                while (iterator.hasNext()) {
                    iterator.next();
                    String[] currentItem = (String[]) (windowBuffer.remove());
                    deduplicatedItems.add(currentItem);
                    LOGGER.trace("adding to deduplicated items: " + restoreCSVString(currentItem));
                }

                List<String> finalItems = new ArrayList<String>();

                LOGGER.trace("final List:");
                for (String[] currentItem : deduplicatedItems) {
                    finalItems.add(restoreCSVString(currentItem));
                    LOGGER.trace(restoreCSVString(currentItem));
                }

                // store new list if we found at least one duplicate (the new list another size than the original)
                if (finalItems.size() != items.size()) {
                    // reverse to get original order of csv
                    List<String> reversedItems = CollectionHelper.reverse(finalItems);

                    boolean backupOriginal = originalCSV.renameTo(new File(csvPath + ".bak"));
                    boolean newFileWritten = false;
                    if (backupOriginal) {
                        newFileWritten = FileHelper.writeToFile(csvPath, reversedItems);
                    }
                    if (backupOriginal && newFileWritten) {
                        LOGGER.info("Found misses for feed id " + feed.getId() + ", new file written to " + csvPath);
                    } else {
                        LOGGER.fatal("could not write output file, dumping to log:\n" + reversedItems);
                    }
                }

            } else {
                LOGGER.debug("Nothing to do for file " + csvPath);
            }
            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals.
        } catch (Throwable th) {
            LOGGER.error(th);
        }

    }

    private void addToBuffer(String[] itemToAdd) {
        // if full, store last element
        if (windowBuffer.size() == windowSize) {
            String[] leastRecentItem = (String[]) (windowBuffer.remove());
            deduplicatedItems.add(leastRecentItem);
            LOGGER.trace("adding to deduplicated items: " + restoreCSVString(leastRecentItem));
        }
        windowBuffer.add(itemToAdd);
        LOGGER.trace("adding to windowbuffer: " + restoreCSVString(itemToAdd));
    }

    private boolean isOriginal(String title) {
        // TODO Auto-generated method stub
        return !title.equals(StringHelper.removeNonAsciiCharacters(title));
    }

    /**
     * Checks whether there is a MISS-line between the two given lines
     * 
     * @param itemA
     * @param itemB
     * @return true if there is a MISS-line between the two given lines
     */
    private boolean missBetweenLines(int itemA, int itemB) {
        boolean missBetween = false;
        if (itemA != itemB) {
            int low = 0;
            int high = 0;
            if (itemA < itemB) {
                low = itemA;
                high = itemB;
            } else {
                low = itemB;
                high = itemA;
            }
            for (Integer missline : linesContainingMISS) {
                if (low < missline && high > missline) {
                    missBetween = true;
                    break;
                }
            }
        }
        return missBetween;
    }

    private String restoreCSVString(String[] toPrint) {
        String result = "";
        for (String part : toPrint) {
            result += part + ";";
        }
        return result.substring(0, result.length() - 1);
    }

    /**
     * Two titles are encoding duplicates if the strings are not equal but equal after removing non-ASCII chars. eg:
     * stringA = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
     * stringB = "?????hk????????????? ???? nputSemicolonHere??l?"
     * strings are encoding duplicates
     * 
     * stringC = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
     * stringD = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
     * strings are *not* encoding duplicates since they are real duplicates
     * 
     * @param stringA
     * @param stringB
     * @return
     */
    private static boolean isEncodingDuplicate(String stringA, String stringB) {
        boolean encodingDuplicate = false;
        String aStriped = StringHelper.removeNonAsciiCharacters(stringA.replace("?", "").replace("–", ""));
        String bStriped = StringHelper.removeNonAsciiCharacters(stringB.replace("?", "").replace("–", ""));
        encodingDuplicate = (!stringA.equals(stringB) && aStriped.equals(bStriped));
        return encodingDuplicate;
    }

    // detect encoding and normal duplicates
    private static boolean isDuplicate(String stringA, String stringB) {
        String aStriped = StringHelper.removeNonAsciiCharacters(stringA.replace("?", "").replace("–", ""));
        String bStriped = StringHelper.removeNonAsciiCharacters(stringB.replace("?", "").replace("–", ""));
        return aStriped.equals(bStriped);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        EncodingFixer2 fixer = new EncodingFixer2(null);
        fixer.run();

        // String boese = "?????hk????????????? ???? nputSemicolonHere??l?";
        // String gut = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹";
        // boolean equal = isTitleEqual(gut, boese);
        // System.out.println("bös: " + boese);
        // System.out.println("gut: " + gut);
        // System.out.println("gleich?: " + equal);

    }

}
