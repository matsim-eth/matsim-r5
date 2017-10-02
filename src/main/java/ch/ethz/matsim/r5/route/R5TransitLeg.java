package ch.ethz.matsim.r5.route;

import ch.ethz.matsim.r5.utils.spatial.LatLon;

public class R5TransitLeg extends AbstractR5TransitLeg {
	private int patternId;
	private int timeId;

	public R5TransitLeg(double departureTime, double travelTime, double distance, LatLon departureLocation,
			LatLon arrivalLocation, String departureStopId, String arrivalStopId, int patternId, int timeId) {
		super(departureTime, travelTime, distance, departureLocation, arrivalLocation, departureStopId, arrivalStopId);
		
		this.patternId = patternId;
		this.timeId = timeId;
	}
	
	public int getPatternId() {
		return patternId;
	}
	
	public int getTimeId() {
		return timeId;
	}
}
