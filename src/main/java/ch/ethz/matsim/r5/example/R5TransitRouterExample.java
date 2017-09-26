package ch.ethz.matsim.r5.example;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import com.conveyal.r5.transit.TransportNetwork;

import ch.ethz.matsim.r5.R5TransitRouter;
import ch.ethz.matsim.r5.scoring.R5ItineraryScorer;
import ch.ethz.matsim.r5.scoring.SoonestArrivalTimeScorer;
import ch.ethz.matsim.r5.utils.R5Cleaner;
import ch.ethz.matsim.r5.utils.R5TransitScheduleBuilder;

public class R5TransitRouterExample {
	static public void main(String[] args) throws Exception {
		String path = "/home/sebastian/r5/input/network.dat";
		TransportNetwork transportNetwork = TransportNetwork.read(new File(path));
		new R5Cleaner(transportNetwork).run();

		CoordinateTransformation xyToLatLon = new CH1903LV03PlustoWGS84();
		CoordinateTransformation latLonToXY = new WGS84toCH1903LV03Plus();

		TransitScheduleFactory scheduleFactory = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = new R5TransitScheduleBuilder(scheduleFactory, latLonToXY)
				.build(transportNetwork);

		R5ItineraryScorer scorer = new SoonestArrivalTimeScorer();

		String day = "2017-09-25";
		String timezone = "+02:00";

		R5TransitRouter router = new R5TransitRouter(transportNetwork, schedule, scorer, xyToLatLon, latLonToXY, day,
				timezone);

		Network network = NetworkUtils.createNetwork();

		Node startNode = network.getFactory().createNode(Id.createNodeId("start"),
				latLonToXY.transform(new Coord(8.5591612, 47.3762586)));
		Node endNode = network.getFactory().createNode(Id.createNodeId("end"),
				latLonToXY.transform(new Coord(8.574383, 47.372402)));

		Link startLink = network.getFactory().createLink(Id.createLinkId("start"), startNode, startNode);
		Link endLink = network.getFactory().createLink(Id.createLinkId("end"), endNode, endNode);

		LinkWrapperFacility fromFacility = new LinkWrapperFacility(startLink);
		LinkWrapperFacility toFacility = new LinkWrapperFacility(endLink);
		
		double departureTime = 8.0 * 3600.0;

		List<Leg> route = router.calcRoute(fromFacility, toFacility, departureTime, null);

		//new TransitRouterWrapper(router, schedule, network, walkRouter)
		//new TransitRouterWrapper(router, schedule, network, null).calcRoute(fromFacility, toFacility, departureTime, null);

		if (route == null) {
			System.err.println("No route found");
		} else {
			System.out.println("Length: " + route.size());

			for (Leg leg : route) {
				System.out.println(String.format("Mode: %s, Start: %s, Duration: %s, %s", leg.getMode(),
						Time.writeTime(leg.getDepartureTime()), Time.writeTime(leg.getTravelTime()),
						leg.getRoute().getRouteDescription()));
			}
		}
	}
}
