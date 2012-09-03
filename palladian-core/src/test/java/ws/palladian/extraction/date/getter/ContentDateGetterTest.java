package ws.palladian.extraction.date.getter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

public class ContentDateGetterTest {

    private final DocumentParser htmlParser = ParserFactory.createHtmlParser();

    @Test
    public void testGetContentDate() throws ParserException, FileNotFoundException {
        Document document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit1.htm"));
        ContentDateGetter contentDateGetter = new ContentDateGetter();
        List<ContentDate> dates = contentDateGetter.getDates(document);
        assertEquals(6, dates.size());
        assertEquals("2010-08-22", dates.get(0).getNormalizedDateString());
        assertEquals("2010-08-22", dates.get(1).getNormalizedDateString());
        assertEquals("2010-08-22", dates.get(2).getNormalizedDateString());
        assertEquals("2010-08-22", dates.get(3).getNormalizedDateString());
        assertEquals("2010-08-22", dates.get(4).getNormalizedDateString());
        assertEquals("2010-08", dates.get(5).getNormalizedDateString());

        document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit2.htm"));
        dates = contentDateGetter.getDates(document);
        assertEquals(2, dates.size());
        assertEquals("2010-09-03", dates.get(0).getNormalizedDateString());
        assertEquals("2010-09-02", dates.get(1).getNormalizedDateString());

    }

    @Test
    public void testGetFindAllDatesTime() throws FileNotFoundException {
        String text = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/text01.txt"));
        List<ContentDate> dates = ContentDateGetter.findAllDates(text);
        assertEquals(142, dates.size());

        Set<String> stringPos = CollectionHelper.newHashSet();
        for (ContentDate date : dates) {
            stringPos.add(date.getDateString() + date.get(ContentDate.DATEPOS_IN_DOC));
        }
        assertEquals(119, stringPos.size());
    }

    @Test
    public void testGetContentDate2() throws Exception {
        File testPage = ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit3.html");
        Document document = htmlParser.parse(testPage);
        document.setDocumentURI("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu");

        ContentDateGetter contentDateGetter = new ContentDateGetter();
        List<ContentDate> dates = contentDateGetter.getDates(document);

        assertEquals(17, dates.size());

        ContentDate date = dates.get(0);
        assertEquals("0-01-02", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(-1, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(4574, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0.261, date.getRelDocPos(), 0.0001);
        assertEquals(0.5, date.getOrdDocPos(), 0.0001);
        assertEquals(1.0, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("0", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.059, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(12749, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(-1, date.getDistAgeAfter(), 0.0001);
        assertEquals("0", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("0", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("span", date.getTag());
        assertNull(null, date.getKeyword());

        date = dates.get(1);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.176, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(24, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(2);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.235, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(3);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.294, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(4);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.353, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(5);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.412, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(6);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.471, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(7);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.529, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(8);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.588, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(9);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.647, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(10);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.706, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(11);
        assertEquals("2010-07-20", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.059, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.118, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(-1, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(12);
        assertEquals("2010-07-20", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.118, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.118, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(24, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(13);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.765, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(14);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.824, date.getOrdAgePos(), 0.0001);
        assertEquals(0.824, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
        assertEquals("date", date.getKeyword());

        date = dates.get(15);
        assertEquals("2010-07-19", date.getNormalizedDateString());
        assertEquals(1, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(-1, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(201, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(-1, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0, date.getRelDocPos(), 0.0001);
        assertEquals(0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.882, date.getOrdAgePos(), 0.0001);
        assertEquals("0", date.getKeyClass());
        assertEquals("1", date.getKeyLoc());
        assertEquals(0.0, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.765, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(-1, date.getDistPosBefore(), 0.0001);
        assertEquals(4575, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("1", date.getKeyLoc201());
        assertEquals("0", date.getKeyLoc202());
//        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
//        assertEquals("1", date.getIsKeyClass3());
//        assertEquals("2010-07-19", date.getStructureDate().getNormalizedDateString());
        assertTrue(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertFalse(date.isInUrl());
        assertEquals("li", date.getTag());
//        assertEquals("date", date.getKeyword());

        date = dates.get(16);
        assertEquals("2010-07", date.getNormalizedDateString());
        assertEquals(0, date.get(ContentDate.DATEPOS_IN_TAGTEXT));
        assertEquals(16, date.get(ContentDate.DISTANCE_DATE_KEYWORD));
        assertEquals(202, date.get(ContentDate.KEYWORDLOCATION));
        assertEquals(17323, date.get(ContentDate.DATEPOS_IN_DOC));
        assertEquals(0.99, date.getRelDocPos(), 0.0001);
        assertEquals(1.0, date.getOrdDocPos(), 0.0001);
        assertEquals(0.941, date.getOrdAgePos(), 0.0001);
        assertEquals("2", date.getKeyLoc());
        assertEquals(0.46699999999999997, date.getKeyDiff(), 0.0001);
        assertEquals("0", date.getSimpleTag());
        assertEquals("0", date.getHTag());
        assertEquals(0.059, date.getRelCntSame(), 0.0001);
        assertEquals(0.058823529411764705, date.getRelSize(), 0.0001);
        assertEquals(12749, date.getDistPosBefore(), 0.0001);
        assertEquals(-1, date.getDistPosAfter(), 0.0001);
        assertEquals(0, date.getDistAgeBefore(), 0.0001);
        assertEquals(0, date.getDistAgeAfter(), 0.0001);
        assertEquals("0", date.getKeyLoc201());
        assertEquals("1", date.getKeyLoc202());
        assertEquals("0", date.getIsKeyClass1());
        assertEquals("0", date.getIsKeyClass2());
        assertEquals("1", date.getIsKeyClass3());
//        assertNull(date.getStructureDate());
        assertFalse(date.hasStructureDate());
        assertFalse(date.isInMetaDates());
        assertTrue(date.isInUrl());
        assertEquals("span", date.getTag());
        assertEquals("date", date.getKeyword());

    }

}
