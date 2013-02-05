package ws.palladian.extraction.location;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;

/**
 * A simple, in-memory location source.
 * 
 * @author Philipp Katz
 */
public class CollectionLocationSource implements LocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionLocationSource.class);

    private final Map<Integer, Location> locationsIds;
    private final MultiMap<String, Location> locationsNames;
    private final MultiMap<Integer, Integer> hierarchy; // XXX not sure, if a normal map wouldn't be sufficient here

    public CollectionLocationSource() {
        locationsIds = CollectionHelper.newHashMap();
        locationsNames = MultiMap.create();
        hierarchy = MultiMap.create();
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        return locationsNames.get(locationName.toLowerCase());
    }

    @Override
    public void save(Location location) {
        locationsNames.add(location.getPrimaryName().toLowerCase(), location);
        for (String alternativeName : location.getAlternativeNames()) {
            locationsNames.add(alternativeName.toLowerCase(), location);
        }
        locationsIds.put(location.getId(), location);
    }

    @Override
    public void addHierarchy(int childId, int parentId, String type) {
        if (childId == parentId) {
            throw new IllegalArgumentException("A child cannot be the parent of itself (id was " + childId + ")");
        }
        hierarchy.add(childId, parentId);
    }

    @Override
    public Location retrieveLocation(int locationId) {
        return locationsIds.get(locationId);
    }

    @Override
    public List<Location> getHierarchy(Location location) {
        List<Location> ret = CollectionHelper.newArrayList();
        Location currentLocation = location;
        for (;;) {
            currentLocation = getParentLocation(currentLocation);
            if (currentLocation == null) {
                break;
            }
            ret.add(currentLocation);
        }
        return ret;
    }

    private Location getParentLocation(Location location) {
        List<Integer> parentIds = hierarchy.get(location.getId());
        if (parentIds == null) {
            LOGGER.trace("No parent for {}", location.getId());
            return null;
        }
        if (parentIds.size() > 1) {
            LOGGER.warn("Multiple parent for {}", location.getId());
        }
        return locationsIds.get(parentIds.get(0));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CollectionLocationSource [#locationsIds=");
        builder.append(locationsIds.size());
        builder.append(", #locationsNames=");
        builder.append(locationsNames.size());
        builder.append(", #hierarchy=");
        builder.append(hierarchy.size());
        builder.append("]");
        return builder.toString();
    }

}
