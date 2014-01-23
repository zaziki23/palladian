package ws.palladian.extraction.location.scope;

import static ws.palladian.extraction.location.LocationExtractorUtils.ANNOTATION_LOCATION_FUNCTION;
import static ws.palladian.extraction.location.LocationExtractorUtils.COORDINATE_FILTER;
import static ws.palladian.extraction.location.LocationType.CONTINENT;
import static ws.palladian.extraction.location.LocationType.COUNTRY;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.helper.collection.CollectionHelper;

public final class HighestPopulationScopeDetector implements ScopeDetector {

    private static final String NAME = "MaximumPopulation";

    @Override
    public Location getScope(Collection<LocationAnnotation> locations) {
        Validate.notNull(locations, "locations must not be null");
        Set<Location> locationSet = CollectionHelper.convertSet(locations, ANNOTATION_LOCATION_FUNCTION);
        CollectionHelper.removeNulls(locationSet);
        CollectionHelper.remove(locationSet, COORDINATE_FILTER);

        long maximumPopulation = 0;
        Location selectedLocation = null;
        for (Location location : locationSet) {
            if (location.getType() == CONTINENT || location.getType() == COUNTRY) {
                return location;
            }
            Long population = location.getPopulation();
            if (population != null && population > maximumPopulation) {
                maximumPopulation = population;
                selectedLocation = location;
            }
        }
        return selectedLocation;
    }

    @Override
    public String toString() {
        return NAME;
    }

}