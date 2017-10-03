package ch.ethz.matsim.r5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;

import ch.ethz.matsim.r5.route.LinkFinder;
import ch.ethz.matsim.r5.route.R5AccessLeg;
import ch.ethz.matsim.r5.route.R5EgressLeg;
import ch.ethz.matsim.r5.route.R5Leg;
import ch.ethz.matsim.r5.route.R5TransferLeg;
import ch.ethz.matsim.r5.route.R5TransitLeg;
import ch.ethz.matsim.r5.utils.spatial.CoordToLatLonTransformation;
import ch.ethz.matsim.r5.utils.spatial.LatLon;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

public class R5TeleportationRoutingModule implements RoutingModule {
	final private R5LegRouter router;
	final private RoutingModule walkRouter;

	final private CoordToLatLonTransformation coordToLatLon;
	final private LatLonToCoordTransformation latLonToCoord;
	
	final private LinkFinder linkFinder;

	public R5TeleportationRoutingModule(R5LegRouter router, CoordToLatLonTransformation coordToLatLon,
			LatLonToCoordTransformation latLonToCoord, RoutingModule walkRouter, LinkFinder linkFinder) {
		this.router = router;
		this.coordToLatLon = coordToLatLon;
		this.latLonToCoord = latLonToCoord;
		this.walkRouter = walkRouter;
		this.linkFinder = linkFinder;
	}
	
	private Id<Link> getStartLinkId(R5Leg leg, Facility<?> fromFacility) {
		if (leg instanceof R5AccessLeg) {
			return fromFacility.getLinkId();
		} else if (leg instanceof R5TransitLeg) {
			return linkFinder.findLink(((R5TransitLeg) leg).getDepartureStopId());
		} else if (leg instanceof R5TransferLeg) {
			return linkFinder.findLink(((R5TransferLeg) leg).getDepartureStopId());
		} else if (leg instanceof R5EgressLeg) {
			return linkFinder.findLink(((R5EgressLeg) leg).getDepartureStopId());
		}

		throw new IllegalStateException();
	}
	
	private Id<Link> getEndLinkId(R5Leg leg, Facility<?> toFacility) {
		if (leg instanceof R5EgressLeg) {
			return toFacility.getLinkId();
		} else if (leg instanceof R5TransitLeg) {
			return linkFinder.findLink(((R5TransitLeg) leg).getArrivalStopId());
		} else if (leg instanceof R5TransferLeg) {
			return linkFinder.findLink(((R5TransferLeg) leg).getArrivalStopId());
		} else if (leg instanceof R5AccessLeg) {
			return linkFinder.findLink(((R5AccessLeg) leg).getArrivalStopId());
		}

		throw new IllegalStateException();
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		LatLon fromLocation = coordToLatLon.transform(fromFacility.getCoord());
		LatLon toLocation = coordToLatLon.transform(toFacility.getCoord());

		List<R5Leg> legs = router.route(fromLocation, toLocation, departureTime, person);
		
		if (legs != null) {
			List<PlanElement> matsimPlan = new ArrayList<>(legs.size());
	
			Activity previousActivity = null;
	
			// TODO: Here we need proper link IDs ...
			// Ideally we need to attach all stops to links
			
			for (int i = 0; i < legs.size(); i++) {
				R5Leg leg = legs.get(i);
	
				boolean isWalk = leg instanceof R5AccessLeg || leg instanceof R5EgressLeg || leg instanceof R5TransferLeg;
				Leg matsimLeg = PopulationUtils.createLeg(isWalk ? "transit_walk" : "pt");
				matsimLeg.setDepartureTime(leg.getDepartureTime());
				matsimLeg.setTravelTime(leg.getArrivalTime() - leg.getDepartureTime());
				
				Id<Link> startLinkId = getStartLinkId(leg, fromFacility);
				Id<Link> endLinkId = getEndLinkId(leg, toFacility);
	
				Route matsimRoute = new GenericRouteImpl(startLinkId, endLinkId);
				matsimRoute.setDistance(leg.getDistance());
				matsimRoute.setTravelTime(leg.getArrivalTime() - leg.getDepartureTime());
	
				matsimLeg.setRoute(matsimRoute);
				matsimPlan.add(matsimLeg);
	
				if (previousActivity != null) {
					previousActivity.setLinkId(startLinkId);
					previousActivity.setEndTime(leg.getDepartureTime());
				}
	
				if (i < legs.size() - 1) {
					Activity activity = PopulationUtils.createActivityFromCoord("pt interaction",
							latLonToCoord.transform(leg.getArrivalLocation()));
					activity.setStartTime(leg.getArrivalTime());
					matsimPlan.add(activity);
					previousActivity = activity;
				}
			}
	
			return matsimPlan;
		} else if (walkRouter != null) {
			List<? extends PlanElement> alternative = walkRouter.calcRoute(fromFacility, toFacility, departureTime, person);
			((Leg)alternative.get(0)).setMode("transit_walk");
			return alternative;
		} else {
			throw new RuntimeException("R5 did not find a PT route and no alternative routing module is defined");
		}
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new StageActivityTypesImpl("pt interaction");
	}
}
