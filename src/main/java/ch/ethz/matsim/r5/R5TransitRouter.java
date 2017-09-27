package ch.ethz.matsim.r5;

import java.time.temporal.ChronoField;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

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

import ch.ethz.matsim.r5.scoring.R5ItineraryScorer;
import ch.ethz.matsim.r5.utils.spatial.CoordToLatLonTransformation;
import ch.ethz.matsim.r5.utils.spatial.LatLon;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

/**
 * R5 Transit Router for MATSim
 * 
 * - Currently transit routes are not related to the MATSim network - Distances
 * are crowfly distances, while travel times are routed according to schedule
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class R5TransitRouter implements TransitRouter {
	final private TransportNetwork transportNetwork;
	final private TransitSchedule transitSchedule;
	final private R5ItineraryScorer scorer;

	final private CoordToLatLonTransformation coordToLatLon;
	final private LatLonToCoordTransformation latLonToCoord;

	final private String day;
	final private String timezone;

	/**
	 * R5 Transit Router for MATSim
	 * 
	 * @param transportNetwork
	 *            R5 TransportNetwork instance
	 * @param transitSchedule
	 *            MATSim transit schedule with R5 stop ids included
	 * @param scorer
	 *            Scores the obtained routes for selection
	 * @param coordToLonLat
	 *            Transformation from MATSim (x,y) to longitude/latitude
	 * @param lonLatToCoord
	 *            Transformaton from longitude/latitude to MATSim (x,y)
	 * @param day
	 *            Selected day of the transit schedule, given as a string in
	 *            YYYY-MM-DD
	 * @param timezone
	 *            Selected timezone in which departure times are given (should match
	 *            the schedule input), given e.g. as +02:00
	 */
	public R5TransitRouter(TransportNetwork transportNetwork, TransitSchedule transitSchedule,
			R5ItineraryScorer scorer, CoordToLatLonTransformation coordToLonLat, LatLonToCoordTransformation lonLatToCoord,
			String day, String timezone) {
		this.transportNetwork = transportNetwork;
		this.transitSchedule = transitSchedule;
		this.scorer = scorer;
		this.coordToLatLon = coordToLonLat;
		this.latLonToCoord = lonLatToCoord;
		this.day = day;
		this.timezone = timezone;
	}

	/**
	 * Creates a profile request for R5
	 * 
	 * - no direct walk is allowed - access and egree only through "walk"
	 * 
	 * @param fromFacility
	 *            Start facility of trip.
	 * @param toFacility
	 *            End facility of trip
	 * @param departureTime
	 *            in seconds
	 * @param departureTimeOffset
	 *            defines the search space starting from departureTime (in seconds)
	 */
	private ProfileRequest prepareProfileRequest(Facility<?> fromFacility, Facility<?> toFacility, double departureTime) {
		ProfileRequest profileRequest = new ProfileRequest();

		LatLon fromLatLon = coordToLatLon.transform(fromFacility.getCoord());
		LatLon toLatLon = coordToLatLon.transform(toFacility.getCoord());

		// endTimestamp is +1 sec to avoid aborting
		String startTimestamp = String.format("%sT%s%s", day, Time.writeTime(departureTime), timezone);
		String endTimestamp = String.format("%sT%s%s", day, Time.writeTime(departureTime + 1.0), timezone);

		profileRequest.zoneId = transportNetwork.getTimeZone();
		profileRequest.fromLat = fromLatLon.getLatitude();
		profileRequest.fromLon = fromLatLon.getLongitude();
		profileRequest.toLat = toLatLon.getLatitude();
		profileRequest.toLon = toLatLon.getLongitude();
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
	 * - Max. one access/egress leg - Min. one transit leg - Access/egress/transfer
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

	/**
	 * Returns a TransitStopFacility for an R5 stop
	 * 
	 * - Right not it is created on the fly with the respective coordinates -
	 * Eventually this should be cached / transit stops should be explicitly
	 * attached to links
	 * 
	 * @param stop
	 */
	private TransitStopFacility getStopFacility(Stop stop) {
		Id<TransitStopFacility> stopId = Id.create(stop.stopId, TransitStopFacility.class);
		
		if (!transitSchedule.getFacilities().containsKey(stopId)) {
			Coord coord = latLonToCoord.transform(new LatLon(stop.lat, stop.lon));
			
			TransitStopFacility facility = transitSchedule.getFactory().createTransitStopFacility(stopId, coord, false);
			facility.setName(stop.name);
			
			transitSchedule.addStopFacility(facility);
		}
		
		return transitSchedule.getFacilities().get(stopId);
	}

	/**
	 * Returns the distance of a R5 trip from one stop to another
	 * 
	 * - Right now the CROWFLY distance is computed - Eventually this should become
	 * a routed distance
	 * 
	 * @param fromStop
	 *            from R5
	 * @param toStop
	 *            from R5
	 * @param routeIndex
	 *            from R5
	 */
	private double getRouteDistance(Stop fromStop, Stop toStop, int routeIndex) {
		// TODO: Eventually, this should become a routed distance!
		Coord fromLatLon = latLonToCoord.transform(new LatLon(fromStop.lat, fromStop.lon));
		Coord toLatLon = latLonToCoord.transform(new LatLon(toStop.lat, toStop.lon));
		return CoordUtils.calcEuclideanDistance(fromLatLon, toLatLon);
	}

	/**
	 * Transforms an R5 itinerary into a chain of MATSim legs
	 * 
	 * @param fromFacility
	 *            Start facility
	 * @param toFacility
	 *            End facility
	 * @param departureTime
	 *            in seconds of day
	 * @param option
	 *            from R5
	 * @param itinerary
	 *            fromR5
	 * @return
	 */
	private List<Leg> transformToLegs(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			ProfileOption option, Itinerary itinerary) {
		List<Leg> plan = new LinkedList<>();
		verify(option);
		
		double currentTime = departureTime;

		// Add access walk
		if (option.access.size() == 1) {
			StreetSegment segment = option.access.get(0);

			Stop accessStop = option.transit.get(0).from;
			Facility<?> accessFacility = getStopFacility(accessStop);

			Leg leg = PopulationUtils.createLeg("transit_walk");
			leg.setDepartureTime(departureTime);
			leg.setTravelTime(segment.duration);

			Route route = new GenericRouteImpl(null, null); //fromFacility.getLinkId(), accessFacility.getLinkId());
			route.setDistance(((double) segment.distance) / 1e6);
			route.setTravelTime(segment.duration);

			leg.setRoute(route);
			plan.add(leg);
			currentTime = leg.getDepartureTime() + leg.getTravelTime();
		}

		Route previousTransferRoute = null;
		// Add transit segments
		for (int i = 0; i < option.transit.size(); i++) {
			int patternIndex = itinerary.connection.transit.get(i).pattern;
			int timeIndex = itinerary.connection.transit.get(i).time;

			TransitSegment segment = option.transit.get(i);
			SegmentPattern pattern = option.transit.get(i).segmentPatterns.get(patternIndex);

			double segmentDepartureTime = pattern.fromDepartureTime.get(timeIndex).get(ChronoField.SECOND_OF_DAY);
			double segmentArrivalTime = pattern.toArrivalTime.get(timeIndex).get(ChronoField.SECOND_OF_DAY);
			double segmentTravelTime = segmentArrivalTime - segmentDepartureTime;

			TransitStopFacility departureFacility = getStopFacility(segment.from);
			TransitStopFacility arrivalFacility = getStopFacility(segment.to);

			Leg leg = PopulationUtils.createLeg("pt");
			leg.setDepartureTime(segmentDepartureTime);
			leg.setTravelTime(segmentTravelTime);
			
			Id<TransitLine> lineId = Id.create(pattern.patternId, TransitLine.class);
			Id<TransitRoute> routeId = Id.create(segment.routes.get(pattern.routeIndex).shortName, TransitRoute.class);
			
			ExperimentalTransitRoute route = new ExperimentalTransitRoute(departureFacility, arrivalFacility, lineId, routeId);

			//Route route = new GenericRouteImpl(departureFacility.getLinkId(), arrivalFacility.getLinkId());
			route.setDistance(getRouteDistance(segment.from, segment.to, pattern.routeIndex));
			route.setTravelTime(segmentTravelTime);
			//route.setRouteDescription(segment.from.name + " --- (" + segment.routes.get(pattern.routeIndex).shortName + ") ---> " + segment.to.name);

			leg.setRoute(route);
			plan.add(leg);
			
			currentTime = leg.getDepartureTime() + leg.getTravelTime();

			if (previousTransferRoute != null) {
				previousTransferRoute.setEndLinkId(departureFacility.getLinkId());
				previousTransferRoute = null;
			}

			if (segment.middle != null) {
				Leg transferLeg = PopulationUtils.createLeg("transit_walk");
				transferLeg.setDepartureTime(segmentDepartureTime + segmentTravelTime);
				transferLeg.setTravelTime(segment.middle.duration);

				Route transferRoute = new GenericRouteImpl(arrivalFacility.getLinkId(), null);
				transferRoute.setTravelTime(segment.middle.duration);
				transferRoute.setDistance(((double) segment.middle.distance) / 1e6);

				transferLeg.setRoute(transferRoute);
				plan.add(transferLeg);
				currentTime = leg.getDepartureTime() + leg.getTravelTime();

				previousTransferRoute = transferRoute;
			}
		}

		// Add egress walk
		if (option.egress.size() == 1) {
			StreetSegment segment = option.egress.get(0);

			Stop egressStop = option.transit.get(option.transit.size() - 1).to;
			Facility<?> egressFacility = getStopFacility(egressStop);

			Leg leg = PopulationUtils.createLeg("transit_walk");
			leg.setDepartureTime(currentTime);
			leg.setTravelTime(segment.duration);

			Route route = new GenericRouteImpl(null, null); //egressFacility.getLinkId(), toFacility.getLinkId());
			route.setDistance(((double) segment.distance) / 1e6);
			route.setTravelTime(segment.duration);

			leg.setRoute(route);
			plan.add(leg);
		}
		
		plan.get(0).setDepartureTime(departureTime);
		plan.get(0).setTravelTime(itinerary.endTime.get(ChronoField.SECOND_OF_DAY));

		return plan;
	}

	/**
	 * Calculates a chain of MATSim legs for a given PT OD relation
	 * 
	 * Uses R5 to find a set of viable itineraries and selects the one that scores best
	 * 
	 * @param fromFacility
	 *            Start facility
	 * @param toFacility
	 *            End facility
	 * @param departureTime
	 *            in seconds of day
	 * @param person
	 *            not used right now
	 */
	public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {
		PointToPointQuery query = new PointToPointQuery(transportNetwork);

		ProfileRequest profileRequest = prepareProfileRequest(fromFacility, toFacility, departureTime);
		ProfileResponse response = query.getPlan(profileRequest);

		// Find quickest connection (soonest arrival time)
		ProfileOption selectedOption = null;
		Itinerary selectedItinerary = null;
		double selectedScore = Double.NEGATIVE_INFINITY;

		for (ProfileOption option : response.getOptions()) {
			for (Itinerary itinerary : option.itinerary) {
				double score = scorer.scoreItinerary(itinerary);

				if (score > selectedScore) {
					selectedScore = score;
					selectedOption = option;
					selectedItinerary = itinerary;
				}
			}
		}

		if (selectedOption != null) {
			System.out.println(Time.writeTime(selectedItinerary.duration));
			return transformToLegs(fromFacility, toFacility, departureTime, selectedOption, selectedItinerary);
		}

		return null; // No route found
	}
}
