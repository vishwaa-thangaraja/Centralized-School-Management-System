package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AdminAuditRecord {
    private final IntegerProperty logId;
    private final StringProperty userName;
    private final StringProperty roleName;
    private final StringProperty loginTime;
    private final StringProperty logoutTime;
    private final StringProperty ipAddress;

    public AdminAuditRecord(int logId, String userName, String roleName, String loginTime, String logoutTime, String ipAddress) {
        this.logId = new SimpleIntegerProperty(logId);
        this.userName = new SimpleStringProperty(userName);
        this.roleName = new SimpleStringProperty(roleName);
        this.loginTime = new SimpleStringProperty(loginTime);
        this.logoutTime = new SimpleStringProperty(logoutTime);
        this.ipAddress = new SimpleStringProperty(ipAddress);
    }

    public int getLogId() { return logId.get(); }
    public IntegerProperty logIdProperty() { return logId; }

    public String getUserName() { return userName.get(); }
    public StringProperty userNameProperty() { return userName; }

    public String getRoleName() { return roleName.get(); }
    public StringProperty roleNameProperty() { return roleName; }

    public String getLoginTime() { return loginTime.get(); }
    public StringProperty loginTimeProperty() { return loginTime; }

    public String getLogoutTime() { return logoutTime.get(); }
    public StringProperty logoutTimeProperty() { return logoutTime; }

    public String getIpAddress() { return ipAddress.get(); }
    public StringProperty ipAddressProperty() { return ipAddress; }
}
