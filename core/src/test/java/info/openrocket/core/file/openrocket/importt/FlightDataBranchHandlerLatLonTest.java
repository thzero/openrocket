package info.openrocket.core.file.openrocket.importt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import info.openrocket.core.document.OpenRocketDocument;
import info.openrocket.core.document.Simulation;
import info.openrocket.core.file.GeneralRocketLoader;
import info.openrocket.core.file.RocketLoadException;
import info.openrocket.core.simulation.FlightDataBranch;
import info.openrocket.core.simulation.FlightDataType;
import info.openrocket.core.util.BaseTestCase;

/**
 * ITAR: the rocket's geographic latitude/longitude flight-path position is no longer exposed.
 * Legacy .ork files still contain {@code latitude}/{@code longitude} columns in their stored
 * simulation data; these must be dropped on load so the coordinates are neither displayed,
 * exported, nor written back out on the next save. The surrounding columns must still load
 * correctly despite the dropped columns (comma alignment is preserved).
 */
public class FlightDataBranchHandlerLatLonTest extends BaseTestCase {

	private static final double EPSILON = 1e-9;

	@Test
	public void latLonColumnsAreStrippedFromLegacyFiles() throws RocketLoadException {
		// A minimal legacy .ork whose stored flight data has latitude/longitude between and after
		// other columns, to prove the drop keeps the remaining values aligned.
		String xml = """
				<?xml version="1.0" encoding="utf-8"?>
				<openrocket version="1.10" creator="OpenRocket test">
				  <rocket>
				    <name>Lat/lon scrub</name>
				    <subcomponents>
				      <stage>
				        <name>Sustainer</name>
				        <subcomponents>
				          <bodytube>
				            <name>Body</name>
				            <length>0.3</length>
				            <thickness>0.001</thickness>
				            <radius>0.02</radius>
				          </bodytube>
				        </subcomponents>
				      </stage>
				    </subcomponents>
				  </rocket>
				  <simulations>
				    <simulation status="loaded">
				      <name>Sim</name>
				      <simulator>RK4Simulator</simulator>
				      <calculator>BarrowmanCalculator</calculator>
				      <conditions>
				        <configid>01234567-89ab-cdef-0123-456789abcdef</configid>
				        <launchrodlength>1.0</launchrodlength>
				      </conditions>
				      <flightdata maxaltitude="120.0">
				        <databranch name="Main" types="time,latitude,altitude,longitude">
				          <datapoint>0.0,52.0,0.0,4.0</datapoint>
				          <datapoint>0.5,52.1,120.0,4.1</datapoint>
				        </databranch>
				      </flightdata>
				    </simulation>
				  </simulations>
				</openrocket>
				""";

		GeneralRocketLoader loader = new GeneralRocketLoader((File) null);
		ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		OpenRocketDocument document = loader.load(input, "lat-lon-scrub");

		assertEquals(1, document.getSimulations().size());
		Simulation simulation = document.getSimulations().get(0);
		assertNotNull(simulation.getSimulatedData());
		FlightDataBranch branch = simulation.getSimulatedData().getBranch(0);

		// No latitude/longitude type survives, by save key or by (localized) display name.
		for (FlightDataType type : branch.getTypes()) {
			assertFalse("latitude".equals(type.getSaveKey()), "latitude column should have been dropped");
			assertFalse("longitude".equals(type.getSaveKey()), "longitude column should have been dropped");
			assertFalse("Latitude".equalsIgnoreCase(type.getName()), "latitude column should have been dropped");
			assertFalse("Longitude".equalsIgnoreCase(type.getName()), "longitude column should have been dropped");
		}

		// The surviving columns kept their (correctly aligned) values.
		assertTrue(branch.getTypes().length >= 2);
		assertEquals(2, branch.getLength());
		assertEquals(0.0, branch.getByIndex(FlightDataType.TYPE_TIME, 0), EPSILON);
		assertEquals(0.5, branch.getByIndex(FlightDataType.TYPE_TIME, 1), EPSILON);
		assertEquals(0.0, branch.getByIndex(FlightDataType.TYPE_ALTITUDE, 0), EPSILON);
		assertEquals(120.0, branch.getByIndex(FlightDataType.TYPE_ALTITUDE, 1), EPSILON);
	}
}
