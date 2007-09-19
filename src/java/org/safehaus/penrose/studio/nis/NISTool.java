package org.safehaus.penrose.studio.nis;

import org.safehaus.penrose.partition.*;
import org.safehaus.penrose.nis.NISDomain;
import org.safehaus.penrose.source.*;
import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.studio.util.FileUtil;
import org.safehaus.penrose.studio.project.Project;
import org.safehaus.penrose.studio.nis.event.NISEventListener;
import org.safehaus.penrose.studio.nis.event.NISEvent;
import org.safehaus.penrose.connection.Connection;
import org.safehaus.penrose.connection.ConnectionConfig;
import org.safehaus.penrose.jdbc.adapter.JDBCAdapter;
import org.safehaus.penrose.jdbc.JDBCClient;
import org.safehaus.penrose.management.PenroseClient;
import org.safehaus.penrose.management.PartitionClient;
import org.safehaus.penrose.naming.PenroseContext;
import org.safehaus.penrose.config.PenroseConfig;
import org.apache.tools.ant.filters.ExpandProperties;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.log4j.Logger;

import javax.naming.Context;
import java.util.*;
import java.io.File;

/**
 * @author Endi Sukma Dewata
 */
public class NISTool {

    public Logger log = Logger.getLogger(getClass());

    public final static String NIS_TOOL              = "nis_tool";
    public final static String NIS_TEMPLATE          = "nis_template";
    public final static String NIS_CONNECTION_NAME   = "NIS";

    public final static String CACHE_USERS           = "cache_users";
    public final static String CACHE_GROUPS          = "cache_groups";
    public final static String CACHE_CONNECTION_NAME = "Cache";

    public final static String CHANGE_USERS          = "change_users";
    public final static String CHANGE_GROUPS         = "change_groups";

    public final static String LDAP_CONNECTION_NAME  = "LDAP";

    private Project project;

    protected Partition nisPartition;

    protected Source domains;
    private Source schedules;
    protected Source actions;
    protected Source hosts;
    protected Source files;
    protected Source changes;
    protected Source users;
    protected Source groups;

    protected Map<String,NISDomain> nisDomains = new TreeMap<String,NISDomain>();

    protected Partitions partitions = new Partitions();

    protected Collection<NISEventListener> listeners = new ArrayList<NISEventListener>();

    public NISTool() {
    }

    public void init(Project project) throws Exception {
        this.project = project;

        File partitionsDir = new File(project.getWorkDir(), "partitions");
        PenroseConfig penroseConfig = project.getPenroseConfig();
        PenroseContext penroseContext = project.getPenroseContext();

        PartitionConfig partitionConfig = project.getPartitionConfigs().getPartitionConfig(NIS_TOOL);

        PartitionFactory partitionFactory = new PartitionFactory();
        partitionFactory.setPartitionsDir(partitionsDir);
        partitionFactory.setPenroseConfig(penroseConfig);
        partitionFactory.setPenroseContext(penroseContext);

        nisPartition = partitionFactory.createPartition(partitionConfig);

        domains   = nisPartition.getSource("penrose_domains");
        schedules = nisPartition.getSource("penrose_schedules");
        actions   = nisPartition.getSource("penrose_actions");
        hosts     = nisPartition.getSource("penrose_hosts");
        files     = nisPartition.getSource("penrose_files");
        changes   = nisPartition.getSource("penrose_changes");
        users     = nisPartition.getSource("penrose_users");
        groups    = nisPartition.getSource("penrose_groups");

        initNisDomains();
    }

    public void initNisDomains() throws Exception {

        SearchRequest searchRequest = new SearchRequest();
        SearchResponse searchResponse = new SearchResponse();

        domains.search(searchRequest, searchResponse);

        while (searchResponse.hasNext()) {
            SearchResult searchResult = searchResponse.next();
            Attributes attributes = searchResult.getAttributes();

            String domainName = (String)attributes.getValue("name");
            String fullName = (String)attributes.getValue("fullName");
            String server = (String)attributes.getValue("server");
            String suffix = (String)attributes.getValue("suffix");

            NISDomain domain = new NISDomain();
            domain.setName(domainName);
            domain.setFullName(fullName);
            domain.setServer(server);
            domain.setSuffix(suffix);

            nisDomains.put(domainName, domain);

            PartitionConfig partitionConfig = project.getPartitionConfigs().getPartitionConfig(domainName);
            boolean createPartitionConfig = partitionConfig == null;

            if (createPartitionConfig) { // create missing partition configs during start
                createPartitionConfig(domain);
            }

            // create missing databases/tables
            createDatabase(domain);

            if (createPartitionConfig) {
                project.upload("partitions/"+domain.getName());

                PenroseClient penroseClient = project.getClient();
                penroseClient.startPartition(domain.getName());
            }

            loadPartition(domain);
        }
    }

