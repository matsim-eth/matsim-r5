package ch.ethz.matsim.r5.matsim;

import org.matsim.core.config.ReflectiveConfigGroup;

public class R5ConfigGroup extends ReflectiveConfigGroup {
	final static public String GROUP_NAME = "r5";

	final static public String COORDINATE_SYSTEM = "coordinateSystem";
	final static public String NETWORK_INPUT_PATH = "networkInputPath";
	final static public String REQUEST_DAY = "requestDay";
	final static public String REQUEST_TIMEZONE = "requestTimezone";

	private String coordinateSystem = null;
	private String networkInputPath = null;
	private String requestDay = "2017-09-25";
	private String requestTimezone = "+02:00";

	public R5ConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(NETWORK_INPUT_PATH)
	public String getNetworkInputPath() {
		return networkInputPath;
	}

	@StringSetter(NETWORK_INPUT_PATH)
	public void setNetworkInputPath(String networkInputPath) {
		this.networkInputPath = networkInputPath;
	}

	@StringGetter(REQUEST_DAY)
	public String getRequestDay() {
		return requestDay;
	}

	@StringSetter(REQUEST_DAY)
	public void setRequestDay(String requestDay) {
		this.requestDay = requestDay;
	}

	@StringGetter(REQUEST_TIMEZONE)
	public String getRequestTimezone() {
		return requestTimezone;
	}

	@StringSetter(REQUEST_TIMEZONE)
	public void setRequestTimezone(String requestTimezone) {
		this.requestTimezone = requestTimezone;
	}

	@StringGetter(COORDINATE_SYSTEM)
	public String getCoordinateSystem() {
		return coordinateSystem;
	}

	@StringSetter(COORDINATE_SYSTEM)
	public void setCoordinateSystem(String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

}
