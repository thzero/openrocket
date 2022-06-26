package net.sf.openrocket.file;

import java.util.List;

import net.sf.openrocket.aerodynamics.Warning;
import net.sf.openrocket.aerodynamics.WarningSet;
import net.sf.openrocket.motor.Motor;
import net.sf.openrocket.motor.Motor.Type;
import net.sf.openrocket.startup.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MotorFinder implementation that searches the thrust curve motor database
 * for a motor.
 * 
 * @author Sampo Niskanen <sampo.niskanen@iki.fi>
 */
public class DatabaseMotorFinder implements MotorFinder {
	private static final Logger log = LoggerFactory.getLogger(DatabaseMotorFinder.class);
	
	/**
	 * Do something when a missing motor is found.
	 * 
	 * This implementation adds a Warning.MissingMotor to the warning set and returns null.
	 * 
	 * Override this function to change the behavior.
	 * 
	 * @return The Motor which will be put in the Rocket.
	 */
	protected Motor handleMissingMotor(Type type, String manufacturer, String designation, double diameter, double length, String digest, WarningSet warnings) {
		Warning.MissingMotor mmw = new Warning.MissingMotor();
		mmw.setDesignation(designation);
		mmw.setDigest(digest);
		mmw.setDiameter(diameter);
		mmw.setLength(length);
		mmw.setManufacturer(manufacturer);
		mmw.setType(type);
		warnings.add(mmw);
		return null;
	}
	
	@Override
	public Motor findMotor(Type type, String manufacturer, String designation, double diameter, double length, String digest, WarningSet warnings) {
		log.debug("type " + type + ", manufacturer " + manufacturer + ", designation " + designation + ", diameter " +  diameter + ", length " + length + ", digest " +  digest + ", warnings " +  warnings);
		
		if (designation == null) {
			warnings.add(Warning.fromString("No motor specified, ignoring."));
			return null;
		}
		
		List<? extends Motor> motors = Application.getMotorSetDatabase().findMotors(digest, type, manufacturer, designation, diameter, length);
		// No motors
		if (motors.size() == 0) {
			return handleMissingMotor(type, manufacturer, designation, diameter, length, digest, warnings);
		}

		StringBuilder builder = new StringBuilder();

		// One motor
		if (motors.size() == 1) {
			Motor motor = motors.get(0);
			log.debug("motor is " + motor.getDesignation());

			if (digest != null && !digest.equals(motor.getDigest())) {
				generateWarningDifferentThrustCurve(builder, designation, manufacturer);
				warnings.add(builder.toString());
			}

			return motor;
		}
		
		// Multiple motors, check digest for which one to use
		if (digest != null) {
			// Check for motor with correct digest
			for (Motor m : motors) {
				if (digest.equals(m.getDigest())) {
					return m;
				}
			}

			generateWarningDifferentThrustCurve(builder, designation, manufacturer);
			warnings.add(builder.toString());
		} else {
			generateWarningMultipleMotorsWithDesignation(builder, designation, manufacturer);
			warnings.add(builder.toString());
		}

		return motors.get(0);
	}

	private void generateWarningDifferentThrustCurve(StringBuilder builder, String designation, String manufacturer) {
		generateWarning(builder, designation, manufacturer, "has differing thrust curve than the original");
	}

	private void generateWarningMultipleMotorsWithDesignation(StringBuilder builder, String designation, String manufacturer) {
		builder.delete(0, 0);
		builder.append("Multiple motors with designation '").append(designation).append("'");
		if (manufacturer != null) {
			builder.append(" for manufacturer '").append(manufacturer).append("'");
		}
		builder.append(" found, one chosen arbitrarily.");
	}

	private void generateWarning(StringBuilder builder, String designation, String manufacturer, String warning) {
		builder.delete(0, 0);
		builder.append("Motor with designation '").append(designation).append("'");
		if (manufacturer != null) {
			builder.append(" for manufacturer '").append(manufacturer).append("'");
		}
		builder.append(" ").append(warning).append(".");
	}
}
