package net.sf.openrocket;

import com.google.inject.AbstractModule;
import net.sf.openrocket.material.Material;
import net.sf.openrocket.preset.ComponentPreset;
import net.sf.openrocket.preset.ComponentPreset.Type;
import net.sf.openrocket.startup.Preferences;

import java.util.Collections;
import java.util.Set;
import java.util.prefs.BackingStoreException;

public class ServicesForTesting extends AbstractModule {

	@Override
	protected void configure() {
		bind(Preferences.class).to(PreferencesForTesting.class);
	}

	public static class PreferencesForTesting extends Preferences {

		private static java.util.prefs.Preferences root = null;

		@Override
		public boolean getBoolean(String key, boolean defaultValue) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void putBoolean(String key, boolean value) {
			// TODO Auto-generated method stub

		}

		@Override
		public int getInt(String key, int defaultValue) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void putInt(String key, int value) {
			// TODO Auto-generated method stub

		}

		@Override
		public double getDouble(String key, double defaultValue) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void putDouble(String key, double value) {
			// TODO Auto-generated method stub

		}

		@Override
		public String getString(String key, String defaultValue) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void putString(String key, String value) {
			// TODO Auto-generated method stub

		}

		@Override
		public String getString(String directory, String key, String defaultValue) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void putString(String directory, String key, String value) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addUserMaterial(Material m) {
			// TODO Auto-generated method stub

		}

		@Override
		public Set<Material> getUserMaterials() {
			return Collections.<Material> emptySet();
		}

		@Override
		public void removeUserMaterial(Material m) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setComponentFavorite(ComponentPreset preset, Type type, boolean favorite) {
			// TODO Auto-generated method stub

		}

		@Override
		public Set<String> getComponentFavorites(Type type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public java.util.prefs.Preferences getNode(String nodeName) {
			return getBaseNode().node(nodeName);
		}

		private java.util.prefs.Preferences getBaseNode() {
			if (root == null) {
				final String name = "OpenRocket-unittest-" + System.currentTimeMillis();
				root = java.util.prefs.Preferences.userRoot().node(name);
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {
							root.removeNode();
						} catch (BackingStoreException e) {
							e.printStackTrace();
						}
					}
				});
			}
			return root;
		}

	}
}
