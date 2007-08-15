/**
 * Copyright (c) 2000-2006, Identyx Corporation.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.safehaus.penrose.studio.directory.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.Wizard;
import org.safehaus.penrose.studio.server.ServersView;
import org.safehaus.penrose.studio.directory.EntryNode;
import org.safehaus.penrose.studio.directory.wizard.CreateLDAPProxyWizard;
import org.safehaus.penrose.studio.PenroseStudio;
import org.apache.log4j.Logger;

public class MapLDAPTreeAction extends Action {

    Logger log = Logger.getLogger(getClass());

    EntryNode node;

	public MapLDAPTreeAction(EntryNode node) {
        this.node = node;

        setText("Map LDAP Tree...");
        setId(getClass().getName());
	}
	
	public void run() {
        try {
            ServersView serversView = ServersView.getInstance();

            PenroseStudio penroseStudio = PenroseStudio.getInstance();
            //if (!penroseStudio.isCommercial()) return;

            Wizard wizard = new CreateLDAPProxyWizard(node.getPartitionConfig(), node.getEntryMapping());

            WizardDialog dialog = new WizardDialog(serversView.getSite().getShell(), wizard);
            dialog.setPageSize(600, 300);
            dialog.open();

            penroseStudio.notifyChangeListeners();

            serversView.open(node);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
	}
	
}