package ch.ethz.matsim.r5.utils.spatial;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

public class DefaultCoordToLatLon implements CoordToLatLonTransformation {
	final private CoordinateTransformation transformation;
	
	public DefaultCoordToLatLon(CoordinateTransformation transformation) {
		this.transformation = transformation;
	}

	@Override
	public LatLon transform(Coord coord) {
		Coord temp = transformation.transform(coord);
		return new LatLon(temp.getY(), temp.getX());
	}
}
