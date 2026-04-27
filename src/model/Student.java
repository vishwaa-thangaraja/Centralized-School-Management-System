package model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public class Student {
    private final IntegerProperty studentId;
    private final IntegerProperty userId;
    private final IntegerProperty classId;
    private final StringProperty name;
    private final StringProperty email;
    private final StringProperty phone;
    private final StringProperty classDisplay;
    private final StringProperty dob;
    private final StringProperty gender;
    private final StringProperty conduct;
    private final StringProperty conductRemarks;

    public Student() {
        this(0, 0, 0, "", "", "", "", "", "", "", "");
    }

    public Student(
            int studentId,
            int userId,
            int classId,
            String name,
            String email,
            String phone,
            String classDisplay,
            String dob,
            String gender,
            String conduct,
            String conductRemarks
    ) {
        this.studentId = new SimpleIntegerProperty(studentId);
        this.userId = new SimpleIntegerProperty(userId);
        this.classId = new SimpleIntegerProperty(classId);
        this.name = new SimpleStringProperty(name);
        this.email = new SimpleStringProperty(email);
        this.phone = new SimpleStringProperty(phone);
        this.classDisplay = new SimpleStringProperty(classDisplay);
        this.dob = new SimpleStringProperty(dob);
        this.gender = new SimpleStringProperty(gender);
        this.conduct = new SimpleStringProperty(conduct);
        this.conductRemarks = new SimpleStringProperty(conductRemarks);
    }

    public int getStudentId() { return studentId.get(); }
    public void setStudentId(int value) { studentId.set(value); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public int getUserId() { return userId.get(); }
    public void setUserId(int value) { userId.set(value); }
    public IntegerProperty userIdProperty() { return userId; }

    public int getClassId() { return classId.get(); }
    public void setClassId(int value) { classId.set(value); }
    public IntegerProperty classIdProperty() { return classId; }

    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String value) { phone.set(value); }
    public StringProperty phoneProperty() { return phone; }

    public String getClassDisplay() { return classDisplay.get(); }
    public void setClassDisplay(String value) { classDisplay.set(value); }
    public StringProperty classDisplayProperty() { return classDisplay; }

    public String getDob() { return dob.get(); }
    public void setDob(String value) { dob.set(value); }
    public StringProperty dobProperty() { return dob; }

    public String getGender() { return gender.get(); }
    public void setGender(String value) { gender.set(value); }
    public StringProperty genderProperty() { return gender; }

    public String getConduct() { return conduct.get(); }
    public void setConduct(String value) { conduct.set(value); }
    public StringProperty conductProperty() { return conduct; }

    public String getConductRemarks() { return conductRemarks.get(); }
    public void setConductRemarks(String value) { conductRemarks.set(value); }
    public StringProperty conductRemarksProperty() { return conductRemarks; }
}
