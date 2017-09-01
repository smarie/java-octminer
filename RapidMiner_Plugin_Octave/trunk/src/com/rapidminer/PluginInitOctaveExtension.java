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

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rapid_i.deployment.update.client.ManagedExtension;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.RapidMiner.ExitMode;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.parameter.ParameterTypeDirectory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeLong;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.octave.manager.OctaveConnectionManager;

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

    private static final Logger LOGGER = Logger
            .getLogger(PluginInitOctaveExtension.class.getName());
    public static Log log = LogFactory
            .getLog("com.rapidminer.PluginInitOctaveExtension");

    public static final String PROPERTY_OCTAVE_NB_ENGINES = "rapidminer.octave.engine.nb";
    public static final String PROPERTY_OCTAVE_OCTAVEPATH = "rapidminer.octave.engine.path";
    public static final String PROPERTY_OCTAVE_OPTIONS = "rapidminer.octave.engine.startup_options";
    public static final String PROPERTY_OCTAVE_READ_TIMEOUT_SECONDS = "rapidminer.octave.engine.read_timeout_seconds";
    public static final String PROPERTY_OCTAVE_WRITE_TIMEOUT_SECONDS = "rapidminer.octave.engine.write_timeout_seconds";
    public static final String PROPERTY_OCTAVE_M_FILEPATH = "rapidminer.octave.supportfunctions.path";
    public static final String OCTAVECONFIGURATION = "Octave";
    private static File userHomeFolderPath;
    public static String pathToSupportFunctionsFolder;
    private static File copy;

    private static Throwable octaveLoadingExecption = null;
    private static boolean octaveRunSuccess = false;

    /**
     * Registers the main parameters for the extension. Then installs the .m
     * support functions if needed. Finally tries to load Octave.
     *
     * @throws IOException
     */
    public static void initPlugin() throws IOException {

        /*
         * 1. register the parameters of the plugin
         */
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
        ParameterService
                .registerParameter(new ParameterTypeLong(
                        PROPERTY_OCTAVE_READ_TIMEOUT_SECONDS,
                        "Maximum time allowed for an Octave engine to execute a script or send data back",
                        10,
                        Long.MAX_VALUE,
                        OctaveConnectionManager.OCTAVE_READ_TIMEOUT_DEFAULT_VALUE,
                        true));
        ParameterService.registerParameter(new ParameterTypeLong(
                PROPERTY_OCTAVE_WRITE_TIMEOUT_SECONDS,
                "Maximum time allowed to sending a command to Octave engine",
                10, Long.MAX_VALUE,
                OctaveConnectionManager.OCTAVE_WRITE_TIMEOUT_DEFAULT_VALUE,
                true));

        /*
         * 2. unzip the support files in user home directory
         */
        pathToSupportFunctionsFolder = installSupportFunctionsFolder();

        ParameterService.registerParameter(new ParameterTypeDirectory(
                PROPERTY_OCTAVE_M_FILEPATH,
                "Choose the path of the .m support functions (by default "
                        + pathToSupportFunctionsFolder + ")",
                pathToSupportFunctionsFolder));

        // always reset to the correct value here
        ParameterService.setParameterValue(PROPERTY_OCTAVE_M_FILEPATH,
                pathToSupportFunctionsFolder);

        /*
         * 3. Try to run octave and store the error if any
         */
        LOGGER.info("Trying to load an Octave engine...");
        try {
            loadOctaveEngine(true);
        } catch (Throwable t) {
            octaveLoadingExecption = t;
            LogService
                    .getRoot()
                    .log(Level.SEVERE,
                            "Failed to load Octave library! Check your Octave installation and PATH environment variable.",
                            t);
        }
    }

    /**
     * Called at GUI initialization step. If there was an exception when trying
     * to run octave (see initPlugin) then
     *
     * @param mainframe
     */
    public static void initGui(MainFrame mainframe) {

        // check if octave loading created an error in initPlugin()
        ExecutionMode executionMode = RapidMinerGUI.getExecutionMode();
        if (octaveLoadingExecption != null) {

            // show dialog only in UI mode
            if (executionMode != ExecutionMode.UI) {
                return;
            }

            // if ok is selected, extension won't be initialized. If cancel is
            // selected, extension will even be deactivated
            if (!resetOctaveSettingsAskUserWhatToDo()) {
                return;
            }

            // reset octave engine parameter
            ParameterService.setParameterValue(PROPERTY_OCTAVE_OCTAVEPATH, "");
        }

        // installing settings if still undefined
        if (!octaveRunSuccess) {
            if (!installExtensionWithWizardIfNeeded()) {
                return;
            }
        }

        // initializing JRI if needed
        if (!octaveRunSuccess) {
            loadOctaveEngine(executionMode != ExecutionMode.UI);
        }

        if (octaveRunSuccess) {

        } else {
            LOGGER.severe("Could not acquire connection to Octave. Could not load Console.");
        }

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
     * This method tries to load the octave engine from the path specified by
     * the user. If the octave path is undefined this won't work.
     */
    private static boolean loadOctaveEngine(boolean quiet)
            throws UnsatisfiedLinkError, SecurityException {
        // load library
        String octavePath = ParameterService
                .getParameterValue(PROPERTY_OCTAVE_OCTAVEPATH);

        if (octavePath != null && octavePath.length() > 0) {
            LOGGER.log(Level.CONFIG, "Property " + PROPERTY_OCTAVE_OCTAVEPATH
                    + " is set to " + octavePath
                    + ". Using this octave engine.");
        } else {
            LOGGER.log(
                    Level.CONFIG,
                    "Property "
                            + PROPERTY_OCTAVE_OCTAVEPATH
                            + " is undefined or empty: trying to find octave in the PATH");
        }

        try {

            OctaveConnectionManager.onlyInstance
                    .testCreateEngine("startup_test");
            octaveRunSuccess = true;
        } catch (UnsatisfiedLinkError e) {
            if (!quiet)
                SwingTools.showVerySimpleErrorMessage(
                        "octave.could_not_load_engine", octavePath,
                        e.getMessage());
            else
                LOGGER.log(Level.SEVERE, I18N.getMessage(I18N.getErrorBundle(),
                        "octave.could_not_load_native_lib", octavePath,
                        e.getMessage()), e);
            throw e;
        } catch (SecurityException e) {
            if (!quiet)
                SwingTools.showVerySimpleErrorMessage(
                        "octave.could_not_load_engine", octavePath,
                        e.getMessage());
            else
                LOGGER.log(Level.SEVERE, I18N.getMessage(I18N.getErrorBundle(),
                        "octave.could_not_load_native_lib", octavePath,
                        e.getMessage()), e);
            throw e;
        }
        return true;
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

    /**
     * Ask the user if he wants to try to setup Octave properly of if he wants
     * to deactivate the extension.
     *
     * @return
     */
    private static boolean resetOctaveSettingsAskUserWhatToDo() {
        int dialogOptions = ConfirmDialog.YES_NO_OPTION;
        int result = LibraryLoadingErrorDialog.showLoadingErrorDialog(
                "octave.could_not_load_native_lib", dialogOptions,
                ParameterService.getParameterValue(PROPERTY_OCTAVE_OCTAVEPATH),
                octaveLoadingExecption.getMessage());
        return result == ConfirmDialog.YES_OPTION
                || result == ConfirmDialog.CANCEL_OPTION;
    }

    /**
     * Currently there will be only installed the native JRI interface
     */
    private static boolean installExtensionWithWizardIfNeeded() {
        // if (RapidMiner.getRapidMinerPropertyValue(PROPERTY_R_NATIVE_LIBRARY)
        // == null ||
        // RapidMiner.getRapidMinerPropertyValue(PROPERTY_R_NATIVE_LIBRARY).isEmpty())
        // {
        File libFile = new File(
                ParameterService.getParameterValue(PROPERTY_OCTAVE_OCTAVEPATH)
                        + "");
        // File rHomeDir = new File(System.getenv("PATH") + "");

        if (!libFile.exists()) {

            // check if octave is in the os PATH. if so, try to retrieve its
            // path.
            // Runtime rt = Runtime.getRuntime();
            // Process proc = rt.exec("svn help");
            // int exitVal = proc.exitValue();

            // libFile = new File(rHomeDir, "library" + File.separator + "rJava"
            // + File.separator + "jri" + File.separator + "jri.dll");
            // ParameterService.setParameterValue(PROPERTY_OCTAVE_OCTAVEPATH,
            // libFile.getAbsolutePath());
        }

        ButtonDialog readMeDialog = null;

        // presenting readme for installing Octave...
        final File finalLibFile = libFile;

        readMeDialog = new ButtonDialog("octave_install_instructions", true,
                new Object[] {}) {

            private static final long serialVersionUID = 1L;

            private boolean selectedFile;
            private JButton okButton;
            private JButton deactivateExtensionButton;
            private JButton skipButton;
            {
                ExtendedHTMLJEditorPane text;

                URL installInstructionsURL;
                String osName = System.getProperty("os.name").toLowerCase();

                installInstructionsURL = PluginInitOctaveExtension.class
                        .getResource("/com/rapidminer/resources/help/install_instructions_all.html");
                // if (osName.startsWith("windows")) {
                // // includes: Windows 2000, Windows 95, Windows 98, Windows
                // // NT, Windows Vista, Windows XP
                // installInstructionsURL = PluginInitOctaveExtension.class
                // .getResource("/com/rapidminer/resources/help/install_instructions_windows.html");
                // } else if (osName.startsWith("mac os x")) {
                // // OS X
                // installInstructionsURL = PluginInitOctaveExtension.class
                // .getResource("/com/rapidminer/resources/help/install_instructions_osx.html");
                // } else {
                // // everything else
                // installInstructionsURL = PluginInitOctaveExtension.class
                // .getResource("/com/rapidminer/resources/help/install_instructions_other.html");
                // }

                try {
                    text = new ExtendedHTMLJEditorPane(
                            installInstructionsURL.toString());
                } catch (Exception e) {
                    text = new ExtendedHTMLJEditorPane("text/html",
                            "<b>Could not find help resource: Distribution damaged.</b>");
                }
                text.installDefaultStylesheet();
                text.setEditable(false);
                ((HTMLEditorKit) text.getEditorKit()).getStyleSheet().addRule(
                        "a  {text-decoration:underline; color:blue;}");

                text.addHyperlinkListener(new HyperlinkListener() {

                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (HyperlinkEvent.EventType.ACTIVATED.equals(e
                                .getEventType())) {
                            try {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } catch (Exception e1) {
                                SwingTools
                                        .showVerySimpleErrorMessage("cannot_open_browser");
                            }
                        }
                    }
                });

                ExtendedJScrollPane centerComponent = new ExtendedJScrollPane(
                        text);
                centerComponent
                        .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                okButton = makeOkButton("octave.select.octaveEngine");
                skipButton = makeCancelButton("octave.skip.installation");

                final ManagedExtension extension = ManagedExtension
                        .get("rmx_octave");
                if (extension != null) {
                    deactivateExtensionButton = new JButton(new ResourceAction(
                            false, "octave.deactivate") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            extension.setActive(false);
                            ManagedExtension.saveConfiguration();
                            cancel();
                        }
                    });
                    layoutDefault(centerComponent, ButtonDialog.LARGE,
                            deactivateExtensionButton, okButton, skipButton);
                } else {
                    layoutDefault(centerComponent, ButtonDialog.LARGE,
                            okButton, skipButton);
                }
                selectedFile = false;
            }

            @Override
            protected void ok() {
                if (!selectedFile) {
                    if (!finalLibFile.exists()) {
                        // if not exists let user choose himself
                        JFileChooser fileChooser = SwingTools
                                .createFileChooser("octave.lib", null, false,
                                        new FileFilter[0]);
                        File selectedFile = null;
                        boolean approved = fileChooser.showDialog(null, "Ok") == JFileChooser.APPROVE_OPTION;
                        while ((selectedFile == null || !selectedFile.exists())
                                && approved) {
                            selectedFile = fileChooser.getSelectedFile();
                            ParameterService.setParameterValue(
                                    PROPERTY_OCTAVE_OCTAVEPATH,
                                    selectedFile.getAbsolutePath());
                            if (!approved) {
                                approved = fileChooser.showDialog(null, "Ok") == JFileChooser.APPROVE_OPTION;
                            }
                        }
                        if (selectedFile == null) {
                            setConfirmed(false);
                            cancel();
                        }
                    } else {
                        SwingTools.showMessageDialog("octave_found_library",
                                finalLibFile.getAbsolutePath());
                        ParameterService.setParameterValue(
                                PROPERTY_OCTAVE_OCTAVEPATH,
                                finalLibFile.getAbsolutePath());
                    }

                    selectedFile = true;
                    okButton.setAction(new ResourceAction("octave.restart") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ParameterService.saveParameters();
                            RapidMiner.quit(ExitMode.NORMAL);
                        }
                    });
                } else {
                    super.ok();
                }
            }
        };
        readMeDialog.setVisible(true);
        return readMeDialog.wasConfirmed();
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
