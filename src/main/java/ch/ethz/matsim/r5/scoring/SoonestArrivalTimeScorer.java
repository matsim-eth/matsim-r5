package ch.ethz.matsim.r5.scoring;

import java.time.temporal.ChronoField;

import com.conveyal.r5.api.util.Itinerary;

public class SoonestArrivalTimeScorer implements R5ItineraryScorer {
	@Override
	public double scoreItinerary(Itinerary itinerary) {
		return -itinerary.endTime.get(ChronoField.SECOND_OF_DAY);
	}
}
