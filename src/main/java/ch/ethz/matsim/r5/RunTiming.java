package ch.ethz.matsim.r5;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import com.conveyal.r5.transit.TransportNetwork;

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
		
		CoordToLatLonTransformation coordToLatLon = new DefaultCoordToLatLon(new CH1903LV03PlustoWGS84());
		LatLonToCoordTransformation latLonToCoord = new DefaultLatLonToCoord(new WGS84toCH1903LV03Plus());

		TransitScheduleFactory scheduleFactory = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = scheduleFactory.createTransitSchedule();

		R5ItineraryScorer scorer = new SoonestArrivalTimeScorer();
		
		String day = "2017-09-25";
		String timezone = "+02:00";

		R5TransitRouter router = new R5TransitRouter(transportNetwork, schedule, scorer, coordToLatLon, latLonToCoord,
				day, timezone);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/sebastian/temp/od_pairs.csv")));
		String line;
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/sebastian/temp/times.txt")));
		
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(";");
			
			String id = parts[0];
			Coord startCoord = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
			Coord endCoord = new Coord(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]));
			double departureTime = Double.parseDouble(parts[9]) * 60.0;
			
			List<Leg> legs = router.calcRoute(new FakeFacility(startCoord), new FakeFacility(endCoord), departureTime, null);
			
			if (legs == null) {
				System.err.println("not found");
			} else {
				double arrivalTime = legs.get(0).getTravelTime();
				
				System.out.println(Time.writeTime(arrivalTime - departureTime));
				
				writer.write(String.format("%s;%f;%f\n", id, arrivalTime / 60.0, (arrivalTime - departureTime) / 60.0));
				writer.flush();
			}
		}
		
		reader.close();
		writer.close();		
	}
}
