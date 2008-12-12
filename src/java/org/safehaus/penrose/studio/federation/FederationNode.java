package org.safehaus.penrose.studio.federation;

import org.safehaus.penrose.studio.tree.Node;
import org.safehaus.penrose.studio.project.ProjectNode;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.studio.PenroseImage;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.partition.PartitionManagerClient;
import org.safehaus.penrose.partition.PartitionClient;
import org.safehaus.penrose.module.ModuleManagerClient;
import org.safehaus.penrose.module.ModuleClient;
import org.safehaus.penrose.module.ModuleConfig;
import org.safehaus.penrose.federation.Federation;
import org.apache.log4j.Logger;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Action;

/**
 * @author Endi S. Dewata
 */
public class FederationNode extends Node {

    Logger log = Logger.getLogger(getClass());

    private Project project;

    public FederationNode(String name, Object parent) throws Exception {
        super(name, PenroseStudio.getImage(PenroseImage.FOLDER), null, parent);
    }

    public void init() throws Exception {
        refresh();
    }

    public void refresh() throws Exception {

        children.clear();

        PartitionManagerClient partitionManagerClient = project.getClient().getPartitionManagerClient();

        log.debug("Partitions:");

        for (String partitionName : partitionManagerClient.getPartitionNames()) {
            log.debug(" - "+partitionName);

            PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partitionName);

            ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
            ModuleClient moduleClient = moduleManagerClient.getModuleClient(Federation.FEDERATION);
            if (!moduleClient.exists()) continue;

            ModuleConfig moduleConfig = moduleClient.getModuleConfig();
            String moduleClass = moduleConfig.getModuleClass();
            //log.debug("   - "+moduleConfig.getName()+" module: "+moduleClass);
            if (!moduleClass.equals("org.safehaus.penrose.federation.module.FederationModule")) continue;

            FederationDomainNode node = new FederationDomainNode(partitionName, this);
            node.setProject(project);
            node.init();

            children.add(node);
        }
    }

    public void showMenu(IMenuManager manager) throws Exception {

        manager.add(new Action("Refresh") {
            public void run() {
                try {
                    refresh();

                    PenroseStudio penroseStudio = PenroseStudio.getInstance();
                    penroseStudio.notifyChangeListeners();

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public boolean hasChildren() {
        return true;
    }
}
