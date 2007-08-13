package org.safehaus.penrose.studio.nis;

import org.safehaus.penrose.studio.tree.Node;
import org.safehaus.penrose.studio.server.ServersView;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.PenroseImage;
import org.safehaus.penrose.studio.PenrosePlugin;
import org.safehaus.penrose.studio.project.ProjectNode;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.studio.nis.wizard.NewNISDomainWizard;
import org.safehaus.penrose.nis.NISDomain;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.naming.PenroseContext;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbenchActionConstants;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.io.File;

/**
 * @author Endi S. Dewata
 */
public class NISNode extends Node {

    private ServersView view;
    private ProjectNode projectNode;

    protected NISTool nisTool;
    protected boolean started;

    Map<String,Node> children = new TreeMap<String,Node>();

    public NISNode(String name, String type, Image image, Object object, Object parent) {
        super(name, type, image, object, parent);
        projectNode = (ProjectNode)parent;
        view = projectNode.getView();
    }

    public void showMenu(IMenuManager manager) throws Exception {

        manager.add(new Action("Open") {
            public void run() {
                try {
                    open();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            public boolean isEnabled() {
                return !started;
            }
        });

        manager.add(new Action("Close") {
            public void run() {
                try {
                    close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            public boolean isEnabled() {
                return started;
            }
        });

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.add(new Action("New Domain...") {
            public void run() {
                try {
                    newDomain();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            public boolean isEnabled() {
                return started;
            }
        });

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.add(new Action("Refresh") {
            public void run() {
                try {
                    refresh();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            public boolean isEnabled() {
                return started;
            }
        });
    }

    public void open() throws Exception {
        if (!started) start();
    }

    public void close() throws Exception {
        nisTool = null;
        children.clear();
        started = false;

        PenroseStudio penroseStudio = PenroseStudio.getInstance();
        penroseStudio.notifyChangeListeners();
    }

    public void start() throws Exception {
        Project project = projectNode.getProject();
        PenroseConfig penroseConfig = project.getPenroseConfig();
        PenroseContext penroseContext = project.getPenroseContext();
        File workDir = project.getWorkDir();

        nisTool = new NISTool();
        nisTool.init(penroseConfig, penroseContext, workDir);

        for (NISDomain nisDomain : nisTool.getNisDomains().values()) {
            addNisDomain(nisDomain);
        }

        started = true;

        PenroseStudio penroseStudio = PenroseStudio.getInstance();
        penroseStudio.notifyChangeListeners();
    }

    public void addNisDomain(NISDomain nisDomain) {

        NISDomainNode node = new NISDomainNode(
                nisDomain.getName(),
                ServersView.ENTRY,
                PenrosePlugin.getImage(PenroseImage.NODE),
                nisDomain,
                this
        );

        children.put(nisDomain.getName(), node);
    }

    public void removeNisDomain(String name) {
        children.remove(name);
    }

    public boolean hasChildren() throws Exception {

        if (!started) return false;

        Map<String,NISDomain> domains = nisTool.getNisDomains();
        return domains.size() > 0;
    }

    public Collection<Node> getChildren() throws Exception {
        return children.values();
    }

    public void newDomain() throws Exception {

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        NewNISDomainWizard wizard = new NewNISDomainWizard();
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.setPageSize(600, 300);
        dialog.open();

        NISDomain domain = wizard.getDomain();

        nisTool.createDomain(domain);
        nisTool.createPartitionConfig(domain);
        nisTool.createDatabase(domain);
        nisTool.createPartition(domain);

        Project project = projectNode.getProject();
        project.upload("partitions/"+domain.getPartition());

        addNisDomain(domain);

        PenroseStudio penroseStudio = PenroseStudio.getInstance();
        penroseStudio.notifyChangeListeners();
    }

    public void refresh() throws Exception {
        PenroseStudio penroseStudio = PenroseStudio.getInstance();
        penroseStudio.notifyChangeListeners();
    }

    public NISTool getNisTool() {
        return nisTool;
    }

    public void setNisTool(NISTool nisTool) {
        this.nisTool = nisTool;
    }

    public ServersView getView() {
        return view;
    }

    public void setView(ServersView view) {
        this.view = view;
    }

    public ProjectNode getProjectNode() {
        return projectNode;
    }

    public void setProjectNode(ProjectNode projectNode) {
        this.projectNode = projectNode;
    }
}
