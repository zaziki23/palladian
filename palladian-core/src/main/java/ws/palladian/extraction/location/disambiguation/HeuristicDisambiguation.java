package ws.palladian.extraction.location.disambiguation;

import static ws.palladian.extraction.location.LocationType.CITY;
import static ws.palladian.extraction.location.LocationType.CONTINENT;
import static ws.palladian.extraction.location.LocationType.COUNTRY;
import static ws.palladian.extraction.location.LocationType.UNIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationExtractorUtils.CoordinateFilter;
import ws.palladian.extraction.location.LocationExtractorUtils.LocationTypeFilter;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * Heuristic disambiguation strategy based on anchor locations, and proximities.
 * </p>
 * 
 * @author Philipp Katz
 */
public class HeuristicDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HeuristicDisambiguation.class);

    public static final int ANCHOR_DISTANCE_THRESHOLD = 150;

    public static final int LOWER_POPULATION_THRESHOLD = 5000;

    public static final int ANCHOR_POPULATION_THRESHOLD = 1000000;

    public static final int SAME_DISTANCE_THRESHOLD = 50;

    /** Maximum distance for anchoring. */
    private final int anchorDistanceThreshold;

    /** Minimum population for anchoring. */
    private final int lowerPopulationThreshold;

    /** Minimum population for a location to become anchor. */
    private final int anchorPopulationThreshold;

    /** Maximum distance between two locations with equal name, to assume they are the same. */
    private final int sameDistanceThreshold;

    public HeuristicDisambiguation() {
        this(ANCHOR_DISTANCE_THRESHOLD, LOWER_POPULATION_THRESHOLD, ANCHOR_POPULATION_THRESHOLD,
                SAME_DISTANCE_THRESHOLD);
    }

    /**
     * <p>
     * Create a new {@link HeuristicDisambiguation} with the specified settings.
     * </p>
     * 
     * @param anchorDistanceThreshold The maximum distance for a location to be "catched" by an anchor.
     * @param lowerPopulationThreshold The minimum population threshold to be "catched " as child of an anchor.
     * @param anchorPopulationThreshold The minimum population threshold for a location to become an anchor.
     * @param sameDistanceThreshold The maximum distance between two locations with the same names to assume, that they
     *            are actually the same.
     */
    public HeuristicDisambiguation(int anchorDistanceThreshold, int lowerPopulationThreshold,
            int anchorPopulationThreshold, int sameDistanceThreshold) {
        this.anchorDistanceThreshold = anchorDistanceThreshold;
        this.lowerPopulationThreshold = lowerPopulationThreshold;
        this.anchorPopulationThreshold = anchorPopulationThreshold;
        this.sameDistanceThreshold = sameDistanceThreshold;
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<Annotated, Location> locations) {

        List<LocationAnnotation> result = CollectionHelper.newArrayList();

        Set<Location> anchors = getAnchors(locations);

        for (Annotated annotation : locations.keySet()) {
            Collection<Location> candidates = locations.get(annotation);
            if (candidates.isEmpty()) {
                LOGGER.debug("'{}' could not be found and will be dropped", annotation.getValue());
                continue;
            }

            LOGGER.debug("'{}' has {} candidates", annotation.getValue(), candidates.size());

            // for distance checks below, only consider anchor locations, which are not in the current candidate set
            Collection<Location> currentAnchors = new HashSet<Location>(anchors);
            currentAnchors.removeAll(candidates);

            Set<Location> preselection = CollectionHelper.newHashSet();

            for (Location candidate : candidates) {
                if (anchors.contains(candidate)) {
                    LOGGER.debug("{} is in anchors", candidate);
                    preselection.add(candidate);
                    continue;
                }
                for (Location anchor : currentAnchors) {
                    double distance = GeoUtils.getDistance(candidate, anchor);
                    LocationType anchorType = anchor.getType();
                    if (distance < anchorDistanceThreshold) {
                        LOGGER.debug("Distance of {} to anchors: {}", distance, candidate);
                        preselection.add(candidate);
                    } else if (anchorType == CITY || anchorType == UNIT || anchorType == COUNTRY) {
                        if (LocationExtractorUtils.isDescendantOf(candidate, anchor)
                                && candidate.getPopulation() > lowerPopulationThreshold) {
                            LOGGER.debug("{} is child of anchor '{}'", candidate, anchor.getPrimaryName());
                            preselection.add(candidate);
                        }
                    }
                }
            }
            if (preselection.size() > 0) {
                Location selection = selectLocation(preselection);
                result.add(new LocationAnnotation(annotation, selection));
            }
        }
        return result;
    }

    private static Location selectLocation(Collection<Location> selection) {

        // if we have a continent, take the continent
        Set<Location> result = LocationExtractorUtils.filterConditionally(selection, new LocationTypeFilter(CONTINENT));
        if (result.size() == 1) {
            return CollectionHelper.getFirst(result);
        }

        List<Location> temp = new ArrayList<Location>(selection);
        Collections.sort(temp, new Comparator<Location>() {
            @Override
            public int compare(Location l1, Location l2) {

                // if locations are nested, take the "deepest" one
                if (LocationExtractorUtils.isDescendantOf(l2, l1)) {
                    return 1;
                } else if (LocationExtractorUtils.isDescendantOf(l1, l2)) {
                    return -1;
                }

                // as last step, compare by population
                Long p1 = l1.getPopulation() != null ? l1.getPopulation() : 0;
                Long p2 = l2.getPopulation() != null ? l2.getPopulation() : 0;

                // XXX dirty hack; favor cities
                if (l1.getType() == CITY) {
                    p1 *= 2;
                }
                if (l2.getType() == CITY) {
                    p2 *= 2;
                }

                return p2.compareTo(p1);

            }
        });
        return CollectionHelper.getFirst(temp);
    }

    private Set<Location> getAnchors(MultiMap<Annotated, Location> locations) {
        Set<Location> anchorLocations = CollectionHelper.newHashSet();

        // get prominent anchor locations; continents, countries and locations with very high population
        for (Location location : locations.allValues()) {
            LocationType type = location.getType();
            long population = location.getPopulation() != null ? location.getPopulation() : 0;
            if (type == CONTINENT || type == COUNTRY || population > anchorPopulationThreshold) {
                LOGGER.debug("Prominent anchor location: {}", location);
                anchorLocations.add(location);
            }
        }

        // get unique and unambiguous locations; location whose name only occurs once, or which are very closely
        // together (because we might have multiple entries in the database with the same name which lie on a cluster)
        for (Annotated annotation : locations.keySet()) {
            Collection<Location> group = locations.get(annotation);
            String name = annotation.getValue();

            // in case we have locations with same name, but once with and without coordinates in the DB, we drop those
            // without coordinates
            group = LocationExtractorUtils.filterConditionally(group, new CoordinateFilter());

            if (LocationExtractorUtils.getLargestDistance(group) < sameDistanceThreshold) {
                Location location = LocationExtractorUtils.getBiggest(group);
                if (location.getPopulation() > lowerPopulationThreshold || name.split("\\s").length > 2) {
                    anchorLocations.add(location);
                }
            } else {
                LOGGER.debug("Ambiguous location: {} ({} candidates)", name, group.size());
            }
        }

        // if we could not get any anchor locations, just take the biggest one from the given candidates
        if (anchorLocations.isEmpty()) {
            Location biggest = LocationExtractorUtils.getBiggest(locations.allValues());
            if (biggest != null) {
                LOGGER.warn("No anchor found, took biggest location: {}", biggest);
                anchorLocations.add(biggest);
            } else {
                LOGGER.warn("No anchor found.");
            }
        }
        return anchorLocations;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HeuristicDisambiguation [anchorDistanceThreshold=");
        builder.append(anchorDistanceThreshold);
        builder.append(", lowerPopulationThreshold=");
        builder.append(lowerPopulationThreshold);
        builder.append(", anchorPopulationThreshold=");
        builder.append(anchorPopulationThreshold);
        builder.append(", sameDistanceThreshold=");
        builder.append(sameDistanceThreshold);
        builder.append("]");
        return builder.toString();
    }

}
