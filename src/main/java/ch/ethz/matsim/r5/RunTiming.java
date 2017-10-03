package ch.ethz.matsim.r5;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
import org.matsim.core.utils.misc.Time;

import com.conveyal.r5.transit.TransportNetwork;

import ch.ethz.matsim.r5.distance.CrowflyDistanceEstimator;
import ch.ethz.matsim.r5.distance.DistanceEstimator;
import ch.ethz.matsim.r5.route.R5Leg;
import ch.ethz.matsim.r5.scoring.R5ItineraryScorer;
import ch.ethz.matsim.r5.scoring.SoonestArrivalTimeScorer;
import ch.ethz.matsim.r5.utils.R5Cleaner;
import ch.ethz.matsim.r5.utils.spatial.CoordToLatLonTransformation;
import ch.ethz.matsim.r5.utils.spatial.DefaultCoordToLatLon;
import ch.ethz.matsim.r5.utils.spatial.DefaultLatLonToCoord;
import ch.ethz.matsim.r5.utils.spatial.LatLonToCoordTransformation;

public class RunTiming {
	static public void main(String[] args) throws Exception {
		String path = "/home/sebastian/r5/input/network.dat";
		TransportNetwork transportNetwork = TransportNetwork.read(new File(path));
		new R5Cleaner(transportNetwork).run();

		String day = "2017-09-25";
		String timezone = "+02:00";

		LatLonToCoordTransformation latLonToCoord = new DefaultLatLonToCoord(new WGS84toCH1903LV03Plus());
		CoordToLatLonTransformation coordToLatLon = new DefaultCoordToLatLon(new CH1903LV03PlustoWGS84());

		R5ItineraryScorer scorer = new SoonestArrivalTimeScorer();
		DistanceEstimator distanceEstimator = new CrowflyDistanceEstimator(latLonToCoord, 1.0);
		R5LegRouter router = new R5LegRouter(transportNetwork, scorer, distanceEstimator, day, timezone);

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream("/home/sebastian/temp/od_pairs.csv")));
		String line;

		ExecutorService executor = Executors.newFixedThreadPool(8);
		List<Future<Result>> futures = new LinkedList<>();

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream("/home/sebastian/temp/times.txt")));
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(";");

			String id = parts[0];

			Coord startCoord = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
			Coord endCoord = new Coord(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]));
			double departureTime = Double.parseDouble(parts[9]) * 60.0;

			futures.add(executor.submit(new Callable<Result>() {
				@Override
				public Result call() {
					List<R5Leg> legs = router.route(coordToLatLon.transform(startCoord),
							coordToLatLon.transform(endCoord), departureTime);

					if (legs == null) {
						return new Result(id);
					} else {
						double arrivalTime = legs.get(legs.size() - 1).getDepartureTime()
								+ legs.get(legs.size() - 1).getTravelTime();
						return new Result(id, departureTime, arrivalTime);
					}
				}
			}));
		}

		futures.forEach(f -> {
			try {
				Result result = f.get();

				if (!result.found) {
					System.err.println(result.id + " not found");
				} else {
					System.out.println(result.id + " " + Time.writeTime(result.arrivalTime - result.departureTime));
				}

				writer.write(String.format("%s;%f;%f\n", result.id, result.arrivalTime / 60.0,
						(result.arrivalTime - result.departureTime) / 60.0));
				writer.flush();

			} catch (InterruptedException | ExecutionException | IOException e) {
				throw new RuntimeException(e);
			}
		});

		reader.close();
		writer.close();

		executor.shutdown();
	}

	static public class Result {
		final public String id;
		final public double departureTime;
		final public double arrivalTime;
		final public boolean found;

		public Result(String id) {
			this.id = id;
			this.found = false;
			this.departureTime = Double.NaN;
			this.arrivalTime = Double.NaN;
		}

		public Result(String id, double departureTime, double arrivalTime) {
			this.id = id;
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;
			this.found = true;
		}
	}
}
