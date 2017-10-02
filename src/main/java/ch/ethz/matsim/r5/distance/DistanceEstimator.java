package ch.ethz.matsim.r5.distance;

import com.conveyal.r5.api.util.Stop;

public interface DistanceEstimator {
	double getDistance(Stop fromStop, Stop toStop, int routeIndex);
}
