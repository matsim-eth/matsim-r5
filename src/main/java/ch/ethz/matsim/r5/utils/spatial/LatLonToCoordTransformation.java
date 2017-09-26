package ch.ethz.matsim.r5.utils.spatial;

import org.matsim.api.core.v01.Coord;

public interface LatLonToCoordTransformation {
	Coord transform(LatLon latLon);
}