    public void createPartitionConfig(NISDomain domain) throws Exception {

        String domainName = domain.getName();
        log.debug("Creating partition "+domainName+".");

        File sampleDir = new File(project.getWorkDir(), "samples/"+NISTool.NIS_TEMPLATE);

        if (!sampleDir.exists()) {
            project.download("samples/"+NISTool.NIS_TEMPLATE);
        }

        File partitionDir = new File(project.getWorkDir(), "partitions"+File.separator+ domainName);
        FileUtil.copy(sampleDir, partitionDir);

        log.debug("Replacing parameter values.");

        String nisDomain  = domain.getFullName();
        String nisServer  = domain.getServer();
        String ldapSuffix = domain.getSuffix();

        log.debug(" - NIS Domain: "+nisDomain);
        log.debug(" - NIS Server: "+ nisServer);
        log.debug(" - LDAP Suffix: "+ldapSuffix);

        Connection dbConnection = nisPartition.getConnection(NISTool.NIS_CONNECTION_NAME);

        String dbUrl = dbConnection.getParameter("url");
        int i = dbUrl.indexOf("://");
        int j = dbUrl.indexOf("/", i+3);
        if (j < 0) j = dbUrl.length();

        String dbHostPort = dbUrl.substring(i+3, j);
        int k = dbHostPort.indexOf(":");
        String dbHostname, dbPort;
        if (k < 0) {
            dbHostname = dbHostPort;
            dbPort = "3306";
        } else {
            dbHostname = dbHostPort.substring(0, k);
            dbPort = dbHostPort.substring(k+1);
        }

        String dbUser = dbConnection.getParameter("user");
        String dbPassword = dbConnection.getParameter("password");

        PenroseConfig penroseConfig = project.getPenroseConfig();
        String penroseBindDn = penroseConfig.getRootDn().toString();
        String penroseBindPassword = new String(penroseConfig.getRootPassword());

        Connection ldapConnection = nisPartition.getConnection(NISTool.LDAP_CONNECTION_NAME);
        String url = ldapConnection.getParameter(Context.PROVIDER_URL);

        i = url.indexOf("://");
        String ldapProtocol = url.substring(0, i);

        j = url.indexOf("/", i+3);
        String ldapHostPort = url.substring(i+3, j);

        k = ldapHostPort.indexOf(":");
        String ldapHostname, ldapPort;
        if (k < 0) {
            ldapHostname = ldapHostPort;
            ldapPort = "389";
        } else {
            ldapHostname = ldapHostPort.substring(0, k);
            ldapPort = ldapHostPort.substring(k+1);
        }

        String ldapBindDn = ldapConnection.getParameter(Context.SECURITY_PRINCIPAL);
        String ldapBindPassword = ldapConnection.getParameter(Context.SECURITY_CREDENTIALS);
        
        org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();

        antProject.setProperty("DOMAIN",           domainName);

        antProject.setProperty("NIS_SERVER",       nisServer);
        antProject.setProperty("NIS_DOMAIN",       nisDomain);

        antProject.setProperty("DB_SERVER",        dbHostname);
        antProject.setProperty("DB_PORT",          dbPort);
        antProject.setProperty("DB_USER",          dbUser);
        antProject.setProperty("DB_PASSWORD",      dbPassword);

        antProject.setProperty("PENROSE_PROTOCOL", "ldap");
        antProject.setProperty("PENROSE_SERVER",   "localhost");
        antProject.setProperty("PENROSE_PORT",     "10389");
        antProject.setProperty("PENROSE_BASE",     ldapSuffix);
        antProject.setProperty("PENROSE_USER",     penroseBindDn);
        antProject.setProperty("PENROSE_PASSWORD", penroseBindPassword);

        antProject.setProperty("LDAP_PROTOCOL",    ldapProtocol);
        antProject.setProperty("LDAP_SERVER",      ldapHostname);
        antProject.setProperty("LDAP_PORT",        ldapPort);
        antProject.setProperty("LDAP_BASE",        ldapSuffix);
        antProject.setProperty("LDAP_USER",        ldapBindDn);
        antProject.setProperty("LDAP_PASSWORD",    ldapBindPassword);

        RDNBuilder rb = new RDNBuilder();
        rb.set("name", domain.getName());

        DN dn = new DN(rb.toRdn());

        SearchResponse response = schedules.search(dn, null, SearchRequest.SCOPE_BASE);
        SearchResult result = response.next();
        Attributes attributes = result.getAttributes();

        for (Field field : schedules.getFields()) {
            String fieldName = field.getName();
            if ("name".equals(fieldName)) continue;

            Object o = attributes.getValue(fieldName);
            String schedule = o == null ? "" : o.toString();
            String enabled = o == null ? "false" : "true";

            antProject.setProperty(fieldName+".schedule", schedule);
            antProject.setProperty(fieldName+".enabled", enabled);
        }

        Copy copy = new Copy();
        copy.setOverwrite(true);
        copy.setProject(antProject);

        FileSet fs = new FileSet();
        fs.setDir(new File(partitionDir, "template"));
        fs.setIncludes("**/*");
        copy.addFileset(fs);

        copy.setTodir(new File(partitionDir, "DIR-INF"));

        FilterChain filterChain = copy.createFilterChain();
        ExpandProperties expandProperties = new ExpandProperties();
        expandProperties.setProject(antProject);
        filterChain.addExpandProperties(expandProperties);

        copy.execute();

        PartitionConfigs partitionConfigs = project.getPartitionConfigs();
        PartitionConfig partitionConfig = partitionConfigs.load(partitionDir);
        partitionConfigs.addPartitionConfig(partitionConfig);
    }

