package ch.ethz.matsim.r5.example;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.conveyal.r5.point_to_point.PointToPointRouterServer;
import com.conveyal.r5.transit.TransportNetwork;

import ch.ethz.matsim.r5.matsim.R5ConfigGroup;
import ch.ethz.matsim.r5.matsim.R5Module;
import ch.ethz.matsim.sioux_falls.SiouxFallsUtils;

public class SiouxFallsExample {
	static public void main(String[] args) throws Exception {
		// Download OSM + GTFS and create R5 network if it does not exist yet
		File dataDirectory = new File("sf-data");
		File networkPath = new File("sf-data/network.dat");
		prepareR5(dataDirectory, networkPath);

		// R5 for MATSim setup

		R5ConfigGroup r5Config = new R5ConfigGroup();
		r5Config.setCoordinateSystem("EPSG:26914");
		r5Config.setRequestDay("2015-09-22");
		r5Config.setRequestTimezone("-05:00");
		r5Config.setNetworkInputPath(networkPath.getAbsolutePath());
		
		// Standard MATSim setup

		Config config = ConfigUtils.loadConfig(SiouxFallsUtils.getConfigURL(), r5Config);
		config.transit().setUseTransit(false);
		config.global().setNumberOfThreads(8);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new R5Module()); // Add R5 module

		controler.run();
	}
	
	static private void prepareR5(File dataDirectory, File networkPath) throws IOException {
		if (!networkPath.exists()) {
			dataDirectory.mkdir();

			FileUtils.forceMkdir(new File("sf-data"));

			System.out.println("Downloading GTFS ...");
			FileUtils.copyURLToFile(new URL("https://transitfeeds.com/p/sioux-area-metro/361/latest/download"),
					new File(dataDirectory, "gtfs.zip"));
			
			System.out.println("Downloading OSM ...");
			FileUtils.copyURLToFile(new URL("http://overpass-api.de/api/map?bbox=-96.8026,43.4818,-96.6807,43.6199"),
					new File(dataDirectory, "sf.osm"));
			
			System.out.println("Converting GTFS ...");
			Runtime.getRuntime().exec("osmconvert sf.osm --out-pbf >sf.osm.pbf", new String[] {}, dataDirectory);

			System.out.println("Create R5 network ...");
			TransportNetwork transportNetwork = TransportNetwork.fromDirectory(dataDirectory);
			transportNetwork.write(networkPath);
			
			System.out.println("Preparation done.");
		} else {
			System.out.println("R5 network exists. Preparation done.");
		}
	}
}
