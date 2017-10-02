package ch.ethz.matsim.r5.example;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import com.conveyal.r5.transit.TransportNetwork;

import ch.ethz.matsim.r5.R5TransitRouter;
import ch.ethz.matsim.r5.distance.CrowflyDistanceEstimator;
import ch.ethz.matsim.r5.distance.DistanceEstimator;
import ch.ethz.matsim.r5.route.R5Leg;
import ch.ethz.matsim.r5.scoring.R5ItineraryScorer;
import ch.ethz.matsim.r5.scoring.SoonestArrivalTimeScorer;
import ch.ethz.matsim.r5.utils.R5Cleaner;
import ch.ethz.matsim.r5.utils.spatial.CoordToLatLonTransformation;
import ch.ethz.matsim.r5.utils.spatial.DefaultCoordToLatLon;
import ch.ethz.matsim.r5.utils.spatial.DefaultLatLonToCoord;
import ch.ethz.matsim.r5.utils.spatial.LatLon;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

public class R5TransitRouterExample {
	static public void main(String[] args) throws Exception {
		String path = "/home/sebastian/r5/input/network.dat";
		TransportNetwork transportNetwork = TransportNetwork.read(new File(path));
		new R5Cleaner(transportNetwork).run();

		String day = "2017-09-25";
		String timezone = "+02:00";
		
		LatLonToCoordTransformation latLonToCoord = new DefaultLatLonToCoord(new WGS84toCH1903LV03Plus());
		R5ItineraryScorer scorer = new SoonestArrivalTimeScorer();
		DistanceEstimator distanceEstimator = new CrowflyDistanceEstimator(latLonToCoord);
		R5TransitRouter router = new R5TransitRouter(transportNetwork, scorer, distanceEstimator, day, timezone);

		LatLon fromLocation = new LatLon(47.384868612482634, 8.495950698852539);
		LatLon toLocation = new LatLon(47.39974354712813, 8.465995788574219);
		double departureTime = 8.0 * 3600.0;

		List<R5Leg> route = router.calcRoute(fromLocation, toLocation, departureTime, null);

		for (R5Leg leg : route) {
			System.out.println(String.format("Mode: %s, Start: %s, Duration: %s", leg.getClass().toString(),
					Time.writeTime(leg.getDepartureTime()), Time.writeTime(leg.getTravelTime())));
		}
	}
}
