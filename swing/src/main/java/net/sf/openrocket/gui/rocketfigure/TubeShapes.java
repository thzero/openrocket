package net.sf.openrocket.gui.rocketfigure;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import net.sf.openrocket.util.Coordinate;
import net.sf.openrocket.util.Transformation;


public class TubeShapes extends RocketComponentShape {
	
	public static Shape getShapesSide(
			Transformation transformation,
			Coordinate instanceAbsoluteLocation,
			final double length, final double radius ){
	    
	    return new Rectangle2D.Double((instanceAbsoluteLocation.x),    //x - the X coordinate of the upper-left corner of the newly constructed Rectangle2D
			(instanceAbsoluteLocation.y-radius), // y - the Y coordinate of the upper-left corner of the newly constructed Rectangle2D
			length, // w - the width of the newly constructed Rectangle2D
			2*radius); //  h - the height of the newly constructed Rectangle2D
	}
	
	public static Shape getShapesBack(
			Transformation transformation,
			Coordinate instanceAbsoluteLocation, 
			final double radius ) {
		
		return new Ellipse2D.Double((instanceAbsoluteLocation.z-radius), (instanceAbsoluteLocation.y-radius), 2*radius, 2*radius);
	}
	
	
}
