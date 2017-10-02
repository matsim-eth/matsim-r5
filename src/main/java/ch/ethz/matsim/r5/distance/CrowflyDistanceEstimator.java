package ch.ethz.matsim.r5.distance;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import com.conveyal.r5.api.util.Stop;

import ch.ethz.matsim.r5.utils.spatial.LatLon;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

public class CrowflyDistanceEstimator implements DistanceEstimator {
	final private LatLonToCoordTransformation latLonToCoord;;
	
	public CrowflyDistanceEstimator(LatLonToCoordTransformation latLonToCoord) {
		this.latLonToCoord = latLonToCoord;
	}
	
	@Override
	public double getDistance(Stop fromStop, Stop toStop, int routeIndex) {
		Coord fromCoord = latLonToCoord.transform(new LatLon(fromStop.lat, fromStop.lon));
		Coord toCoord = latLonToCoord.transform(new LatLon(toStop.lat, toStop.lon));
		return CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
	}
}
