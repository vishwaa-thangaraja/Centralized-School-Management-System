package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AdminUserRecord {
    private final IntegerProperty userId;
    private final StringProperty name;
    private final StringProperty email;
    private final StringProperty roleName;
    private final StringProperty phone;
    private final IntegerProperty isActive;

    public AdminUserRecord(int userId, String name, String email, String roleName, String phone, int isActive) {
        this.userId = new SimpleIntegerProperty(userId);
        this.name = new SimpleStringProperty(name);
        this.email = new SimpleStringProperty(email);
        this.roleName = new SimpleStringProperty(roleName);
        this.phone = new SimpleStringProperty(phone);
        this.isActive = new SimpleIntegerProperty(isActive);
    }

    public int getUserId() { return userId.get(); }
    public IntegerProperty userIdProperty() { return userId; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }

    public String getRoleName() { return roleName.get(); }
    public StringProperty roleNameProperty() { return roleName; }

    public String getPhone() { return phone.get(); }
    public StringProperty phoneProperty() { return phone; }

    public int getIsActive() { return isActive.get(); }
    public IntegerProperty isActiveProperty() { return isActive; }

    public String getActiveText() { return getIsActive() == 1 ? "Active" : "Inactive"; }
}
