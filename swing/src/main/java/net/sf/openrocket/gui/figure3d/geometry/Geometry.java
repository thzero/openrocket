package net.sf.openrocket.gui.figure3d.geometry;

import com.jogamp.opengl.GL2;

public interface Geometry {
	public static enum Surface {
		ALL, OUTSIDE, INSIDE, EDGES;
	}
	
	public void render(GL2 gl);
}
