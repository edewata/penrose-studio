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
package org.safehaus.penrose.studio.schema.node;

import org.eclipse.swt.graphics.Image;
import org.safehaus.penrose.client.PenroseClient;
import org.safehaus.penrose.schema.SchemaClient;
import org.safehaus.penrose.schema.SchemaManagerClient;
import org.safehaus.penrose.schema.AttributeType;
import org.safehaus.penrose.schema.SchemaConfig;
import org.safehaus.penrose.studio.PenroseImage;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.server.Server;
import org.safehaus.penrose.studio.server.node.ServerNode;
import org.safehaus.penrose.studio.server.ServersView;
import org.safehaus.penrose.studio.tree.Node;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class AttributeTypesNode extends Node {

    ServersView view;
    ServerNode projectNode;
    SchemaNode schemaNode;

    private SchemaConfig schemaConfig;

    public AttributeTypesNode(ServersView view, String name, Image image, Object object, Node parent) {
        super(name, image, object, parent);
        schemaNode = (SchemaNode)parent;
        projectNode = schemaNode.getProjectNode();
        this.view = projectNode.getServersView();
    }

    public boolean hasChildren() throws Exception {
        return !getChildren().isEmpty();
    }

    public Collection<Node> getChildren() throws Exception {

        Collection<Node> children = new ArrayList<Node>();

        Server project = projectNode.getServer();
        PenroseClient client = project.getClient();
        SchemaManagerClient schemaManagerClient = client.getSchemaManagerClient();
        SchemaClient schemaClient = schemaManagerClient.getSchemaClient(schemaConfig.getName());

        Collection<AttributeType> attributeTypes = schemaClient.getAttributeTypes();
        for (AttributeType attributeType : attributeTypes) {
            children.add(new AttributeTypeNode(
                    view,
                    attributeType.getName(),
                    PenroseStudio.getImage(PenroseImage.ATTRIBUTE_TYPE),
                    attributeType,
                    this
            ));
        }

        return children;
    }

    public SchemaConfig getSchemaConfig() {
        return schemaConfig;
    }

    public void setSchemaConfig(SchemaConfig schemaConfig) {
        this.schemaConfig = schemaConfig;
    }
}