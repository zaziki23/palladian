package ws.palladian.extraction.location;

import org.junit.Test;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ws.palladian.extraction.location.LocationType.CITY;
import static ws.palladian.extraction.location.LocationType.UNIT;

public class LocationExtractorUtilsTest {

    private final Location l4 = new ImmutableLocation(4653031, "Richmond", CITY, GeoCoordinate.from(35.38563, -86.59194), 0l);
    private final Location l5 = new ImmutableLocation(4074277, "Madison County", UNIT, GeoCoordinate.from(34.73342, -86.56666), 0l);
    private final Location l6 = new ImmutableLocation(100080784, "Madison County", UNIT, GeoCoordinate.from(34.76583, -86.55778), null);

    @Test
    public void testDifferentNames() {
        assertTrue(LocationExtractorUtils.differentNames(Arrays.asList(l4, l5, l6)));
        assertFalse(LocationExtractorUtils.differentNames(Arrays.asList(l5, l6)));
    }

}
