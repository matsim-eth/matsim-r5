package ch.ethz.matsim.r5.example;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import ch.ethz.matsim.r5.matsim.R5ConfigGroup;
import ch.ethz.matsim.r5.matsim.R5Module;

public class Switzerland001Example {
	static public void main(String[] args) {
		R5ConfigGroup r5Config = new R5ConfigGroup();
		r5Config.setCoordinateSystem(TransformationFactory.CH1903_LV03_Plus);
		r5Config.setRequestDay("2017-09-25");
		r5Config.setRequestTimezone("+02:00");
		r5Config.setNetworkInputPath("/home/sebastian/r5/input/network.dat");
		
		Config config = ConfigUtils.loadConfig("/home/sebastian/baseline_scenario/data/output_config.xml", r5Config);

		config.global().setNumberOfThreads(8);
		config.qsim().setNumberOfThreads(8);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new R5Module());
		
		controler.run();
	}
}
