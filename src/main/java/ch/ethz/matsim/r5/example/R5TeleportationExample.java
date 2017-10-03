package ch.ethz.matsim.r5.example;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.FakeFacility;

import com.conveyal.r5.transit.TransportNetwork;

import ch.ethz.matsim.r5.R5LegRouter;
import ch.ethz.matsim.r5.R5TeleportationRoutingModule;
import ch.ethz.matsim.r5.distance.CrowflyDistanceEstimator;
import ch.ethz.matsim.r5.distance.DistanceEstimator;
import ch.ethz.matsim.r5.route.LinkFinder;
import ch.ethz.matsim.r5.route.LoopLinkFinder;
import ch.ethz.matsim.r5.route.UnknownLinkFinder;
import ch.ethz.matsim.r5.scoring.R5ItineraryScorer;
import ch.ethz.matsim.r5.scoring.SoonestArrivalTimeScorer;
import ch.ethz.matsim.r5.utils.R5Cleaner;
import ch.ethz.matsim.r5.utils.spatial.CoordToLatLonTransformation;
import ch.ethz.matsim.r5.utils.spatial.DefaultCoordToLatLon;
import ch.ethz.matsim.r5.utils.spatial.DefaultLatLonToCoord;
import ch.ethz.matsim.r5.utils.spatial.LatLon;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

public class R5TeleportationExample {
	static public void main(String[] args) throws Exception {
		String path = "/home/sebastian/r5/input/network.dat";
		TransportNetwork transportNetwork = TransportNetwork.read(new File(path));
		new R5Cleaner(transportNetwork).run();

		String day = "2017-09-25";
		String timezone = "+02:00";
		double beelineDistanceFactor = Math.sqrt(2.0);

		LatLonToCoordTransformation latLonToCoord = new DefaultLatLonToCoord(new WGS84toCH1903LV03Plus());
		CoordToLatLonTransformation coordToLatLon = new DefaultCoordToLatLon(new CH1903LV03PlustoWGS84());

		R5ItineraryScorer scorer = new SoonestArrivalTimeScorer();
		DistanceEstimator distanceEstimator = new CrowflyDistanceEstimator(latLonToCoord, beelineDistanceFactor);
		LinkFinder linkFinder = new UnknownLinkFinder();
		R5LegRouter router = new R5LegRouter(transportNetwork, scorer, distanceEstimator, day, timezone);
		
		RoutingModule routingModule = new R5TeleportationRoutingModule(router, coordToLatLon, latLonToCoord, null, linkFinder);

		Facility<?> fromFacility = new FakeFacility(new Coord(2721239.0, 1236409.0));
		Facility<?> toFacility = new FakeFacility(new Coord(2563424.0, 1208527.0));
		double departureTime = Time.parseTime("17:12:38");

		List<? extends PlanElement> plan = routingModule.calcRoute(fromFacility, toFacility, departureTime, null);

		for (PlanElement element : plan) {
			if (element instanceof Activity) {
				Activity activity = (Activity) element;
				System.out.println("ACT " + Time.writeTime(activity.getStartTime()) + " -> "
						+ Time.writeTime(activity.getEndTime()));
			}

			if (element instanceof Leg) {
				Leg leg = (Leg) element;
				System.out.println(
						"LEG " + leg.getMode() + " (" + String.format("%.2f", leg.getRoute().getDistance() / 1e3)
								+ "km) " + Time.writeTime(leg.getDepartureTime()) + " -> "
								+ Time.writeTime(leg.getDepartureTime() + leg.getTravelTime()));
			}
		}
	}
}
