package ch.ethz.matsim.r5.matsim;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.conveyal.r5.transit.TransportNetwork;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import ch.ethz.matsim.r5.R5LegRouter;
import ch.ethz.matsim.r5.R5TeleportationRoutingModule;
import ch.ethz.matsim.r5.distance.CrowflyDistanceEstimator;
import ch.ethz.matsim.r5.distance.DistanceEstimator;
import ch.ethz.matsim.r5.route.LinkFinder;
import ch.ethz.matsim.r5.route.LoopLinkFinder;
import ch.ethz.matsim.r5.scoring.R5ItineraryScorer;
import ch.ethz.matsim.r5.scoring.SoonestArrivalTimeScorer;
import ch.ethz.matsim.r5.utils.R5Cleaner;
import ch.ethz.matsim.r5.utils.spatial.CoordToLatLonTransformation;
import ch.ethz.matsim.r5.utils.spatial.DefaultCoordToLatLon;
import ch.ethz.matsim.r5.utils.spatial.DefaultLatLonToCoord;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

public class R5Module extends AbstractModule {
	static public Logger logger = Logger.getLogger(R5Module.class);

	@Provides
	@Singleton
	public TransportNetwork provideTransportNetwork(R5ConfigGroup config) {
		try {
			logger.info("Loading R5 network ...");

			File inputFile = config.getNetworkInputPath().startsWith("/") ? new File(config.getNetworkInputPath())
					: new File(ConfigGroup.getInputFileURL(getConfig().getContext(), config.getNetworkInputPath())
							.getPath());
			
			TransportNetwork transportNetwork = TransportNetwork.read(inputFile);
			
			logger.info("Cleaning R5 network ...");
			new R5Cleaner(transportNetwork).run();

			return transportNetwork;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error while loading R5 network");
		}
	}

	@Provides
	@Singleton
	public LinkFinder provideLinkFinder(Network network, TransportNetwork transportNetwork,
			LatLonToCoordTransformation transformation) {
		LoopLinkFinder finder = new LoopLinkFinder(transportNetwork, transformation);
		finder.createStopLinks(network);
		return finder;
	}

	@Provides
	@Singleton
	public CoordToLatLonTransformation provideCoordToLatLon(R5ConfigGroup config) {
		return new DefaultCoordToLatLon(TransformationFactory.getCoordinateTransformation(config.getCoordinateSystem(),
				TransformationFactory.WGS84));
	}

	@Provides
	@Singleton
	public LatLonToCoordTransformation provideLatLonToCoord(R5ConfigGroup config) {
		return new DefaultLatLonToCoord(TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				config.getCoordinateSystem()));
	}

	@Provides
	@Singleton
	public R5ItineraryScorer provideItineraryScorer() {
		return new SoonestArrivalTimeScorer();
	}

	@Provides
	@Singleton
	public DistanceEstimator provideDistanceEstimator(LatLonToCoordTransformation latLonToCoord,
			PlansCalcRouteConfigGroup routeConfig) {
		double beelineDistanceFactor = routeConfig.getBeelineDistanceFactors().get("pt");
		return new CrowflyDistanceEstimator(latLonToCoord, beelineDistanceFactor);
	}

	@Provides
	@Singleton
	public R5LegRouter provideR5LegRouter(TransportNetwork transportNetwork, R5ItineraryScorer scorer,
			DistanceEstimator distanceEstimator, R5ConfigGroup config) {
		return new R5LegRouter(transportNetwork, scorer, distanceEstimator, config.getRequestDay(),
				config.getRequestTimezone());
	}

	@Provides
	@Singleton
	public R5TeleportationRoutingModule provideR5TeleportationRoutingModule(R5LegRouter router,
			CoordToLatLonTransformation coordToLatLon, LatLonToCoordTransformation latLonToCoord,
			@Named("walk") RoutingModule walkRoutingModule, LinkFinder linkFinder) {
		return new R5TeleportationRoutingModule(router, coordToLatLon, latLonToCoord, walkRoutingModule, linkFinder);
	}

	@Override
	public void install() {
		if (getConfig().transit().isUseTransit()) {
			throw new IllegalStateException("R5 is not compatible with network-based PT");
		}

		addRoutingModuleBinding("pt").to(R5TeleportationRoutingModule.class);
	}
}
