package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.UrlTagger;
import ws.palladian.helper.collection.CollectionHelper;

public class UrlTaggerTest {

	
    // TODO these tests should be performed in UrlHelperTest,
    // as UrlTagger is only a wrapper around the RegExp providing additional position information
	@Test
	public void testUrlTagging() {
		UrlTagger urlTagger = new UrlTagger();
        Annotations annotations = null;
		
        annotations = urlTagger
                .tagUrls("You can download it here: cinefreaks.com/coolstuff.zip but be aware of the size.");
        System.out.println(annotations);
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getOffset());
        assertEquals(28, annotations.get(0).getLength());
        
        annotations = urlTagger
                .tagUrls("You can download it here: 1-2-3.net/auctions-Are-out.jpg but be aware of the size.");
        System.out.println(annotations);
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getOffset());
        assertEquals(30, annotations.get(0).getLength());

		annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com/coolstuff.zip but be aware of the size.");
		assertEquals(1, annotations.size());
		assertEquals(26, annotations.get(0).getOffset());
		assertEquals(39, annotations.get(0).getLength());
		
		annotations = urlTagger.tagUrls("You can download it here: www.cinefreaks.com/coolstuff.zip but be aware of the size.");
		assertEquals(1, annotations.size());
		assertEquals(26, annotations.get(0).getOffset());
		assertEquals(32, annotations.get(0).getLength());
		
		annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com/");
		assertEquals(1, annotations.size());
		assertEquals(26, annotations.get(0).getOffset());
		assertEquals(26, annotations.get(0).getLength());
		
        annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com.");
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getOffset());
        assertEquals(25, annotations.get(0).getLength());

        annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com?");
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getOffset());
        assertEquals(25, annotations.get(0).getLength());

        annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com! Or somewhere else");
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getOffset());
        assertEquals(25, annotations.get(0).getLength());

        annotations = urlTagger
                .tagUrls("You can download it here: http://www.cinefreaks.com. This is the next sentence");
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getOffset());
        assertEquals(25, annotations.get(0).getLength());

        annotations = urlTagger.tagUrls("You can download it here: http://www.cinefreaks.com, this is the next...");
        assertEquals(1, annotations.size());
        assertEquals(26, annotations.get(0).getOffset());
        assertEquals(25, annotations.get(0).getLength());

		annotations = urlTagger.tagUrls("http://www.google.cpm/search?tbm=isch&hl=en&source=hp&biw=1660&bih=751&q=alfred+neuman+mad+magazine&gbv=2&aq=1s&aqi=g1g-s1g-sx1&aql=&oq=alfred+newman+m");
		CollectionHelper.print(annotations);
		assertEquals(1, annotations.size());
		assertEquals(0, annotations.get(0).getOffset());
		assertEquals(151, annotations.get(0).getLength());

		
	}
}
