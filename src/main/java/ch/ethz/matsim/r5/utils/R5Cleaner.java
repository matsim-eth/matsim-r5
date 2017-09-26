package ch.ethz.matsim.r5.utils;

import java.util.LinkedList;
import java.util.List;

import com.conveyal.r5.transit.RouteInfo;
import com.conveyal.r5.transit.TransportNetwork;

/**
 * Cleans the R5 network to be usable by MATSim
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class R5Cleaner {
	final private TransportNetwork transportNetwork;
	
	public R5Cleaner(TransportNetwork transportNetwork) {
		this.transportNetwork = transportNetwork;
	}
	
	/**
	 * Cleans the network for MATSim:
	 * 
	 * - E.g. Switzerland GTFS contains additional modes
	 * - Find the routes with those additional modes
	 * - Clear all schedules on those routes
	 */
	public void run() {
		List<Integer> routeIndices = new LinkedList<>();

		for (int i = 0; i < transportNetwork.transitLayer.routes.size(); i++) {
			RouteInfo route = transportNetwork.transitLayer.routes.get(i);

			if (route.route_type >= 1500) {
				route.route_type = 1200; // Needs to be valid to avoid exception
				routeIndices.add(i); // Save to deactivate associated patterns
			}
		}

		for (com.conveyal.r5.transit.TripPattern pattern : transportNetwork.transitLayer.tripPatterns) {
			if (routeIndices.contains(pattern.routeIndex)) {
				pattern.tripSchedules.clear(); // Remove all schedules to deactivate
			}
		}
	}
}