    public void loadPartition(NISDomain domain) throws Exception {

        String domainName = domain.getName();
        log.debug("Loading partition "+domainName+".");

        File partitionsDir = new File(project.getWorkDir(), "partitions");
        PenroseConfig penroseConfig = project.getPenroseConfig();
        PenroseContext penroseContext = project.getPenroseContext();

        PartitionConfig partitionConfig = project.getPartitionConfigs().getPartitionConfig(domainName);

        PartitionFactory partitionFactory = new PartitionFactory();
        partitionFactory.setPartitionsDir(partitionsDir);
        partitionFactory.setPenroseConfig(penroseConfig);
        partitionFactory.setPenroseContext(penroseContext);

        Partition partition = partitionFactory.createPartition(partitionConfig);

        partitions.addPartition(partition);
    }

    public void createDomain(NISDomain domain) throws Exception {

        String domainName = domain.getName();
        log.debug("Creating domain "+domainName+".");

        RDNBuilder rb = new RDNBuilder();
        rb.set("name", domainName);
        DN dn = new DN(rb.toRdn());

        Attributes attributes = new Attributes();
        attributes.setValue("name", domainName);
        attributes.setValue("fullName", domain.getFullName());
        attributes.setValue("server", domain.getServer());
        attributes.setValue("suffix", domain.getSuffix());

        domains.add(dn, attributes);

        attributes = new Attributes();
        attributes.setValue("name", domainName);
        
        schedules.add(dn, attributes);
        
        nisDomains.put(domainName, domain);

        NISEvent event = new NISEvent();
        event.setDomain(domain);
        
        for (NISEventListener listener : listeners) {
            listener.domainAdded(event);
        }
    }

    public void updateDomain(String oldDomainName, NISDomain domain) throws Exception {

        RDNBuilder rb = new RDNBuilder();
        rb.set("name", domain.getName());
        RDN rdn = rb.toRdn();
        DN dn = new DN(rdn);

        Collection<Modification> modifications = new ArrayList<Modification>();

        modifications.add(new Modification(
                Modification.REPLACE,
                new Attribute("fullName", domain.getFullName())
        ));

        modifications.add(new Modification(
                Modification.REPLACE,
                new Attribute("server", domain.getServer())
        ));

        modifications.add(new Modification(
                Modification.REPLACE,
                new Attribute("suffix", domain.getSuffix())
        ));

        domains.modify(dn, modifications);

        NISEvent event = new NISEvent();
        event.setDomain(domain);

        for (NISEventListener listener : listeners) {
            listener.domainModified(event);
        }
    }

    public void removePartitionConfig(NISDomain domain) throws Exception {

        String domainName = domain.getName();
        log.debug("Removing partition "+domainName+".");

        project.getPartitionConfigs().removePartitionConfig(domainName);

        File partitionDir = new File(project.getWorkDir(), "partitions"+File.separator+ domainName);
        FileUtil.delete(partitionDir);
    }

