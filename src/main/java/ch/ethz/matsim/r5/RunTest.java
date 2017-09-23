package ch.ethz.matsim.r5;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
import com.conveyal.r5.api.util.SegmentPattern;
import com.conveyal.r5.api.util.StreetSegment;
import com.conveyal.r5.api.util.TransitJourneyID;
import com.conveyal.r5.api.util.TransitModes;
import com.conveyal.r5.api.util.TripPattern;
import com.conveyal.r5.point_to_point.builder.PointToPointQuery;
import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.transit.RouteInfo;
import com.conveyal.r5.transit.TransportNetwork;

public class RunTest {
	public static void main(String[] args) throws Exception {
		final String day = "2017-02-05";
		final String timezone = "+02:00";

		String path = "/home/sebastian/r5/input/network.dat";
		TransportNetwork transportNetwork = TransportNetwork.read(new File(path));
		
		// START: Cleanup of Switzerland GTFS, there are invalid modes included
		
		List<Integer> routeIndices = new LinkedList<>();
		
		for (int i = 0; i < transportNetwork.transitLayer.routes.size(); i ++) {
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
		
		// END: Cleanup
		
		//Coord fromCoord = new Coord(47.4085154657897, 8.507575392723083);
		//Coord toCoord = new Coord(47.419115338510494, 8.50142776966095);
		
		//Coord fromCoord = new Coord(47.3779671928823, 8.539735078811646);
		//Coord toCoord = new Coord(47.36685037580459, 8.541269302368164);
		
		Coord fromCoord = new Coord(47.36638530755708, 8.540582656860352);
		Coord toCoord = new Coord(47.40845737841777, 8.504962921142578);
		
		double departureTime = 7.5 * 3600.0;
		
		double minDepartureTime = departureTime;
		double maxDepartureTime = departureTime + 5.0 * 3600.0;

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
		profileRequest.transitModes = EnumSet.of(TransitModes.TRAM, TransitModes.RAIL, TransitModes.BUS, TransitModes.FUNICULAR);
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
				
				/*if (option.summary.contains("routes 8, 5")) {
					System.out.println("HERE");
				}*/
			}
		}
		
		if (quickestOption == null) {
			throw new IllegalStateException();
		}
		
		/*StreetSegment accessSegment = quickestOption.access.get(quickestItinerary.connection.access);
		StreetSegment egressSegment = quickestOption.egress.get(quickestItinerary.connection.egress);
		
		List<SegmentPattern> transitPatterns = new LinkedList<>();
		
		for (int i = 0; i < quickestItinerary.connection.transit.size(); i++) {
			TransitJourneyID journey = quickestItinerary.connection.transit.get(i);
			quickestOption.transit.get(i).segmentPatterns.get(journey.pattern);
		}*/
		
		
		
		System.exit(1);
		
		double itineraryStartTime = quickestItinerary.startTime.get(ChronoField.SECOND_OF_DAY);
		
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
