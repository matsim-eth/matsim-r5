package ch.ethz.matsim.r5.route;

import ch.ethz.matsim.r5.utils.spatial.LatLon;

public class AbstractR5Leg implements R5Leg {
	private double departureTime;;
	private double travelTime;;
	private double distance;
	
	private LatLon departureLocation;
	private LatLon arrivalLocation;
	
	public AbstractR5Leg(double departureTime, double travelTime, double distance, LatLon departureLocation, LatLon arrivalLocation) {
		this.departureTime = departureTime;
		this.travelTime = travelTime;
		this.distance = distance;
		this.departureLocation = departureLocation;
		this.arrivalLocation = arrivalLocation;
	}
	
	@Override
	public double getDepartureTime() {
		return departureTime;
	}

	@Override
	public double getArrivalTime() {
		return departureTime + travelTime;
	}

	@Override
	public double getTravelTime() {
		return travelTime;
	}

	@Override
	public double getDistance() {
		return distance;
	}

	@Override
	public LatLon getDepartureLocation() {
		return departureLocation;
	}

	@Override
	public LatLon getArrivalLocation() {
		return arrivalLocation;
	}
}
