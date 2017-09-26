package ch.ethz.matsim.r5.scoring;

import com.conveyal.r5.api.util.Itinerary;

/**
 * Scores an R5 transit route
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public interface R5ItineraryScorer {
	/**
	 * Returns the score of a route
	 * 
	 * @param itinerary from R5
	 */
	double scoreItinerary(Itinerary itinerary);
}
