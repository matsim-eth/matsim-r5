package ch.ethz.matsim.r5;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

import com.conveyal.r5.api.ProfileResponse;
import com.conveyal.r5.api.util.Itinerary;
import com.conveyal.r5.api.util.LegMode;
import com.conveyal.r5.api.util.ProfileOption;
import com.conveyal.r5.api.util.SegmentPattern;
import com.conveyal.r5.api.util.Stop;
import com.conveyal.r5.api.util.StreetSegment;
import com.conveyal.r5.api.util.TransitModes;
import com.conveyal.r5.api.util.TransitSegment;
import com.conveyal.r5.point_to_point.builder.PointToPointQuery;
import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.transit.TransportNetwork;

import ch.ethz.matsim.r5.distance.DistanceEstimator;
import ch.ethz.matsim.r5.matsim.R5Module;
import ch.ethz.matsim.r5.route.R5AccessLeg;
import ch.ethz.matsim.r5.route.R5EgressLeg;
import ch.ethz.matsim.r5.route.R5Leg;
import ch.ethz.matsim.r5.route.R5TransferLeg;
import ch.ethz.matsim.r5.route.R5TransitLeg;
import ch.ethz.matsim.r5.scoring.R5ItineraryScorer;
import ch.ethz.matsim.r5.utils.spatial.LatLon;

