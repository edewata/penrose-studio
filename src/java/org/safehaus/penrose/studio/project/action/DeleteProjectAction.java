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
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.studio.server.ServersView;

public class DeleteProjectAction extends Action {

    Logger log = Logger.getLogger(getClass());

    public DeleteProjectAction() {
        setText("&Delete Server");
        setImageDescriptor(PenroseStudioPlugin.getImageDescriptor(PenroseImage.SIZE_22x22, PenroseImage.DELETE));
        setAccelerator(SWT.CTRL | 'D');
        setToolTipText("Delete Server");
        setId(getClass().getName());
    }

    public void run() {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        try {
            ServersView serversView = ServersView.getInstance();
            ProjectNode projectNode = serversView.getSelectedProjectNode();
            if (projectNode == null) return;

            Project project = projectNode.getProject();

            boolean confirm = MessageDialog.openQuestion(
                    window.getShell(),
                    "Removing Server", "Are you sure?"
            );

            if (!confirm) return;

            serversView.removeProjectConfig(project.getName());

            PenroseStudio penroseStudio = PenroseStudio.getInstance();
            penroseStudio.notifyChangeListeners();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            MessageDialog.openError(window.getShell(), "Action Failed", e.getMessage());
        }
    }
}
