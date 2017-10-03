package ch.ethz.matsim.r5.route;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class UnknownLinkFinder implements LinkFinder {
	@Override
	public Id<Link> findLink(String stopId) {
		return Id.createLinkId("unknown");
	}
}
