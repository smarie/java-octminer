/*
 *  RapidMiner R Extension
 *
 *  Copyright (C) 2001-2013 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.rapid_i.deployment.update.client.ManagedExtension;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;

/**
 * @author Nils Woehler
 *
 */
public class LibraryLoadingErrorDialog extends ConfirmDialog {

	private static final long serialVersionUID = 1L;

	private int returnOption = ConfirmDialog.NO_OPTION;

	public LibraryLoadingErrorDialog(String key, int mode, boolean showAskAgainCheckbox, Object... arguments) {
		super(key, mode, showAskAgainCheckbox, arguments);
	}

	@Override
	protected JButton makeCancelButton() {
		JButton cancelButton = new JButton(new ResourceAction("octave.deactivate") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				final ManagedExtension extension = ManagedExtension.get("rmx_octave");
				if (extension != null) {
					extension.setActive(false);
					ManagedExtension.saveConfiguration();
				}
				cancel();
			}
		});
		return cancelButton;
	}

	@Override
	protected JButton makeYesButton() {
		JButton yesButton = new JButton(new ResourceAction("octave.restart.installation") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void actionPerformed(ActionEvent e) {
				returnOption = YES_OPTION;
				yes();
			}
		});
		getRootPane().setDefaultButton(yesButton);
		return yesButton;
	}

	@Override
	protected JButton makeNoButton() {
		ResourceAction noAction = new ResourceAction("octave.proceed.without.octave") {

			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void actionPerformed(ActionEvent e) {
				returnOption = NO_OPTION;
				no();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "NO");
		getRootPane().getActionMap().put("NO", noAction);
		JButton noButton = new JButton(noAction);
		return noButton;
	}

	@Override
	public int getReturnOption() {
		return returnOption;
	}

	public static int showLoadingErrorDialog(String key, int mode, Object... i18nArgs) {
		LibraryLoadingErrorDialog dialog = new LibraryLoadingErrorDialog(key, mode, false, i18nArgs);
		dialog.setVisible(true);
		return dialog.getReturnOption();
	}

}
