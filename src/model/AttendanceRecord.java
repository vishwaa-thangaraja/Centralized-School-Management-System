package model;

import javafx.beans.property.SimpleStringProperty;

public class AttendanceRecord {
    private final SimpleStringProperty date;
    private final SimpleStringProperty fnStatus;
    private final SimpleStringProperty anStatus;
    private final SimpleStringProperty remark;

    public AttendanceRecord(String date, String fn, String an, String remark) {
        this.date = new SimpleStringProperty(date);
        this.fnStatus = new SimpleStringProperty(fn);
        this.anStatus = new SimpleStringProperty(an);
        this.remark = new SimpleStringProperty(remark);
    }

    public String getDate() { return date.get(); }
    public String getFnStatus() { return fnStatus.get(); }
    public String getAnStatus() { return anStatus.get(); }
    public String getRemark() { return remark.get(); }
}