package ch.ethz.matsim.r5.utils.spatial;

public class LatLon {
	final private double latitude;
	final private double longitude;

	public LatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
	
	@Override
	public String toString() {
		return "[" + latitude + " ; " + longitude + "]";
	}
}
