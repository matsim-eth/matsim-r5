package ch.ethz.matsim.r5;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;

import com.conveyal.r5.api.ProfileResponse;
import com.conveyal.r5.api.util.Itinerary;
import com.conveyal.r5.api.util.LegMode;
import com.conveyal.r5.api.util.PointToPointConnection;
import com.conveyal.r5.api.util.ProfileOption;
import com.conveyal.r5.api.util.TransitModes;
import com.conveyal.r5.point_to_point.builder.PointToPointQuery;
import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.transit.TransportNetwork;

public class R5TransitRouter {// implements TransitRouter {
	/*final private TransportNetwork transportNetwork;
	final private CoordinateTransformation xyToLonLat;
	
	final private String day = "2017-10-11";
	final private String timezone = "+02:00";
	
	public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {
		PointToPointQuery query = new PointToPointQuery(transportNetwork);
		ProfileRequest profileRequest = new ProfileRequest();
		
		Coord fromCoord = xyToLonLat.transform(fromFacility.getCoord());
		Coord toCoord = xyToLonLat.transform(toFacility.getCoord());
		
		// TODO: Or should we set the same value?
		String startTimestamp = String.format("%sT%s%s", day, Time.writeTime(departureTime), timezone);
		String endTimestamp = String.format("%sT%s%s", day, Time.writeTime(departureTime + 60), timezone);
		
		profileRequest.zoneId = transportNetwork.getTimeZone();
		profileRequest.fromLat = fromCoord.getX();
		profileRequest.fromLon = fromCoord.getY();
		profileRequest.toLat = toCoord.getX();
		profileRequest.toLon = toCoord.getY();
		profileRequest.wheelchair = false;
		profileRequest.bikeTrafficStress = 4;
		profileRequest.setTime(startTimestamp, endTimestamp);
		
		profileRequest.directModes = EnumSet.noneOf(LegMode.class); // No direct walk
		profileRequest.transitModes = EnumSet.of(TransitModes.TRAM,TransitModes.RAIL,TransitModes.BUS);
		profileRequest.accessModes = EnumSet.of(LegMode.WALK);
		profileRequest.egressModes = EnumSet.of(LegMode.WALK);
		
		ProfileResponse response = query.getPlan(profileRequest);
		
		Itinerary quickestItinery = null;
		double quickestItineryEndTime = Integer.MAX_VALUE;
		
		for (ProfileOption option : response.options) {
			for (Itinerary itinerary : option.itinerary) {
				double endTime = itinerary.endTime.getHour() * 3600.0 + itinerary.endTime.getMinute() * 60.0 + itinerary.endTime.getSecond();
				
				if (endTime < quickestItineryEndTime) {
					quickestItinery = itinerary;
					quickestItineryEndTime = endTime;
				}
			}
		}
		
		if (quickestItinery == null) {
			throw new IllegalStateException();
		}
		
		option.
		
		PointToPointConnection connection = quickestItinery.connection;
		connection.
		
		for (PointquickestItinery.connection)
		
		return null;
	}*/
}
