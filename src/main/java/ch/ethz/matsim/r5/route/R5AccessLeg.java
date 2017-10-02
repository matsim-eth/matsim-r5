package ch.ethz.matsim.r5.route;

import ch.ethz.matsim.r5.utils.spatial.LatLon;

public class R5AccessLeg extends AbstractR5Leg {
	private String arrivalStopId;

	public R5AccessLeg(double departureTime, double travelTime, double distance, LatLon departureLocation,
			LatLon arrivalLocation, String arrivalStopId) {
		super(departureTime, travelTime, distance, departureLocation, arrivalLocation);
		this.arrivalStopId = arrivalStopId;
	}

	public String getArrivalStopId() {
		return arrivalStopId;
	}
}
