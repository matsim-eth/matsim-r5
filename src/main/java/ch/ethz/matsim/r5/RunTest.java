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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;

import com.conveyal.r5.api.ProfileResponse;
import com.conveyal.r5.api.util.Itinerary;
import com.conveyal.r5.api.util.LegMode;
import com.conveyal.r5.api.util.PointToPointConnection;
import com.conveyal.r5.api.util.ProfileOption;
import com.conveyal.r5.api.util.SegmentPattern;
import com.conveyal.r5.api.util.Stop;
import com.conveyal.r5.api.util.StreetSegment;
import com.conveyal.r5.api.util.TransitJourneyID;
import com.conveyal.r5.api.util.TransitModes;
import com.conveyal.r5.api.util.TransitSegment;
import com.conveyal.r5.api.util.TripPattern;
import com.conveyal.r5.point_to_point.builder.PointToPointQuery;
import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.transit.RouteInfo;
import com.conveyal.r5.transit.TransportNetwork;
import com.conveyal.r5.transit.TripSchedule;

public class RunTest {
	public static void main(String[] args) throws Exception {
		final String day = "2017-09-25";
		final String timezone = "+02:00";

		String path = "/home/sebastian/r5/input/network.dat";
		TransportNetwork transportNetwork = TransportNetwork.read(new File(path));

		// START: Cleanup of Switzerland GTFS, there are invalid modes included

		List<Integer> routeIndices = new LinkedList<>();

		for (int i = 0; i < transportNetwork.transitLayer.routes.size(); i++) {
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
		
		// 8.722651723531023;47.40223762758548;8.680300944178367;47.42261934202454

		//Coord fromCoord = new Coord(47.40223762758548, 8.722651723531023);
		//Coord toCoord = new Coord(47.42261934202454, 8.680300944178367);
		
		// 8.537413630575365;47.265503341923726;8.377937905740572;47.46262147004803;851
		
		Coord fromCoord = new Coord(47.265503341923726, 8.537413630575365);
		Coord toCoord = new Coord(47.46262147004803, 8.377937905740572);
		
		//Coord fromCoord = new Coord(47.384868612482634, 8.495950698852539);
		//Coord toCoord = new Coord(47.39974354712813, 8.465995788574219);
		
		//Coord fromCoord = new Coord(47.3762586, 8.5591612);
		//Coord toCoord = new Coord(47.372402, 8.574383);

		// Coord fromCoord = new Coord(47.4085154657897, 8.507575392723083);
		// Coord toCoord = new Coord(47.419115338510494, 8.50142776966095);

		// Coord fromCoord = new Coord(47.3779671928823, 8.539735078811646);
		// Coord toCoord = new Coord(47.36685037580459, 8.541269302368164);

		// Coord fromCoord = new Coord(47.36638530755708, 8.540582656860352);
		// Coord toCoord = new Coord(47.40845737841777, 8.504962921142578);

		double departureTime = 7.5 * 3600.0;

		double minDepartureTime = departureTime;
		double maxDepartureTime = 7.5 * 3600.0 + 1;

		PointToPointQuery query = new PointToPointQuery(transportNetwork);
		ProfileRequest profileRequest = new ProfileRequest();

		// TODO: Or should we set the same value?
		String startTimestamp = String.format("%sT%s%s", day, Time.writeTime(minDepartureTime), timezone);
		String endTimestamp = String.format("%sT%s%s", day, Time.writeTime(maxDepartureTime), timezone);

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
		profileRequest.limit = 100;
		profileRequest.monteCarloDraws = 0;

		profileRequest.directModes = EnumSet.noneOf(LegMode.class); // No direct walk
		profileRequest.transitModes = EnumSet.of(TransitModes.TRAM, TransitModes.RAIL, TransitModes.BUS,
				TransitModes.FUNICULAR);
		profileRequest.accessModes = EnumSet.of(LegMode.WALK);
		profileRequest.egressModes = EnumSet.of(LegMode.WALK);
		
		ProfileResponse response = query.getPlan(profileRequest);

		ProfileOption selectedOption = null;
		Itinerary selectedItinerary = null;
		double selectedArrivalTime = Double.POSITIVE_INFINITY;

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

				double arrivalTime = 0.0;
				arrivalTime += itinerary.endTime.getHour() * 3600.0;
				arrivalTime += itinerary.endTime.getMinute() * 60.0;
				arrivalTime += itinerary.endTime.getSecond();

				System.out.println("   Arrival time: " + Time.writeTime(arrivalTime));
				System.out.println("   Stages: " + option.transit.size());

				for (int stageIndex = 0; stageIndex < option.transit.size(); stageIndex++) {
					int patternIndex = itinerary.connection.transit.get(stageIndex).pattern;
					int timeIndex = itinerary.connection.transit.get(stageIndex).time;

					SegmentPattern pattern = option.transit.get(stageIndex).segmentPatterns.get(patternIndex);
					
					double stageDepartureTime = pattern.fromDepartureTime.get(timeIndex).get(ChronoField.SECOND_OF_DAY);
					double stageArrivalTime = pattern.toArrivalTime.get(timeIndex).get(ChronoField.SECOND_OF_DAY);
					
					Stop departureStop = option.transit.get(stageIndex).from;
					Stop arrivalStop = option.transit.get(stageIndex).to;

					System.out.println(String.format("      %s (%s) --> %s (%s)", departureStop.name,
							Time.writeTime(stageDepartureTime), arrivalStop.name, Time.writeTime(stageArrivalTime)));

					if (option.transit.get(stageIndex).middle == null) {
						System.out.println("         No middle.");
					} else {
						StreetSegment middle = option.transit.get(stageIndex).middle;
						System.out.println("         Middle: " + middle.mode + " "
								+ middle.streetEdges.stream().mapToDouble(e -> e.distance).sum());
					}
				}

				if (arrivalTime < selectedArrivalTime) {
					selectedOption = option;
					selectedItinerary = itinerary;
					selectedArrivalTime = arrivalTime;
				}
			}
		}
	}
}
