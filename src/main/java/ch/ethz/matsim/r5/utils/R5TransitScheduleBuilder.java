package ch.ethz.matsim.r5.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.conveyal.r5.api.util.Stop;
import com.conveyal.r5.transit.TransportNetwork;

public class R5TransitScheduleBuilder {
	final private TransitScheduleFactory factory;
	final private CoordinateTransformation latLonToXY;

	public R5TransitScheduleBuilder(TransitScheduleFactory factory, CoordinateTransformation latLonToXY) {
		this.factory = factory;
		this.latLonToXY = latLonToXY;
	}

	public TransitSchedule build(TransportNetwork network) {
		TransitSchedule schedule = factory.createTransitSchedule();

		for (Stop stop : network.transitLayer.findStopsInEnvelope(network.getEnvelope())) {
			Id<TransitStopFacility> facilityId = Id.create(stop.stopId, TransitStopFacility.class);
			Coord coord = latLonToXY.transform(new Coord(stop.lat, stop.lon));

			TransitStopFacility facility = factory.createTransitStopFacility(facilityId, coord, false);
			facility.setName(stop.name);

			schedule.addStopFacility(facility);
		}

		return schedule;
	}
}
