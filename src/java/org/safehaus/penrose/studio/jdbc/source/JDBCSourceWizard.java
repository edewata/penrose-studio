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
package org.safehaus.penrose.studio.jdbc.source;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.safehaus.penrose.partition.*;
import org.safehaus.penrose.jdbc.JDBCClient;
import org.safehaus.penrose.source.SourceConfig;
import org.safehaus.penrose.source.FieldConfig;
import org.safehaus.penrose.source.TableConfig;
import org.safehaus.penrose.connection.ConnectionConfig;
import org.safehaus.penrose.studio.source.wizard.SourceWizardPage;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Endi S. Dewata
 */
public class JDBCSourceWizard extends Wizard {

    Logger log = Logger.getLogger(getClass());

    private PartitionConfig partitionConfig;
    private ConnectionConfig connectionConfig;
    private SourceConfig sourceConfig;

    public SourceWizardPage propertyPage;
    public JDBCTableWizardPage jdbcTablePage;
    public JDBCFieldWizardPage jdbcFieldsPage;
    public JDBCPrimaryKeyWizardPage jdbcPrimaryKeyPage;

    public JDBCSourceWizard(PartitionConfig partition, ConnectionConfig connectionConfig) {
        this.partitionConfig = partition;
        this.connectionConfig = connectionConfig;

        propertyPage = new SourceWizardPage();
        jdbcTablePage = new JDBCTableWizardPage();
        jdbcFieldsPage = new JDBCFieldWizardPage();
        jdbcPrimaryKeyPage = new JDBCPrimaryKeyWizardPage();

        setWindowTitle(connectionConfig.getName()+" - New Source");
    }

    public boolean canFinish() {
        if (!propertyPage.isPageComplete()) return false;
        if (!jdbcTablePage.isPageComplete()) return false;
        if (!jdbcFieldsPage.isPageComplete()) return false;
        if (!jdbcPrimaryKeyPage.isPageComplete()) return false;

        return true;
    }

    public void addPages() {
        addPage(propertyPage);
        addPage(jdbcTablePage);
        addPage(jdbcFieldsPage);
        addPage(jdbcPrimaryKeyPage);
    }

    public IWizardPage getNextPage(IWizardPage page) {
        if (propertyPage == page) {
            jdbcTablePage.setConnectionConfig(connectionConfig);

        } else if (jdbcTablePage == page) {
            TableConfig tableConfig = jdbcTablePage.getTableConfig();
            jdbcFieldsPage.setTableConfig(connectionConfig, tableConfig);

        } else if (jdbcFieldsPage == page) {
            Collection selectedFields = jdbcFieldsPage.getSelectedFieldConfigs();
            jdbcPrimaryKeyPage.setFieldConfigs(selectedFields);
        }

        return super.getNextPage(page);
    }

    public boolean performFinish() {
        try {
            sourceConfig = new SourceConfig();
            sourceConfig.setName(propertyPage.getSourceName());
            sourceConfig.setConnectionName(connectionConfig.getName());

            TableConfig tableConfig = jdbcTablePage.getTableConfig();

            String catalog = tableConfig.getCatalog();
            String schema = tableConfig.getSchema();
            String tableName = tableConfig.getName();

            sourceConfig.setParameter(JDBCClient.CATALOG, catalog);
            sourceConfig.setParameter(JDBCClient.SCHEMA, schema);
            sourceConfig.setParameter(JDBCClient.TABLE, tableName);

            String filter = jdbcFieldsPage.getFilter();
            if (filter != null) {
                sourceConfig.setParameter(JDBCClient.FILTER, filter);
            }

            System.out.println("Saving fields :");
            Collection fields = jdbcPrimaryKeyPage.getFields();
            if (fields.isEmpty()) {
                fields = jdbcFieldsPage.getSelectedFieldConfigs();
            }

            for (Iterator i=fields.iterator(); i.hasNext(); ) {
                FieldConfig field = (FieldConfig)i.next();
                System.out.println(" - "+field.getName()+" "+field.isPrimaryKey());
                sourceConfig.addFieldConfig(field);
            }

            partitionConfig.getSourceConfigs().addSourceConfig(sourceConfig);

            return true;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public SourceConfig getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(SourceConfig connection) {
        this.sourceConfig = connection;
    }

    public boolean needsPreviousAndNextButtons() {
        return true;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public PartitionConfig getPartitionConfig() {
        return partitionConfig;
    }

    public void setPartitionConfig(PartitionConfig partitionConfig) {
        this.partitionConfig = partitionConfig;
    }
}