package org.safehaus.penrose.studio.project.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.apache.log4j.Logger;
import org.safehaus.penrose.studio.PenroseStudioPlugin;
import org.safehaus.penrose.studio.PenroseImage;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.project.ProjectNode;
import org.safehaus.penrose.studio.server.ServersView;

public class DisconnectAction extends Action {

    Logger log = Logger.getLogger(getClass());

    public DisconnectAction() {
        setText("&Disconnect");
        setImageDescriptor(PenroseStudioPlugin.getImageDescriptor(PenroseImage.SIZE_22x22, PenroseImage.DISCONNECT));
        setAccelerator(SWT.CTRL | 'D');
        setToolTipText("Disconnect");
        setId(getClass().getName());
    }

    public void run() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        try {
            ServersView serversView = ServersView.getInstance();
            ProjectNode projectNode = serversView.getSelectedProjectNode();
            if (projectNode == null) return;

            projectNode.disconnect();
            serversView.close(projectNode);

            PenroseStudio penroseStudio = PenroseStudio.getInstance();
            penroseStudio.notifyChangeListeners();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            MessageDialog.openError(window.getShell(), "Action Failed", e.getMessage());
        }
    }
/*
    public boolean isEnabled() {
        try {
            ServersView serversView = ServersView.getInstance();
            ProjectNode projectNode = serversView.getSelectedProjectNode();
            if (projectNode == null) return false;
            
            Project project = projectNode.getProject();
            return project.isConnected();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
*/
}
