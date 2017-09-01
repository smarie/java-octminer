/*
 *  				  RapidMiner Octave Extension.
 *				
 * Copyright (C) 2012-present by Schneider Electric Industries SAS.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of Schneider Electric nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * For more information on this software, see http://www.java.net/projects/octminer.
 */
package com.rapidminer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;

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
 * This class provides hooks for initialization. In particular it registers four
 * main parameters:
 * <ul>
 * <li> {@link #PROPERTY_OCTAVE_NB_ENGINES} the number of parallel Octave engines
 * in the pool
 * <li> {@link #PROPERTY_OCTAVE_OCTAVEPATH} the path to Octave executable
 * <li> {@link #PROPERTY_OCTAVE_OPTIONS} the additional Octave startup options
 * <li> {@link #PROPERTY_OCTAVE_M_FILEPATH} the path to the support m-functions
 * (automatically installed by the extension at rapidminer startup)
 * </ul>
 * 
 * @author Sylvain Marié
 * @author Yaoyu Zhang
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
	 * Set the main parameters
	 * 
	 * @throws IOException
	 */
	public static void initPlugin() throws IOException {

		// register the parameters of the plugin
		ParameterService.registerParameter(new ParameterTypeFile(
				PROPERTY_OCTAVE_OCTAVEPATH,
				"Choose the Octave executable to use ", "*", true));
		ParameterService
				.registerParameter(new ParameterTypeString(
						PROPERTY_OCTAVE_OPTIONS,
						"<html>Choose the program Octave options: Automatically add <br/> \"-no-history,-no-init-file,-no-line-editing,-no-site-file,-silent\"</html>",
						null, true));
		ParameterService
				.registerParameter(new ParameterTypeInt(
						PROPERTY_OCTAVE_NB_ENGINES,
						"Nb of concurrent octave engines to maintain in the pool. Default value is 2",
						1, 100, 2, true));

		// unzip the support files in user home directory
		pathToSupportFunctionsFolder = installSupportFunctionsFolder();

		ParameterService.registerParameter(new ParameterTypeDirectory(
				PROPERTY_OCTAVE_M_FILEPATH,
				"Choose the path of the .m support functions (by default "
						+ pathToSupportFunctionsFolder + ")",
				pathToSupportFunctionsFolder));
		
		// always reset to the correct value here
		ParameterService.setParameterValue(PROPERTY_OCTAVE_M_FILEPATH, pathToSupportFunctionsFolder);
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
