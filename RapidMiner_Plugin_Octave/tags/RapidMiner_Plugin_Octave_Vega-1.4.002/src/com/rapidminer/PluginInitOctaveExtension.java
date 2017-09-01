package com.rapidminer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.parameter.ParameterTypeDirectory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.ParameterService;

/**
 * This class provides hooks for initialization
 * 
 * @author Sebastian Land
 */
public class PluginInitOctaveExtension {

	public static Log log = LogFactory
			.getLog("com.rapidminer.PluginInitOctaveExtension");

	public static final String PROPERTY_OCTAVE_NB_ENGINES = "rapidminer.octave.engine.nb";
	public static final String PROPERTY_OCTAVE_OCTAVEPATH = "rapidminer.octave.engine.path";
	public static final String PROPERTY_OCTAVE_OPTIONS = "rapidminer.octave.engine.startup_options";
	public static final String PROPERTY_OCTAVE_M_FILEPATH = "rapidminer.octave.supportfunctions.path";
	public static final String OCTAVECONFIGURATION = "Octave";
	private static File userHomeFolderPath;
	public static String pathToSupportFunctionsFolder;
	private static File copy;

	/**
	 * Set the
	 * 
	 * @throws IOException
	 */
	public static void initPlugin() throws IOException {

		// register the parameters of the plugin
		ParameterService.registerParameter(new ParameterTypeFile(
				PROPERTY_OCTAVE_OCTAVEPATH,
				"Choose the Octave executable to use ", "*", true),
				OCTAVECONFIGURATION);
		ParameterService
				.registerParameter(
						new ParameterTypeString(
								PROPERTY_OCTAVE_OPTIONS,
								"<html>Choose the program Octave options: Automatically add <br/> \"-no-history,-no-init-file,-no-line-editing,-no-site-file,-silent\"</html>",
								null, true), OCTAVECONFIGURATION);
		ParameterService
				.registerParameter(
						new ParameterTypeInt(
								PROPERTY_OCTAVE_NB_ENGINES,
								"Nb of concurrent octave engines to maintain in the pool. Default value is 2",
								1, 100, 2, true), OCTAVECONFIGURATION);

		// unzip the support files in user home directory
		pathToSupportFunctionsFolder = installSupportFunctionsFolder();

		ParameterService.registerParameter(new ParameterTypeDirectory(
				PROPERTY_OCTAVE_M_FILEPATH,
				"Choose the path of the .m support functions (by default "
						+ pathToSupportFunctionsFolder + ")",
				pathToSupportFunctionsFolder), OCTAVECONFIGURATION);
		
		
	}

	/**
	 * Extracts the zip file containing the .m support functions into
	 * (user.home)/RapidminerOctave/ directory, and returns its folder path
	 * 
	 * @return
	 * @throws IOException
	 */
	private static String installSupportFunctionsFolder() throws IOException {
		String userHome = System.getProperty("user.home");
		userHomeFolderPath = new File(userHome + "/RapidminerOctave/");
		userHomeFolderPath.mkdirs();
		int buff = 2048;
		String pathToSupportFunctionsFolder = null;

		ZipFile zipFile;

		// 1. The version number is in the supportfunctions.version file
		InputStream versionFile = PluginInitOctaveExtension.class
				.getClassLoader().getResourceAsStream(
						"supportfunctions.version");
		Properties p = new Properties();
		p.load(versionFile);
		String supportFileVersion = p.getProperty("version");

		// 2. the version number is used to get the file name right
		URL zipInJarURL = PluginInitOctaveExtension.class.getClassLoader()
				.getResource(
						"RapidMiner_Plugin_Octave_Vega_SupportFunctions-"
								+ supportFileVersion + ".zip");

		// 3. copy zip file in userHomeFolderPath
		FileOutputStream fos = null;
		InputStream zipInJar = null;
		try {
			zipInJar = zipInJarURL.openStream();
			int index = zipInJarURL.getFile().lastIndexOf("/");
			String fileName = zipInJarURL.getFile().substring(index);
			copy = new File(userHomeFolderPath, fileName);

			if (copy.exists()) {
				if (log.isWarnEnabled())
					log.warn("Found an existing version of the support functions "
							+ "zip file, deleting it first");
				copy.delete();
			}
			fos = new FileOutputStream(copy);
			while (zipInJar.available() > 0) { // write contents of 'is' to
												// 'fos'
				fos.write(zipInJar.read());
			}
		} finally {
			if (fos != null)
				fos.close();
			if (zipInJar != null)
				zipInJar.close();
		}

		// unzip it, store the name of the created folder (mFolderName) and
		// delete the zip.
		zipFile = new ZipFile(copy);
		Enumeration zipFileEntries = zipFile.entries();
		File destFolder = new File(userHomeFolderPath,
				"RapidMiner_Plugin_Octave_Vega_SupportFunctions-"
						+ supportFileVersion);
		if (destFolder.exists()) {
			if (log.isWarnEnabled())
				log.warn("Found an existing version of the support functions "
						+ "folder, deleting it first");
			destFolder.delete();
		}

		destFolder.mkdirs();

		try {
			while (zipFileEntries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

				String currentEntry = entry.getName();
				File destFile = new File(destFolder, currentEntry);
				// set the M folder
				if (entry.isDirectory()) {
					destFile.mkdir();
					String t = destFile.toString();
					if (t.length() - 1 == t.lastIndexOf("m")) {
						pathToSupportFunctionsFolder = destFile.toString();
					}
				} else {
					BufferedInputStream mfilebuffe = null;
					BufferedOutputStream dest = null;
					try {
						mfilebuffe = new BufferedInputStream(
								zipFile.getInputStream(entry));
						int currentByte;
						// establish buffer for writing file
						byte data[] = new byte[buff];

						// write the current file to disk
						fos = new FileOutputStream(destFile);
						dest = new BufferedOutputStream(fos, buff);

						// read and write until last byte is encountered
						while ((currentByte = mfilebuffe.read(data, 0, buff)) != -1) {
							dest.write(data, 0, currentByte);
						}
						dest.flush();
					} finally {
						if (dest != null)
							dest.close();
						if (mfilebuffe != null)
							mfilebuffe.close();
					}

				}
			}
		} finally {
			if (zipFile != null)
				zipFile.close();
		}

		// finally delete the zip file in case of success (leave it in case of
		// exception so that we can debug)
		copy.delete();

		return pathToSupportFunctionsFolder;
	}

	public static void initGui(MainFrame mainframe) {
	}

	public static InputStream getOperatorStream(ClassLoader loader) {
		return null;
	}

	public static void initPluginManager() {
	}

	public static void initFinalChecks() {
	}

	public static void initSplashTexts() {
	}

	public static void initAboutTexts(Properties aboutBoxProperties) {
	}

	public static Boolean showAboutBox() {
		return true;
	}

	/**
	 * @return the path of the folder created by the plugin in User Home. In
	 *         this folder there is one subfolder with the support functions,
	 *         and other subfolders can be created.
	 */
	public static String getUserHomeFolderPath() {
		return userHomeFolderPath.getAbsolutePath();
	}
}
