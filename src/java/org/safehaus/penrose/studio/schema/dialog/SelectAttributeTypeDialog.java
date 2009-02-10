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
package org.safehaus.penrose.studio.schema.dialog;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.window.Window;
import org.safehaus.penrose.schema.SchemaManagerClient;
import org.safehaus.penrose.schema.AttributeType;
import org.safehaus.penrose.studio.PenroseImage;
import org.safehaus.penrose.studio.PenroseStudio;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class SelectAttributeTypeDialog extends Dialog {

    Logger log = Logger.getLogger(getClass());

    Shell shell;

    Table attributeTable;

    private SchemaManagerClient schemaManagerClient;
    private Collection<String> selections = new ArrayList<String>();

    private int action = Window.CANCEL;

	public SelectAttributeTypeDialog(Shell parent, int style) {
		super(parent, style);

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);

        createControl(shell);
    }

    public int open () {

        Point size = new Point(600, 400);
        shell.setSize(size);

        Point l = getParent().getLocation();
        Point s = getParent().getSize();

        shell.setLocation(l.x + (s.x - size.x)/2, l.y + (s.y - size.y)/2);

        shell.setText(getText());
        shell.setImage(PenroseStudio.getImage(PenroseImage.LOGO));
        shell.open();

        refresh();

        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }

        return action;
    }

    public void createControl(final Shell parent) {
        parent.setLayout(new GridLayout());

        attributeTable = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        attributeTable.setHeaderVisible(true);
        attributeTable.setLinesVisible(true);

        attributeTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        TableColumn tc = new TableColumn(attributeTable, SWT.NONE);
        tc.setText("Name");
        tc.setWidth(200);

        tc = new TableColumn(attributeTable, SWT.NONE);
        tc.setText("Description");
        tc.setWidth(350);

        Composite buttons = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.END;
        buttons.setLayoutData(gd);
        buttons.setLayout(new RowLayout());

		Button saveButton = new Button(buttons, SWT.PUSH);
        saveButton.setText("Select");

		saveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                TableItem items[] = attributeTable.getSelection();
                for (TableItem item : items) {
                    selections.add(item.getText());
                }
                action = Window.OK;
                shell.close();
			}
		});

		Button cancelButton = new Button(buttons, SWT.PUSH);
        cancelButton.setText("Cancel");
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                shell.close();
			}
		});
	}

    public Collection<String> getSelections() {
        return selections;
    }

    public void setSelections(Collection<String> selections) {
        this.selections = selections;
    }

    public SchemaManagerClient getSchemaManagerClient() {
        return schemaManagerClient;
    }

    public void setSchemaManagerClient(SchemaManagerClient schemaManagerClient) {
        this.schemaManagerClient = schemaManagerClient;
    }

    public void refresh() {

        try {
            attributeTable.removeAll();

            for (AttributeType at : schemaManagerClient.getAttributeTypes()) {
                String name = at.getName();
                String description = at.getDescription();
                
                TableItem item = new TableItem(attributeTable, SWT.NONE);
                item.setText(0, name == null ? "" : name);
                item.setText(1, description == null ? "" : description);
            }
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }
}