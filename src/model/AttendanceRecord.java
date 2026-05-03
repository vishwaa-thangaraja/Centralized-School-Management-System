package model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;

public class AttendanceRecord {
    private final StringProperty date;
    private final StringProperty fnStatus;
    private final StringProperty anStatus;
    private final StringProperty remark;
    private final BooleanProperty editable;

    public AttendanceRecord(String date, String fn, String an, String remark) {
        this.date = new SimpleStringProperty(date);
        this.fnStatus = new SimpleStringProperty(fn);
        this.anStatus = new SimpleStringProperty(an);
        this.remark = new SimpleStringProperty(remark);
        this.editable = new SimpleBooleanProperty(true);
    }

    public String getDate() { return date.get(); }
    public void setDate(String value) { date.set(value); }
    public StringProperty dateProperty() { return date; }

    public String getFnStatus() { return fnStatus.get(); }
    public void setFnStatus(String value) { fnStatus.set(value); }
    public StringProperty fnStatusProperty() { return fnStatus; }

    public String getAnStatus() { return anStatus.get(); }
    public void setAnStatus(String value) { anStatus.set(value); }
    public StringProperty anStatusProperty() { return anStatus; }

    public String getRemark() { return remark.get(); }
    public void setRemark(String value) { remark.set(value); }
    public StringProperty remarkProperty() { return remark; }

    public boolean isEditable() { return editable.get(); }
    public void setEditable(boolean value) { editable.set(value); }
    public BooleanProperty editableProperty() { return editable; }
}
