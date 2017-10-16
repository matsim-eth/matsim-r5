package ch.ethz.matsim.r5.example;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;

import com.conveyal.r5.transit.TransportNetwork;

import ch.ethz.matsim.r5.R5LegRouter;
import ch.ethz.matsim.r5.R5TeleportationRoutingModule;
import ch.ethz.matsim.r5.distance.CrowflyDistanceEstimator;
import ch.ethz.matsim.r5.distance.DistanceEstimator;
import ch.ethz.matsim.r5.route.LinkFinder;
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

		String day = "2017-10-03";
		String timezone = "+02:00";
		double beelineDistanceFactor = Math.sqrt(2.0);

		LatLonToCoordTransformation latLonToCoord = new DefaultLatLonToCoord(new WGS84toCH1903LV03Plus());
		CoordToLatLonTransformation coordToLatLon = new DefaultCoordToLatLon(new CH1903LV03PlustoWGS84());

		R5ItineraryScorer scorer = new SoonestArrivalTimeScorer();
		DistanceEstimator distanceEstimator = new CrowflyDistanceEstimator(latLonToCoord, beelineDistanceFactor);
		LinkFinder linkFinder = new UnknownLinkFinder();
		R5LegRouter router = new R5LegRouter(transportNetwork, scorer, distanceEstimator, day, timezone);

		RoutingModule routingModule = new R5TeleportationRoutingModule(router, coordToLatLon, latLonToCoord, null,
				linkFinder);

		Facility<?> fromFacility = new FakeFacility(
				latLonToCoord.transform(new LatLon(46.85639004531476, 7.070712492578848)));
		Facility<?> toFacility = new FakeFacility(
				latLonToCoord.transform(new LatLon(47.02627488630519, 6.956391395823911)));
		double departureTime = 61958.0;
		
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

	static public class FakeFacility implements Facility {
		private Coord coord;

		public FakeFacility(Coord coord) {
			this.coord = coord;
		}

		@Override
		public Coord getCoord() {
			return this.coord;
		}

		@Override
		public Id getId() {
			return Id.create("unknown", Facility.class);
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return Collections.emptyMap();
		}

		@Override
		public Id getLinkId() {
			// TODO Auto-generated method stub
			return Id.createLinkId("unknown");
		}
	}
}
