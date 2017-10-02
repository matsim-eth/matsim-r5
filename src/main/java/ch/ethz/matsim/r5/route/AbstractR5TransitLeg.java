package ch.ethz.matsim.r5.route;

import ch.ethz.matsim.r5.utils.spatial.LatLon;

public class AbstractR5TransitLeg extends AbstractR5Leg {
	private String departureStopId;
	private String arrivalStopId;

	public AbstractR5TransitLeg(double departureTime, double travelTime, double distance, LatLon departureLocation,
			LatLon arrivalLocation, String departureStopId, String arrivalStopId) {
		super(departureTime, travelTime, distance, departureLocation, arrivalLocation);
		this.departureStopId = departureStopId;
		this.arrivalStopId = arrivalStopId;
	}

	public String getArrivalStopId() {
		return arrivalStopId;
	}
	
	public String getDepartureStopId() {
		return departureStopId;
	}
}
