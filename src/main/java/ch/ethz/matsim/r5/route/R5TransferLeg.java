package ch.ethz.matsim.r5.route;

import ch.ethz.matsim.r5.utils.spatial.LatLon;

public class R5TransferLeg extends AbstractR5TransitLeg {
	private LatLon arrivalLocation = null;
	private String arrivalStopId = null;
	
	public R5TransferLeg(double departureTime, double travelTime, double distance, LatLon departureLocation,
			LatLon arrivalLocation, String departureStopId, String arrivalStopId) {
		super(departureTime, travelTime, distance, departureLocation, arrivalLocation, departureStopId, arrivalStopId);
	}
	
	public R5TransferLeg(double departureTime, double travelTime, double distance, LatLon departureLocation, String departureStopId) {
		super(departureTime, travelTime, distance, departureLocation, null, departureStopId, null);
	}
	
	@Override
	public LatLon getArrivalLocation() {
		return arrivalLocation;
	}
	
	@Override
	public String getArrivalStopId() {
		return arrivalStopId;
	}
	
	public void setArrival(LatLon arrivalLocation, String arrivalStopId) {
		this.arrivalLocation = arrivalLocation;
		this.arrivalStopId = arrivalStopId;
	}
}
