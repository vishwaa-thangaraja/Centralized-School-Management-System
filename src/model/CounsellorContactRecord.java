package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CounsellorContactRecord {
    private final IntegerProperty counsellorUserId;
    private final StringProperty counsellorName;
    private final StringProperty email;
    private final StringProperty phone;
    private final IntegerProperty unreadCount;

    public CounsellorContactRecord(int counsellorUserId, String counsellorName, String email, String phone, int unreadCount) {
        this.counsellorUserId = new SimpleIntegerProperty(counsellorUserId);
        this.counsellorName = new SimpleStringProperty(counsellorName);
        this.email = new SimpleStringProperty(email);
        this.phone = new SimpleStringProperty(phone);
        this.unreadCount = new SimpleIntegerProperty(unreadCount);
    }

    public int getCounsellorUserId() { return counsellorUserId.get(); }
    public IntegerProperty counsellorUserIdProperty() { return counsellorUserId; }

    public String getCounsellorName() { return counsellorName.get(); }
    public StringProperty counsellorNameProperty() { return counsellorName; }

    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }

    public String getPhone() { return phone.get(); }
    public StringProperty phoneProperty() { return phone; }

    public int getUnreadCount() { return unreadCount.get(); }
    public void setUnreadCount(int unreadCount) { this.unreadCount.set(unreadCount); }
    public IntegerProperty unreadCountProperty() { return unreadCount; }
    public String getUnreadDisplay() { return getUnreadCount() <= 0 ? "" : String.valueOf(getUnreadCount()); }
}
