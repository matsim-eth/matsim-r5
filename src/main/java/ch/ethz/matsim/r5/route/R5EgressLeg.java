package ch.ethz.matsim.r5.route;

import ch.ethz.matsim.r5.utils.spatial.LatLon;

public class R5EgressLeg extends AbstractR5Leg {
	private String departureStopId;

	public R5EgressLeg(double departureTime, double travelTime, double distance, LatLon departureLocation,
			LatLon arrivalLocation, String departureStopId) {
		super(departureTime, travelTime, distance, departureLocation, arrivalLocation);
		this.departureStopId = departureStopId;
	}

	public String getDepartureStopId() {
		return departureStopId;
	}
}
