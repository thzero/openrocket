package net.sf.openrocket.file;

import java.io.InputStream;

import net.sf.openrocket.aerodynamics.WarningSet;

public interface RocketLoader {
	/**
	 * Loads a rocket from the specified InputStream.
	 */
	void load(DocumentLoadingContext context, InputStream source) throws RocketLoadException;

	WarningSet getWarnings();
}