    public void removePartition(NISDomain domain) throws Exception {

        String domainName = domain.getName();
        log.debug("Removing partition config "+domainName+".");

        partitions.removePartition(domainName);
    }

    public void removeDomain(NISDomain domain) throws Exception {

        String domainName = domain.getName();
        log.debug("Removing domain "+domainName+".");

        nisDomains.remove(domainName);

        File dir = new File(project.getWorkDir(), "partitions"+File.separator+ domain.getName());
        FileUtil.delete(dir);

        RDNBuilder rb = new RDNBuilder();
        rb.set("name", domainName);
        RDN rdn = rb.toRdn();
        DN dn = new DN(rdn);

        schedules.delete(dn);
        domains.delete(dn);

        NISEvent event = new NISEvent();
        event.setDomain(domain);

        for (NISEventListener listener : listeners) {
            listener.domainRemoved(event);
        }
    }

    public Source getDomains() {
        return domains;
    }

    public void setDomains(Source domains) {
        this.domains = domains;
    }

    public Source getActions() {
        return actions;
    }

    public void setActions(Source actions) {
        this.actions = actions;
    }

    public Source getHosts() {
        return hosts;
    }

    public void setHosts(Source hosts) {
        this.hosts = hosts;
    }

    public Source getFiles() {
        return files;
    }

    public void setFiles(Source files) {
        this.files = files;
    }

    public Source getChanges() {
        return changes;
    }

    public void setChanges(Source changes) {
        this.changes = changes;
    }

    public Source getUsers() {
        return users;
    }

    public void setUsers(Source users) {
        this.users = users;
    }

    public Source getGroups() {
        return groups;
    }

    public void setGroups(Source groups) {
        this.groups = groups;
    }

    public Map<String, NISDomain> getNisDomains() {
        return nisDomains;
    }

    public void setNisDomains(Map<String, NISDomain> nisDomains) {
        this.nisDomains = nisDomains;
    }

    public Partitions getPartitions() {
        return partitions;
    }

    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }

    public Partition getNisPartition() {
        return nisPartition;
    }

    public void setNisPartition(Partition nisPartition) {
        this.nisPartition = nisPartition;
    }


    public Collection<SourceConfig> getCacheConfigs(SourceConfigs sourceConfigs, SourceConfig sourceConfig) {

        Collection<SourceConfig> cacheConfigs = new ArrayList<SourceConfig>();

        SourceSyncConfig sourceSyncConfig = sourceConfigs.getSourceSyncConfig(sourceConfig.getName());
        if (sourceSyncConfig == null) return cacheConfigs;

        for (String name : sourceSyncConfig.getDestinations()) {
            SourceConfig cacheConfig = sourceConfigs.getSourceConfig(name);
            cacheConfigs.add(cacheConfig);
        }

        return cacheConfigs;
    }

    public void  createDatabase(NISDomain domain) throws Exception {

        log.debug("Creating database "+domain.getName()+".");

        Connection connection = nisPartition.getConnection(NIS_CONNECTION_NAME);
        JDBCAdapter adapter = (JDBCAdapter)connection.getAdapter();
        JDBCClient client = adapter.getClient();

        try {
            client.createDatabase(domain.getName());
        } catch (Exception e) {
            log.debug(e.getMessage());
        }

        PartitionConfig partitionConfig = project.getPartitionConfigs().getPartitionConfig(domain.getName());
        SourceConfigs sourceConfigs = partitionConfig.getSourceConfigs();

        for (SourceConfig sourceConfig : sourceConfigs.getSourceConfigs()) {
            if (!CACHE_CONNECTION_NAME.equals(sourceConfig.getConnectionName())) continue;

            try {
                client.createTable(sourceConfig);
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }
    }

    public void removeCache(NISDomain domain) throws Exception {

        log.debug("Removing cache "+domain.getName()+".");

        Connection connection = nisPartition.getConnection(NIS_CONNECTION_NAME);
        JDBCAdapter adapter = (JDBCAdapter)connection.getAdapter();
        JDBCClient client = adapter.getClient();
        client.dropDatabase(domain.getName());
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void addNISListener(NISEventListener listener) {
        listeners.add(listener);
    }

    public void removeNISListener(NISEventListener listener) {
        listeners.remove(listener);
    }

    public Source getSchedules() {
        return schedules;
    }

    public void setSchedules(Source schedules) {
        this.schedules = schedules;
    }
}
