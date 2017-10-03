package ch.ethz.matsim.r5.route;

import java.util.Collections;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

import com.conveyal.r5.transit.TransportNetwork;
import com.vividsolutions.jts.geom.Coordinate;

import ch.ethz.matsim.r5.utils.spatial.LatLon;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

public class LoopLinkFinder implements LinkFinder {
	final private TransportNetwork transportNetwork;
	final private LatLonToCoordTransformation transformation;

	public LoopLinkFinder(TransportNetwork transportNetwork, LatLonToCoordTransformation transformation) {
		this.transportNetwork = transportNetwork;
		this.transformation = transformation;
	}

	public void createStopLinks(Network network) {
		int numberOfStops = transportNetwork.transitLayer.getStopCount();
		NetworkFactory factory = network.getFactory();

		for (int i = 0; i < numberOfStops; i++) {
			Coordinate latlon = transportNetwork.transitLayer.getCoordinateForStopFixed(i);

			// TODO: This is tricky. If the stop is not attached to a vertex in R5 we don't have
			// a coordinate, because they are not recovered from GTFS. For those cases we set the
			// coordinates to the center of the transitLayer. This should only have cosmetic 
			// implications for OTFVis (at least for the teleporting router), but not for the 
			// actual traffic.
			Coord coord = latlon != null ? transformation.transform(new LatLon(latlon.y, latlon.x))
					: transformation.transform(new LatLon(transportNetwork.transitLayer.centerLat,
							transportNetwork.transitLayer.centerLon));

			Node startNode = factory.createNode(Id.createNodeId("transit_start_" + i), coord);
			Node endNode = factory.createNode(Id.createNodeId("transit_end_" + i), coord);

			Link loopLink = factory.createLink(Id.createLinkId("transit_loop_" + i), startNode, endNode);
			loopLink.setAllowedModes(Collections.singleton("pt"));

			network.addNode(startNode);
			network.addNode(endNode);
			network.addLink(loopLink);
		}
	}

	@Override
	public Id<Link> findLink(String stopId) {
		return Id.createLinkId("transit_loop_" + transportNetwork.transitLayer.indexForStopId.get(stopId));
	}
}
