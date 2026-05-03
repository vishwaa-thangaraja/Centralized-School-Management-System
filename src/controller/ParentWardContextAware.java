package controller;

import model.Student;
import model.User;

public interface ParentWardContextAware {
    void updateContext(User parentUser, Student selectedWard);
}
