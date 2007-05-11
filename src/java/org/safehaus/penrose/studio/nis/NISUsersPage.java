package org.safehaus.penrose.studio.nis;

import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.apache.log4j.Logger;
import org.safehaus.penrose.studio.PenroseApplication;
import org.safehaus.penrose.studio.nis.action.*;
import org.safehaus.penrose.source.SourceManager;
import org.safehaus.penrose.source.Source;
import org.safehaus.penrose.naming.PenroseContext;
import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.nis.NISDomain;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class NISUsersPage extends FormPage {

    Logger log = Logger.getLogger(getClass());

    FormToolkit toolkit;

    Combo actionCombo;

    Label messageLabel;
    Table usersTable;
    Table conflictsTable;
    Table matchesTable;

    NISEditor editor;
    NISDomain domain;

    Source actions;
    Source domains;

    Map<String,Collection<Conflict>> conflicts = new TreeMap<String,Collection<Conflict>>();

    public NISUsersPage(NISEditor editor) {
        super(editor, "USERS", "  Users  ");

        this.editor = editor;
        this.domain = editor.getDomain();

        PenroseApplication penroseApplication = PenroseApplication.getInstance();
        PenroseContext penroseContext = penroseApplication.getPenroseContext();
        SourceManager sourceManager = penroseContext.getSourceManager();

        actions = sourceManager.getSource("DEFAULT", "penrose.actions");
        domains = sourceManager.getSource("DEFAULT", "penrose.domains");

    }

    public void createFormContent(IManagedForm managedForm) {
        toolkit = managedForm.getToolkit();

        ScrolledForm form = managedForm.getForm();
        form.setText("NIS Users");

        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        Section section = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Action");
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control sourcesSection = createActionSection(section);
        section.setClient(sourcesSection);

        section = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Results");
        section.setLayoutData(new GridData(GridData.FILL_BOTH));

        Control resultsSection = createResultsSection(section);
        section.setClient(resultsSection);

        init();
    }

    public void init() {
        try {
            actionCombo.removeAll();

            SearchRequest request = new SearchRequest();
            request.setFilter("(type=users)");

            SearchResponse<SearchResult> response = new SearchResponse<SearchResult>() {
                public void add(SearchResult result) throws Exception {
                    Attributes attributes = result.getAttributes();
                    String actionName = (String) attributes.getValue("name");
                    String className = (String) attributes.getValue("className");

                    actionCombo.add(actionName);
                    actionCombo.setData(actionName, className);
                }
            };

            actions.search(request, response);

            actionCombo.select(0);

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            String message = e.toString();
            if (message.length() > 500) {
                message = message.substring(0, 500) + "...";
            }
            MessageDialog.openError(editor.getSite().getShell(), "Init Failed", message);
        }
    }

    public Composite createActionSection(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayout(new GridLayout(3, false));

        Label actionLabel = toolkit.createLabel(composite, "Action:");
        GridData gd = new GridData();
        gd.widthHint = 100;
        actionLabel.setLayoutData(gd);

        actionCombo = new Combo(composite, SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        actionCombo.setLayoutData(gd);

        Button runButton = new Button(composite, SWT.PUSH);
        runButton.setText("Run");
        gd = new GridData();
        gd.horizontalAlignment = GridData.END;
        gd.widthHint = 80;
        runButton.setLayoutData(gd);

        runButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                try {
                    run();
                } catch (Exception e) {
                    log.debug(e.getMessage(), e);
                    String message = e.toString();
                    if (message.length() > 500) {
                        message = message.substring(0, 500) + "...";
                    }
                    MessageDialog.openError(editor.getSite().getShell(), "Action Failed", message);
                }
            }
        });

        return composite;
    }

    public Composite createResultsSection(Composite parent) {

        Composite composite = toolkit.createComposite(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(2, false));

        messageLabel = toolkit.createLabel(composite, "");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        messageLabel.setLayoutData(gd);

        Label conflictsLabel = toolkit.createLabel(composite, "Conflicts:");
        conflictsLabel.setLayoutData(new GridData());

        usersTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        gd = new GridData(GridData.FILL_BOTH);
        gd.verticalSpan = 3;
        usersTable.setLayoutData(gd);

        usersTable.setHeaderVisible(true);
        usersTable.setLinesVisible(true);

        TableColumn tc = new TableColumn(usersTable, SWT.NONE);
        tc.setText("User");
        tc.setWidth(100);

        tc = new TableColumn(usersTable, SWT.NONE);
        tc.setText("UID");
        tc.setWidth(80);

        usersTable.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                try {
                    if (usersTable.getSelectionCount() == 0) return;

                    TableItem item = usersTable.getSelection()[0];

                    Attributes attributes = (Attributes)item.getData();

                    showConflicts(attributes);
                    showMatches(attributes);

                } catch (Exception e) {
                    log.debug(e.getMessage(), e);
                    String message = e.toString();
                    if (message.length() > 500) {
                        message = message.substring(0, 500) + "...";
                    }
                    MessageDialog.openError(editor.getSite().getShell(), "Edit Failed", message);
                }
            }
        });

        conflictsTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        conflictsTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        conflictsTable.setHeaderVisible(true);
        conflictsTable.setLinesVisible(true);

        tc = new TableColumn(conflictsTable, SWT.NONE);
        tc.setText("Domain");
        tc.setWidth(120);

        tc = new TableColumn(conflictsTable, SWT.NONE);
        tc.setText("User");
        tc.setWidth(100);

        tc = new TableColumn(conflictsTable, SWT.NONE);
        tc.setText("UID");
        tc.setWidth(80);

        Label matchesLabel = toolkit.createLabel(composite, "Matches:");
        matchesLabel.setLayoutData(new GridData());

        matchesTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        matchesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        matchesTable.setHeaderVisible(true);
        matchesTable.setLinesVisible(true);

        tc = new TableColumn(matchesTable, SWT.NONE);
        tc.setText("Domain");
        tc.setWidth(120);

        tc = new TableColumn(matchesTable, SWT.NONE);
        tc.setText("User");
        tc.setWidth(100);

        tc = new TableColumn(matchesTable, SWT.NONE);
        tc.setText("UID");
        tc.setWidth(80);

        Button editButton = new Button(composite, SWT.PUSH);
        editButton.setText("Edit");
        gd = new GridData();
        gd.widthHint = 80;
        editButton.setLayoutData(gd);

        editButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                try {
                    if (usersTable.getSelectionCount() == 0) return;

                    TableItem item = usersTable.getSelection()[0];

                    Attributes attributes = (Attributes)item.getData();
                    String domain = (String)attributes.getValue("domain");
                    String partition = (String)attributes.getValue("partition");
                    Source source = (Source)attributes.getValue("source");
                    String uid = (String)attributes.getValue("uid");
                    Object origUidNumber = attributes.getValue("origUidNumber");

                    edit(domain, partition, source, uid, origUidNumber);

                } catch (Exception e) {
                    log.debug(e.getMessage(), e);
                    String message = e.toString();
                    if (message.length() > 500) {
                        message = message.substring(0, 500) + "...";
                    }
                    MessageDialog.openError(editor.getSite().getShell(), "Edit Failed", message);
                }
            }
        });

        return composite;
    }

    public void showConflicts(Attributes attributes) throws Exception {

        conflictsTable.removeAll();

        String uid1 = (String) attributes.getValue("uid");
        Collection<Conflict> list = conflicts.get(uid1);

        if (list == null) return;

        for (Conflict conflict : list) {

            Attributes attributes2 = conflict.getAttributes2();

            String domain2 = (String) attributes2.getValue("domain");
            String uid2 = (String) attributes2.getValue("uid");
            Integer uidNumber2 = (Integer) attributes2.getValue("uidNumber");
            if (uidNumber2 == null) uidNumber2 = (Integer) attributes2.getValue("origUidNumber");

            TableItem ti = new TableItem(conflictsTable, SWT.NONE);
            ti.setText(0, domain2);
            ti.setText(1, "" + uid2);
            ti.setText(2, "" + uidNumber2);
            ti.setData(attributes2);
        }
    }

    public void showMatches(Attributes attributes) throws Exception {

        matchesTable.removeAll();

        String uid = (String) attributes.getValue("uid");
        Integer uidNumber = (Integer) attributes.getValue("uidNumber");
        if (uidNumber == null) uidNumber = (Integer) attributes.getValue("origUidNumber");

        PenroseApplication penroseApplication = PenroseApplication.getInstance();
        PenroseContext penroseContext = penroseApplication.getPenroseContext();
        SourceManager sourceManager = penroseContext.getSourceManager();

        SearchRequest searchRequest = new SearchRequest();
        SearchResponse<SearchResult> searchResponse = new SearchResponse<SearchResult>();

        domains.search(searchRequest, searchResponse);

        while (searchResponse.hasNext()) {
            SearchResult searchResults = searchResponse.next();
            Attributes attrs = searchResults.getAttributes();

            String domainName = (String) attrs.getValue("name");
            String partition = (String) attrs.getValue("partition");

            if (domain.getName().equals(domainName)) continue;
            
            SearchRequest request = new SearchRequest();
            request.setFilter("(&(uid="+uid+")(uidNumber="+uidNumber+"))");

            SearchResponse<SearchResult> response = new SearchResponse<SearchResult>();

            Source users = sourceManager.getSource(partition, "cache.users");
            users.search(request, response);

            while (response.hasNext()) {
                SearchResult result = response.next();
                Attributes attributes2 = result.getAttributes();

                String uid2 = (String) attributes2.getValue("uid");
                Integer uidNumber2 = (Integer)attributes2.getValue("uidNumber");

                TableItem ti = new TableItem(matchesTable, SWT.NONE);
                ti.setText(0, domainName);
                ti.setText(1, "" + uid2);
                ti.setText(2, "" + uidNumber2);
                ti.setData(attributes2);
            }
        }
    }

    public void run() throws Exception {

        usersTable.removeAll();
        conflictsTable.removeAll();
        matchesTable.removeAll();
        conflicts.clear();

        String actionName = actionCombo.getText();
        String className = (String) actionCombo.getData(actionName);

        Class clazz = Class.forName(className);
        NISAction action = (NISAction) clazz.newInstance();

        NISActionRequest request = new NISActionRequest();
        request.setDomain(domain.getName());

        SearchRequest searchRequest = new SearchRequest();
        SearchResponse<SearchResult> searchResponse = new SearchResponse<SearchResult>();

        domains.search(searchRequest, searchResponse);

        while (searchResponse.hasNext()) {
            SearchResult result = searchResponse.next();
            Attributes attributes = result.getAttributes();
            String domain = (String) attributes.getValue("name");
            request.addDomain(domain);
        }

        NISActionResponse response = new NISActionResponse() {
            public void add(Object object) {
                Conflict conflict = (Conflict)object;

                Attributes attributes1 = conflict.getAttributes1();
                String uid = (String) attributes1.getValue("uid");

                Collection<Conflict> list = conflicts.get(uid);
                if (list == null) {
                    list = new ArrayList<Conflict>();
                    conflicts.put(uid, list);
                }

                list.add(conflict);
            }
        };

        action.execute(request, response);

        for (Collection<Conflict> list : conflicts.values()) {
            
            Conflict conflict = list.iterator().next();

            Attributes attributes1 = conflict.getAttributes1();
            String uid = (String) attributes1.getValue("uid");
            Integer uidNumber = (Integer)attributes1.getValue("uidNumber");
            if (uidNumber == null) uidNumber = (Integer)attributes1.getValue("origUidNumber");

            TableItem ti = new TableItem(usersTable, SWT.NONE);
            ti.setText(0, uid);
            ti.setText(1, "" + uidNumber);

            ti.setData(attributes1);
        }

        messageLabel.setText("Found " + conflicts.size() + " user(s).");
    }

    public void edit(
            String domain,
            String partition,
            Source source,
            String uid,
            Object origUidNumber
    ) throws Exception {

        PenroseApplication penroseApplication = PenroseApplication.getInstance();
        PenroseContext penroseContext = penroseApplication.getPenroseContext();
        SourceManager sourceManager = penroseContext.getSourceManager();

        RDNBuilder rb = new RDNBuilder();
        rb.set("domain", domain);
        rb.set("uid", uid);
        DN dn = new DN(rb.toRdn());

        NISUserDialog dialog = new NISUserDialog(getSite().getShell(), SWT.NONE);
        dialog.setDomain(domain);
        dialog.setUid(uid);
        dialog.setOrigUidNumber(origUidNumber);

        Source penroseUsers = sourceManager.getSource(partition, "penrose.users");

        SearchRequest request = new SearchRequest();
        request.setDn(dn);
        SearchResponse<SearchResult> response = new SearchResponse<SearchResult>();

        penroseUsers.search(request, response);

        Object currentUidNumber;

        if (response.hasNext()) {
            SearchResult result = response.next();
            Attributes attributes = result.getAttributes();
            currentUidNumber = attributes.getValue("uidNumber");
            dialog.setNewUidNumber(currentUidNumber);

        } else {
            currentUidNumber = origUidNumber;
        }

        dialog.open();

        int action = dialog.getAction();

        if (action == NISUserDialog.CANCEL) return;

        Object uidNumber = dialog.getUidNumber();
        String message = dialog.getMessage();

        if (action == NISUserDialog.SET) {

            if (!origUidNumber.equals(uidNumber)) checkUidNumber(uid, uidNumber);

            Attributes attrs = new Attributes();
            attrs.setValue("domain", domain);
            attrs.setValue("uid", uid);
            attrs.setValue("oldUidNumber", origUidNumber);
            attrs.setValue("uidNumber", uidNumber);
            attrs.setValue("message", message);

            penroseUsers.add(dn, attrs);

        } else if (action == NISUserDialog.CHANGE) {

            if (!origUidNumber.equals(uidNumber)) checkUidNumber(uid, uidNumber);

            Collection<Modification> modifications = new ArrayList<Modification>();
            modifications.add(new Modification(Modification.REPLACE, new Attribute("uidNumber", uidNumber)));
            modifications.add(new Modification(Modification.REPLACE, new Attribute("message", message)));

            penroseUsers.modify(dn, modifications);

        } else { // if (action == NISUserDialog.REMOVE) {

            penroseUsers.delete(dn);
            uidNumber = origUidNumber;
        }

        Source changes = sourceManager.getSource("DEFAULT", "penrose.changes");

        Attributes attributes = new Attributes();
        attributes.setValue("domain", domain);
        attributes.setValue("type", "user");
        attributes.setValue("target", uid);
        attributes.setValue("oldValue", currentUidNumber.toString());
        attributes.setValue("newValue", uidNumber.toString());
        attributes.setValue("message", message);

        changes.add(new DN(), attributes);
    }

    public void checkUidNumber(String uid, Object uidNumber) throws Exception {

        PenroseApplication penroseApplication = PenroseApplication.getInstance();
        PenroseContext penroseContext = penroseApplication.getPenroseContext();
        SourceManager sourceManager = penroseContext.getSourceManager();

        SearchRequest request = new SearchRequest();
        request.setFilter("(uidNumber=" + uidNumber + ")");

        SearchResponse<SearchResult> response = new SearchResponse<SearchResult>();

        Source uidNumbers = sourceManager.getSource("DEFAULT", "penrose.users");
        uidNumbers.search(request, response);

        while (response.hasNext()) {
            SearchResult result = response.next();
            Attributes attributes = result.getAttributes();

            String domainName = (String)attributes.getValue("domain");
            String uid2 = (String)attributes.getValue("uid");
            if (uid.equals(uid2)) continue;

            throw new Exception("UID number "+uidNumber+" is already allocated for user "+uid2+" in domain "+domainName);
        }

        SearchRequest searchRequest = new SearchRequest();
        SearchResponse<SearchResult> searchResponse = new SearchResponse<SearchResult>();

        domains.search(searchRequest, searchResponse);

        while (searchResponse.hasNext()) {
            SearchResult searchResults = searchResponse.next();
            Attributes attributes = searchResults.getAttributes();

            String domainName = (String) attributes.getValue("name");
            String partition = (String) attributes.getValue("partition");

            if (domain.getName().equals(domainName)) continue;

            response = new SearchResponse<SearchResult>();

            Source users = sourceManager.getSource(partition, "cache.users");
            users.search(request, response);

            while (response.hasNext()) {
                SearchResult result = response.next();
                Attributes attrs = result.getAttributes();

                String uid2 = (String)attrs.getValue("uid");
                if (uid.equals(uid2)) continue;

                throw new Exception("UID number "+uidNumber+" is used by user "+uid2+" in domain "+domainName);
            }
        }
    }
}
