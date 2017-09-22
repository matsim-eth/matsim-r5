package ch.ethz.matsim.r5;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.Time;

import com.conveyal.r5.api.ProfileResponse;
import com.conveyal.r5.api.util.Itinerary;
import com.conveyal.r5.api.util.LegMode;
import com.conveyal.r5.api.util.PointToPointConnection;
import com.conveyal.r5.api.util.ProfileOption;
import com.conveyal.r5.api.util.TransitModes;
import com.conveyal.r5.point_to_point.builder.PointToPointQuery;
import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.transit.TransportNetwork;

public class RunTest {
	public static void main(String[] args) throws Exception {
		final String day = "2017-10-11";
		final String timezone = "+02:00";

		String path = "/home/sebastian/r5/input/network.dat";
		TransportNetwork transportNetwork = TransportNetwork.read(new File(path));

		Coord fromCoord = new Coord(47.4085154657897, 8.507575392723083);
		Coord toCoord = new Coord(47.419115338510494, 8.50142776966095);
		
		double departureTime = 8.0 * 3600.0;
		
		double minDepartureTime = departureTime;
		double maxDepartureTime = departureTime + 2.0 * 3600.0;

		PointToPointQuery query = new PointToPointQuery(transportNetwork);
		ProfileRequest profileRequest = new ProfileRequest();

		// TODO: Or should we set the same value?
		String startTimestamp = String.format("%sT%s%s", day, Time.writeTime(minDepartureTime), timezone);
		String endTimestamp = String.format("%sT%s%s", day, Time.writeTime(maxDepartureTime + 60), timezone);
		
		System.out.println(startTimestamp);
		System.out.println(endTimestamp);
		

		profileRequest.zoneId = transportNetwork.getTimeZone();
		profileRequest.fromLat = fromCoord.getX();
		profileRequest.fromLon = fromCoord.getY();
		profileRequest.toLat = toCoord.getX();
		profileRequest.toLon = toCoord.getY();
		profileRequest.wheelchair = false;
		profileRequest.bikeTrafficStress = 4;
		profileRequest.setTime(startTimestamp, endTimestamp);

		profileRequest.directModes = EnumSet.noneOf(LegMode.class); // No direct walk
		profileRequest.transitModes = EnumSet.of(TransitModes.TRAM, TransitModes.RAIL, TransitModes.BUS);
		profileRequest.accessModes = EnumSet.of(LegMode.WALK);
		profileRequest.egressModes = EnumSet.of(LegMode.WALK);

		ProfileResponse response = query.getPlan(profileRequest);
		
		ProfileOption quickestOption = null;
		Itinerary quickestItinerary = null;
		double quickestArrivalTime = Double.POSITIVE_INFINITY;

		for (ProfileOption option : response.getOptions()) {
			System.out.println(option.summary);
			
			System.out.println("   Access: " + option.access.size());
			System.out.println("   Egress: " + option.egress.size());
			System.out.println("   Transit: " + option.transit.size());
			
			for (Itinerary itinerary : option.itinerary) {
				System.out.println("   " + itinerary.startTime + " ---> " + itinerary.endTime);
				
				double startTime = 0.0;
				startTime += itinerary.startTime.getHour() * 3600.0;
				startTime += itinerary.startTime.getMinute() * 60.0;
				startTime += itinerary.startTime.getSecond();
				
				System.out.println("   Start time: " + Time.writeTime(startTime));
				
				ZonedDateTime _firstTransitTime = option.transit.get(0).segmentPatterns.get(itinerary.connection.transit.get(0).pattern).fromDepartureTime.get(itinerary.connection.transit.get(0).time);
				
				double firstTransitTime = 0.0;
				firstTransitTime += _firstTransitTime.getHour() * 3600.0;
				firstTransitTime += _firstTransitTime.getMinute() * 60.0;
				firstTransitTime += _firstTransitTime.getSecond();
				
				System.out.println("   First transit time: " + firstTransitTime + " " + Time.writeTime(firstTransitTime));
				
				double arrivalTime = 0.0;
				arrivalTime += itinerary.endTime.getHour() * 3600.0;
				arrivalTime += itinerary.endTime.getMinute() * 60.0;
				arrivalTime += itinerary.endTime.getSecond();
				
				if (arrivalTime < quickestArrivalTime) {
					quickestOption = option;
					quickestItinerary = itinerary;
					quickestArrivalTime = arrivalTime;
				}
			}
		}
		
		if (quickestOption == null) {
			throw new IllegalStateException();
		}
		
		System.exit(1);
		
		double itineraryStartTime = 0.0;
		itineraryStartTime += quickestItinerary.startTime.getHour() * 3600.0;
		itineraryStartTime += quickestItinerary.startTime.getMinute() * 60.0;
		itineraryStartTime += quickestItinerary.startTime.getSecond();
		
		Leg accessLeg = PopulationUtils.createLeg(TransportMode.transit_walk);
		accessLeg.setDepartureTime(departureTime);
		accessLeg.setTravelTime(quickestOption.access.get(0).duration);
		
		double arrivalAtAccessStop = departureTime + quickestOption.access.get(0).duration;
		
		System.out.println("Best");
		System.out.println("   Access: " + quickestOption.access.size());
		System.out.println("   Egress: " + quickestOption.egress.size());
		System.out.println("   Transit: " + quickestOption.transit.size());
	}
}
