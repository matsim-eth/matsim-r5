package ch.ethz.matsim.r5.route;

import ch.ethz.matsim.r5.utils.spatial.LatLon;

public interface R5Leg {
	double getDepartureTime();
	double getArrivalTime();
	double getTravelTime();
	
	double getDistance();
	
	LatLon getDepartureLocation();
	LatLon getArrivalLocation();
}
