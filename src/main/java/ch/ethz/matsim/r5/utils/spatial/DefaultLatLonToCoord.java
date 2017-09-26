package ch.ethz.matsim.r5.utils.spatial;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

public class DefaultLatLonToCoord implements LatLonToCoordTransformation {
	final private CoordinateTransformation transformation;
	
	public DefaultLatLonToCoord(CoordinateTransformation transformation) {
		this.transformation = transformation;
	}
	
	@Override
	public Coord transform(LatLon latLon) {
		return transformation.transform(new Coord(latLon.getLongitude(), latLon.getLatitude()));
	}
}
