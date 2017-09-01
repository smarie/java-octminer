package com.rapidminer;

import java.io.InputStream;
import java.util.Properties;

import com.rapidminer.gui.MainFrame;

/**
 * This class provides hooks for initialization
 * 
 * @author Sebastian Land
 */
public class PluginInitOctaveExtension {
	
	public static void initGui(MainFrame mainframe) {}
	
	public static InputStream getOperatorStream(ClassLoader loader) {
		return null;
	}
	
	public static void initPluginManager() {}
	
	public static void initFinalChecks() {}
	
	public static void initSplashTexts() {}
	
	public static void initAboutTexts(Properties aboutBoxProperties) {}
	
	public static Boolean showAboutBox() {
		return true;
	}
}
