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

		CoordToLatLonTransformation coordToLatLon = new DefaultCoordToLatLon(new CH1903LV03PlustoWGS84());
		LatLonToCoordTransformation latLonToCoord = new DefaultLatLonToCoord(new WGS84toCH1903LV03Plus());

		TransitScheduleFactory scheduleFactory = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = scheduleFactory.createTransitSchedule();

		R5ItineraryScorer scorer = new SoonestArrivalTimeScorer();

		String day = "2017-09-25";
		String timezone = "+02:00";

		R5TransitRouter router = new R5TransitRouter(transportNetwork, schedule, scorer, coordToLatLon, latLonToCoord,
				day, timezone);

		Network network = NetworkUtils.createNetwork();

		Node startNode = network.getFactory().createNode(Id.createNodeId("start"),
				latLonToCoord.transform(new LatLon(47.384868612482634, 8.495950698852539)));
		Node endNode = network.getFactory().createNode(Id.createNodeId("end"),
				latLonToCoord.transform(new LatLon(47.39974354712813, 8.465995788574219)));

		Link startLink = network.getFactory().createLink(Id.createLinkId("start"), startNode, startNode);
		Link endLink = network.getFactory().createLink(Id.createLinkId("end"), endNode, endNode);

		LinkWrapperFacility fromFacility = new LinkWrapperFacility(startLink);
		LinkWrapperFacility toFacility = new LinkWrapperFacility(endLink);

		double departureTime = 8.0 * 3600.0;

		List<Leg> route = router.calcRoute(fromFacility, toFacility, departureTime, null);

		for (Leg leg : route) {
			System.out.println(String.format("Mode: %s, Start: %s, Duration: %s, %s", leg.getMode(),
					Time.writeTime(leg.getDepartureTime()), Time.writeTime(leg.getTravelTime()),
					leg.getRoute().getRouteDescription()));
		}
	}
}