/**
 * R5 Transit Router for MATSim
 * 
 * Currently transit routes are not related to the MATSim network - Distances
 * are crowfly distances, while travel times are routed according to schedule
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class R5LegRouter {
	final private TransportNetwork transportNetwork;
	final private R5ItineraryScorer scorer;
	final private DistanceEstimator distanceEstimator;

	final private String day;
	final private String timezone;

	/**
	 * R5 Transit Router for MATSim
	 * 
	 * @param transportNetwork
	 *            R5 TransportNetwork instance
	 * @param scorer
	 *            Scores the obtained routes for selection
	 * @param distanceEstimator
	 *            Estimates the distance for a stop relation
	 * @param day
	 *            Selected day of the transit schedule, given as a string in
	 *            YYYY-MM-DD
	 * @param timezone
	 *            Selected timezone in which departure times are given (should match
	 *            the schedule input), given e.g. as +02:00
	 */
	public R5LegRouter(TransportNetwork transportNetwork, R5ItineraryScorer scorer, DistanceEstimator distanceEstimator,
			String day, String timezone) {
		this.distanceEstimator = distanceEstimator;
		this.transportNetwork = transportNetwork;
		this.scorer = scorer;
		this.day = day;
		this.timezone = timezone;
	}

	/**
	 * Creates a profile request for R5
	 * 
	 * <ul>
	 * <li>no direct walk is allowed
	 * <li>access and egress only through "walk"
	 * </ul>
	 * 
	 * @param fromLocation
	 *            Start location of trip.
	 * @param toLocation
	 *            End location of trip
	 * @param departureTime
	 *            in seconds
	 */
	private ProfileRequest prepareProfileRequest(LatLon fromLocation, LatLon toLocation, double departureTime) {
		ProfileRequest profileRequest = new ProfileRequest();
		
		while (departureTime > 24.0 * 3600.0) {
			departureTime -= 24 * 3600.0;
		}
		
		ZonedDateTime departureDateTime = ZonedDateTime.parse(String.format("%sT%s%s", day, Time.writeTime(0.0), timezone));
		departureDateTime = departureDateTime.plusSeconds((long) departureTime);
		
		// endTimestamp is +1 sec to avoid aborting
		ZonedDateTime departureDateTimePlusOne = departureDateTime.plusSeconds(1);

		String startTimestamp = departureDateTime.toString();
		String endTimestamp = departureDateTimePlusOne.toString();

		profileRequest.zoneId = transportNetwork.getTimeZone();
		profileRequest.fromLat = fromLocation.getLatitude();
		profileRequest.fromLon = fromLocation.getLongitude();
		profileRequest.toLat = toLocation.getLatitude();
		profileRequest.toLon = toLocation.getLongitude();
		profileRequest.setTime(startTimestamp, endTimestamp);

		profileRequest.directModes = EnumSet.noneOf(LegMode.class); // No direct walk
		profileRequest.transitModes = EnumSet.allOf(TransitModes.class);
		profileRequest.accessModes = EnumSet.of(LegMode.WALK);
		profileRequest.egressModes = EnumSet.of(LegMode.WALK);

		return profileRequest;
	}

	/**
	 * Verify that R5 schedule is compatible with MATSim
	 * 
	 * <ul>
	 * <li>Max. one access/egress leg
	 * <li>Min. one transit leg
	 * <li>Access/egress/transfer
	 * </ul>
	 * 
	 * @param option
	 *            from R5
	 */
	private void verify(ProfileOption option) {
		if (option.access.size() > 1) {
			throw new IllegalStateException("More than one access segment");
		}

		if (option.transit.size() == 0) {
			throw new IllegalStateException("No transit segment");
		}

		if (option.egress.size() > 1) {
			throw new IllegalStateException("More than one egress segment");
		}

		if (option.access.size() == 1 && !option.access.get(0).mode.equals(LegMode.WALK)) {
			throw new IllegalStateException("Only WALK is allowed for access");
		}

		if (option.egress.size() == 1 && !option.egress.get(0).mode.equals(LegMode.WALK)) {
			throw new IllegalStateException("Only WALK is allowed for egress");
		}
	}

	private LatLon getStopLocation(Stop stop) {
		return new LatLon(stop.lat, stop.lon);
	}

	private String getStopId(Stop stop) {
		return stop.stopId;
	}

	/**
	 * Returns an R5 itinerary as a chain of legs
	 * 
	 * @param fromLocation
	 *            Start location
	 * @param toLocation
	 *            End location
	 * @param departureTime
	 *            in seconds of day
	 * @param option
	 *            from R5
	 * @param itinerary
	 *            fromR5
	 * @return
	 */
	private List<R5Leg> route(LatLon fromLocation, LatLon toLocation, double departureTime, ProfileOption option,
			Itinerary itinerary, Person person) {
		List<R5Leg> plan = new LinkedList<>();
		verify(option);

		double currentTime = itinerary.startTime.get(ChronoField.SECOND_OF_DAY);
		while (currentTime < departureTime)
			currentTime += 3600.0 * 24.0;

		// Add access walk
		if (option.access.size() == 1) {
			StreetSegment segment = option.access.get(0);
			Stop accessStop = option.transit.get(0).from;

			R5AccessLeg accessLeg = new R5AccessLeg(departureTime, segment.duration, ((double) segment.distance) / 1e3,
					fromLocation, getStopLocation(accessStop), getStopId(accessStop));
			plan.add(accessLeg);

			currentTime += segment.duration;

			// TODO: Here we possibly introduce additional wait times. R5 gives a route
			// which may
			// start after departureTime (i.e. the agent would have a bit more time at his
			// previous
			// activity). Here, however, we let the agent start walking immediately, which
			// produces
			// a larger gap between arriving at the stop and entering the vehicle.
		}

		R5TransferLeg previousTransferLeg = null;

		// Add transit segments
		for (int i = 0; i < option.transit.size(); i++) {
			int patternIndex = itinerary.connection.transit.get(i).pattern;
			int timeIndex = itinerary.connection.transit.get(i).time;

			TransitSegment segment = option.transit.get(i);
			SegmentPattern pattern = option.transit.get(i).segmentPatterns.get(patternIndex);

			double segmentDepartureTime = pattern.fromDepartureTime.get(timeIndex).get(ChronoField.SECOND_OF_DAY);
			while (segmentDepartureTime < currentTime)
				segmentDepartureTime += 3600.0 * 24.0;
			double segmentArrivalTime = pattern.toArrivalTime.get(timeIndex).get(ChronoField.SECOND_OF_DAY);
			while (segmentArrivalTime < segmentDepartureTime)
				segmentArrivalTime += 3600.0 * 24.0;
			double segmentTravelTime = segmentArrivalTime - segmentDepartureTime;

			R5TransitLeg transitLeg = new R5TransitLeg(segmentDepartureTime, segmentTravelTime,
					distanceEstimator.getDistance(segment.from, segment.to, pattern.routeIndex),
					getStopLocation(segment.from), getStopLocation(segment.to), getStopId(segment.from),
					getStopId(segment.to), patternIndex, timeIndex);
			plan.add(transitLeg);

			currentTime = segmentDepartureTime + segmentTravelTime;

			if (previousTransferLeg != null) {
				previousTransferLeg.setArrival(getStopLocation(segment.from), getStopId(segment.from));
				previousTransferLeg = null;
			}

			if (segment.middle != null) {
				R5TransferLeg transferLeg = new R5TransferLeg(currentTime, segment.middle.duration,
						((double) segment.middle.distance) / 1e3, getStopLocation(segment.to), getStopId(segment.to));
				plan.add(transferLeg);

				currentTime += segment.middle.duration;
				previousTransferLeg = transferLeg;
			}
		}

		// Add egress walk
		if (option.egress.size() == 1) {
			StreetSegment segment = option.egress.get(0);
			Stop egressStop = option.transit.get(option.transit.size() - 1).to;

			R5EgressLeg egressLeg = new R5EgressLeg(currentTime, segment.duration, ((double) segment.distance) / 1e3,
					getStopLocation(egressStop), toLocation, getStopId(egressStop));
			plan.add(egressLeg);

			currentTime += segment.duration;
		}

		// VALIDATION

		double expectedStartTime = itinerary.startTime.get(ChronoField.SECOND_OF_DAY);
		while (expectedStartTime < departureTime)
			expectedStartTime += 3600.0 * 24.0;
		double expectedEndTime = itinerary.endTime.get(ChronoField.SECOND_OF_DAY);
		while (expectedEndTime < expectedStartTime || expectedEndTime < departureTime)
			expectedEndTime += 3600.0 * 24.0;
		
		double reconstructedEndTime = plan.get(plan.size() - 1).getDepartureTime()
				+ plan.get(plan.size() - 1).getTravelTime();

		if (reconstructedEndTime != expectedEndTime) {
			R5Module.logger.error(String.format("Person: %s, Expected: %s, Found: %s", person == null ? "null" : person.getId().toString(), Time.writeTime(expectedEndTime), Time.writeTime(reconstructedEndTime)));
			throw new IllegalStateException("End time has not been reconstructed properly");
		}

		double previousArrivalTime = departureTime;

		for (R5Leg leg : plan) {
			if (leg.getDepartureTime() < previousArrivalTime) {
				throw new IllegalStateException("Departure is before last arrival");
			}

			if (leg.getTravelTime() < 0.0) {
				throw new IllegalStateException("Negative travel time");
			}
		}

		return plan;
	}

	/**
	 * Calculates a chain of legs for a given PT OD relation
	 * 
	 * Uses R5 to find a set of viable itineraries and selects the one that scores
	 * best
	 * 
	 * @param fromLocation
	 *            Start location
	 * @param toLocation
	 *            End location
	 * @param departureTime
	 *            in seconds of day
	 * @return May return null if no route is found
	 */
	public List<R5Leg> route(LatLon fromLocation, LatLon toLocation, double departureTime, Person person) {
		try {
			PointToPointQuery query = new PointToPointQuery(transportNetwork);
			ProfileRequest profileRequest = prepareProfileRequest(fromLocation, toLocation, departureTime);
			ProfileResponse response = query.getPlan(profileRequest);

			// Find quickest connection (soonest arrival time)
			ProfileOption selectedOption = null;
			Itinerary selectedItinerary = null;
			double selectedScore = Double.NEGATIVE_INFINITY;

			for (ProfileOption option : response.getOptions()) {
				for (Itinerary itinerary : option.itinerary) {
					double score = scorer.scoreItinerary(itinerary, departureTime);

					if (score > selectedScore) {
						selectedScore = score;
						selectedOption = option;
						selectedItinerary = itinerary;
					}
				}
			}

			if (selectedOption != null) {
				return route(fromLocation, toLocation, departureTime, selectedOption, selectedItinerary, person);
			}
		} catch (IllegalStateException e) {
			if (!e.getMessage().contains("No valid itineraries found for path")) {
				throw e;
			}
		}

		return null; // No route found
	}
}
