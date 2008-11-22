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
package org.safehaus.penrose.studio.ldap.connection.editor;

import org.safehaus.penrose.studio.connection.editor.ConnectionEditor;
import org.safehaus.penrose.studio.ldap.connection.JNDIConnectionPropertiesPage;
import org.safehaus.penrose.studio.ldap.connection.editor.LDAPConnectionBrowserPage;
import org.safehaus.penrose.studio.ldap.connection.editor.LDAPConnectionSchemaPage;

/**
 * @author Endi S. Dewata
 */
public class LDAPConnectionEditor extends ConnectionEditor {

    public void addPages() {
        try {
            addPage(new JNDIConnectionPropertiesPage(this));

            //PenroseStudio penroseStudio = PenroseStudio.getInstance();
            //PenroseStudioWorkbenchAdvisor workbenchAdvisor = penroseStudio.getWorkbenchAdvisor();
            //PenroseStudioWorkbenchWindowAdvisor workbenchWindowAdvisor = workbenchAdvisor.getWorkbenchWindowAdvisor();
            //PenroseStudioActionBarAdvisor actionBarAdvisor = workbenchWindowAdvisor.getActionBarAdvisor();

            //if (actionBarAdvisor.getShowCommercialFeaturesAction().isChecked()) {

                addPage(new LDAPConnectionBrowserPage(this));
                addPage(new LDAPConnectionSchemaPage(this));
            //}

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}