package ch.ethz.matsim.r5.route;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface LinkFinder {
	Id<Link> findLink(String stopId);
}
