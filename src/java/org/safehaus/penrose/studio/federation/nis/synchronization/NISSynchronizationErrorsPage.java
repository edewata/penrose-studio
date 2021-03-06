package org.safehaus.penrose.studio.federation.nis.synchronization;

import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.core.runtime.IProgressMonitor;
import org.apache.log4j.Logger;
import org.safehaus.penrose.federation.NISRepositoryClient;
import org.safehaus.penrose.federation.FederationRepositoryConfig;
import org.safehaus.penrose.studio.PenroseStudio;
import org.safehaus.penrose.studio.server.Server;
import org.safehaus.penrose.studio.federation.nis.synchronization.NISSynchronizationEditor;
import org.safehaus.penrose.studio.dialog.ErrorDialog;
import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.partition.PartitionClient;
import org.safehaus.penrose.source.SourceClient;
import org.safehaus.penrose.source.SourceManagerClient;
import org.safehaus.penrose.client.PenroseClient;
import org.safehaus.penrose.partition.PartitionManagerClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.ArrayList;

/**
 * @author Endi S. Dewata
 */
public class NISSynchronizationErrorsPage extends FormPage {

    public DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Logger log = Logger.getLogger(getClass());

    FormToolkit toolkit;

    NISSynchronizationEditor editor;
    NISRepositoryClient nisFederationClient;
    FederationRepositoryConfig domain;

    Table table;
    Label totalLabel;
    Text descriptionText;
    //Text noteText;

    Server server;
    PartitionClient partitionClient;
    SourceClient errors;

    public NISSynchronizationErrorsPage(NISSynchronizationEditor editor) throws Exception {
        super(editor, "ERRORS", "  Errors  ");

        this.editor = editor;
        this.server = editor.getServer();
        this.nisFederationClient = editor.getNISFederationClient();
        this.domain = editor.getDomain();

        String federationName = nisFederationClient.getFederationClient().getFederationDomain();
        PenroseClient penroseClient = server.getClient();

        PartitionManagerClient partitionManagerClient = penroseClient.getPartitionManagerClient();
        partitionClient = partitionManagerClient.getPartitionClient(federationName+"_"+domain.getName());

        SourceManagerClient sourceManagerClient = partitionClient.getSourceManagerClient();
        errors = sourceManagerClient.getSourceClient("errors");
    }

    public void createFormContent(IManagedForm managedForm) {
        toolkit = managedForm.getToolkit();

        ScrolledForm form = managedForm.getForm();
        form.setText("Errors");

        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        Section errorsSection = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        errorsSection.setText("Errors");
        errorsSection.setLayoutData(new GridData(GridData.FILL_BOTH));

        Control errorsControl = createErrorsControl(errorsSection);
        errorsSection.setClient(errorsControl);
    }

    public Composite createErrorsControl(final Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayout(new GridLayout(2, false));

        Composite leftControl = createErrorsLeftControl(composite);
        leftControl.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite rightControl = createErrorsRightControl(composite);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.widthHint = 100;
        rightControl.setLayoutData(gd);

        return composite;
    }

    public Composite createErrorsLeftControl(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayout(new GridLayout());

        table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(100);
        tc.setText("Number");

        tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(150);
        tc.setText("Time");

        tc = new TableColumn(table, SWT.NONE);
        tc.setWidth(300);
        tc.setText("Title");

        table.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                showError();
            }
        });

        totalLabel = toolkit.createLabel(composite, "Total:");
        totalLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(composite, SWT.NONE);

        toolkit.createLabel(composite, "Description:");

        descriptionText = toolkit.createText(composite, "", SWT.BORDER | SWT.READ_ONLY  | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        descriptionText.setLayoutData(new GridData(GridData.FILL_BOTH));

        return composite;
    }

    public Composite createErrorsRightControl(final Composite parent) {

        Composite composite = toolkit.createComposite(parent);

        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        Button removeButton = new Button(composite, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                try {
                    if (table.getSelectionCount() == 0) return;

                    boolean confirm = MessageDialog.openQuestion(
                            editor.getSite().getShell(),
                            "Removing Error",
                            "Are you sure?"
                    );

                    if (!confirm) return;

                    TableItem[] items = table.getSelection();

                    final Collection<DN> dns = new ArrayList<DN>();
                    for (TableItem ti : items) {
                        SearchResult result = (SearchResult)ti.getData();
                        dns.add(result.getDn());
                    }

                    IProgressService progressService = PlatformUI.getWorkbench().getProgressService();

                    progressService.busyCursorWhile(new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                monitor.beginTask("Deleting...", dns.size());

                                for (DN dn : dns) {
                                    
                                    if (monitor.isCanceled()) throw new InterruptedException();

                                    monitor.subTask("Deleting "+dn+"...");

                                    errors.delete(dn);

                                    monitor.worked(1);
                                }

                            } catch (InterruptedException e) {
                                // ignore

                            } catch (Exception e) {
                                throw new InvocationTargetException(e);

                            } finally {
                                monitor.done();
                            }
                        }
                    });

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ErrorDialog.open(e);
                }

                refresh();
            }
        });

        new Label(composite, SWT.NONE);

        Button refreshButton = new Button(composite, SWT.PUSH);
        refreshButton.setText("Refresh");
        refreshButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        refreshButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent selectionEvent) {
                refresh();
            }
        });

        return composite;
    }

    public void refresh() {
        try {
            int[] indices = table.getSelectionIndices();

            table.removeAll();

            final SearchRequest request = new SearchRequest();
            final SearchResponse response = new SearchResponse();

            IProgressService progressService = PlatformUI.getWorkbench().getProgressService();

            progressService.busyCursorWhile(new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException {
                    try {
                        monitor.beginTask("Loading...", IProgressMonitor.UNKNOWN);

                        errors.search(request, response);

                    } catch (InterruptedException e) {
                        // ignore

                    } catch (Exception e) {
                        throw new InvocationTargetException(e);

                    } finally {
                        monitor.done();
                    }
                }
            });

            while (response.hasNext()) {
                SearchResult result = response.next();

                Attributes attributes = result.getAttributes();
                String id = attributes.getValue("id").toString();
                Timestamp time = (Timestamp)attributes.getValue("time");
                String title = (String)attributes.getValue("title");

                TableItem ti = new TableItem(table, SWT.NONE);

                ti.setText(0, id);
                ti.setText(1, df.format(time));
                ti.setText(2, title);

                ti.setData(result);
            }

            totalLabel.setText("Total: "+response.getTotalCount());
            
            table.select(indices);

            showError();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorDialog.open(e);
        }
    }

    public void showError() {

        if (table.getSelectionCount() !=  1) {
            descriptionText.setText("");
            //noteText.setText("");
            return;
        }

        TableItem ti = table.getSelection()[0];

        SearchResult result = (SearchResult)ti.getData();
        Attributes attributes = result.getAttributes();

        String description = (String)attributes.getValue("description");
        descriptionText.setText(description == null ? "" : description);

        //String note = (String)attributes.getValue("note");
        //noteText.setText(note == null ? "" : note);
    }
}
