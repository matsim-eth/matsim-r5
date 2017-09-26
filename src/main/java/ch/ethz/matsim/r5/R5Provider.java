package ch.ethz.matsim.r5;

import java.io.File;

import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import com.conveyal.r5.transit.TransportNetwork;
import com.google.inject.Provider;

import ch.ethz.matsim.r5.scoring.R5ItineraryScorer;
import ch.ethz.matsim.r5.scoring.SoonestArrivalTimeScorer;
import ch.ethz.matsim.r5.utils.R5Cleaner;
import ch.ethz.matsim.r5.utils.spatial.CoordToLatLonTransformation;
import ch.ethz.matsim.r5.utils.spatial.DefaultCoordToLatLon;
import ch.ethz.matsim.r5.utils.spatial.DefaultLatLonToCoord;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

public class R5Provider implements Provider<TransitRouter> {
	final private TransportNetwork transportNetwork;
	final private String day = "2017-09-25";
	final private String timezone = "+02:00";

	final private TransitSchedule transitSchedule;
	final private R5ItineraryScorer scorer;

	final private CoordToLatLonTransformation coordToLatLon;
	final private LatLonToCoordTransformation latLonToCoord;

	public R5Provider(String networkPath) throws Exception {
		transportNetwork = TransportNetwork.read(new File(networkPath));
		new R5Cleaner(transportNetwork).run();

		coordToLatLon = new DefaultCoordToLatLon(new CH1903LV03PlustoWGS84());
		latLonToCoord = new DefaultLatLonToCoord(new WGS84toCH1903LV03Plus());
		scorer = new SoonestArrivalTimeScorer();

		TransitScheduleFactory scheduleFactory = new TransitScheduleFactoryImpl();
		transitSchedule = scheduleFactory.createTransitSchedule();
	}

	@Override
	public TransitRouter get() {
		R5TransitRouter router = new R5TransitRouter(transportNetwork, transitSchedule, scorer, coordToLatLon,
				latLonToCoord, day, timezone);

		return router;
	}
}
