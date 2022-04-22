package cn.cnic.zhongtai.system.model;

public class DataAddToFtpToHdfs {
    private Integer id;

    private String dataImportTaskName;

    private String protocol;

    private String host;

    private Integer port;

    private String username;

    private String password;

    private String path;

    private String suffix;

    private String hdfsPath;

    private String dataImportTaskId;

    private String cron;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDataImportTaskName() {
        return dataImportTaskName;
    }

    public void setDataImportTaskName(String dataImportTaskName) {
        this.dataImportTaskName = dataImportTaskName == null ? null : dataImportTaskName.trim();
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol == null ? null : protocol.trim();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host == null ? null : host.trim();
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path == null ? null : path.trim();
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix == null ? null : suffix.trim();
    }

    public String getHdfsPath() {
        return hdfsPath;
    }

    public void setHdfsPath(String hdfsPath) {
        this.hdfsPath = hdfsPath == null ? null : hdfsPath.trim();
    }

    public String getDataImportTaskId() {
        return dataImportTaskId;
    }

    public void setDataImportTaskId(String dataImportTaskId) {
        this.dataImportTaskId = dataImportTaskId == null ? null : dataImportTaskId.trim();
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron == null ? null : cron.trim();
    }
}