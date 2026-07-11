# CSMS Table Structures

## 3.6.1 Roles Structure:

**Table 3.1: Roles Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| role_id | Number |  | PRIMARY KEY | Unique role id |
| role_name | String | 20 | UNIQUE<br>NOT NULL | Role name |

## 3.6.2 Users Structure:

**Table 3.2: Users Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| user_id | Number |  | PRIMARY KEY | Unique user id |
| name | String | 100 | NOT NULL | Full name of the user |
| email | String | 100 | UNIQUE<br>NOT NULL | User email address |
| password_hash | String | 255 | NOT NULL | Encrypted user password |
| role_id | Number |  | NOT NULL<br>FOREIGN KEY -> ROLES(ROLE_ID) | Related role id |
| phone | String | 15 | - | User contact number |
| created_at | Date |  | DEFAULT SYSDATE | Account creation date |
| is_active | Number | 1 | NOT NULL<br>DEFAULT 1<br>CHECK (IS_ACTIVE IN (0, 1)) | User active status |

## 3.6.3 Classes Structure:

**Table 3.3: Classes Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| class_id | Number |  | PRIMARY KEY | Unique class id |
| class_name | String | 20 | NOT NULL<br>UNIQUE: CLASS_NAME, SECTION, ACADEMIC_YEAR | Class name |
| section | String | 5 | NOT NULL<br>UNIQUE: CLASS_NAME, SECTION, ACADEMIC_YEAR | Class section |
| academic_year | String | 10 | NOT NULL<br>UNIQUE: CLASS_NAME, SECTION, ACADEMIC_YEAR | Academic year |

## 3.6.4 Subjects Structure:

**Table 3.4: Subjects Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| subject_id | Number |  | PRIMARY KEY | Unique subject id |
| subject_name | String | 100 | UNIQUE<br>NOT NULL | Subject name |

## 3.6.5 Teachers Structure:

**Table 3.5: Teachers Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| teacher_id | Number |  | PRIMARY KEY | Unique teacher id |
| user_id | Number |  | UNIQUE<br>FOREIGN KEY -> USERS(USER_ID) | Related user id |
| qualification | String | 100 | - | Teacher qualification |
| experience | Number |  | CHECK (EXPERIENCE >= 0) | Teaching experience in years |

## 3.6.6 Students Structure:

**Table 3.6: Students Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| student_id | Number |  | PRIMARY KEY | Unique student id |
| user_id | Number |  | UNIQUE<br>NOT NULL<br>FOREIGN KEY -> USERS(USER_ID) | Related user id |
| dob | Date |  | NOT NULL | Student date of birth |
| gender | String | 10 | CHECK (GENDER IN ('Male','Female','Other')) | Student gender |
| conduct | String | 20 | CHECK (CONDUCT IN ('Excellent','Good','Average','Poor')) | Student conduct grade |
| conduct_remarks | String | 200 | - | Remarks about student conduct |

## 3.6.7 Class Subject Teacher Structure:

**Table 3.7: Class Subject Teacher Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| class_id | Number |  | PRIMARY KEY<br>FOREIGN KEY -> CLASSES(CLASS_ID) | Related class id |
| subject_id | Number |  | PRIMARY KEY<br>FOREIGN KEY -> SUBJECTS(SUBJECT_ID) | Related subject id |
| teacher_id | Number |  | PRIMARY KEY<br>FOREIGN KEY -> TEACHERS(TEACHER_ID) | Related teacher id |

## 3.6.8 Student Class Structure:

**Table 3.8: Student Class Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| student_id | Number |  | PRIMARY KEY<br>FOREIGN KEY -> STUDENTS(STUDENT_ID) | Related student id |
| class_id | Number |  | PRIMARY KEY<br>FOREIGN KEY -> CLASSES(CLASS_ID) | Related class id |

## 3.6.9 Parent Student Structure:

**Table 3.9: Parent Student Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| parent_id | Number |  | PRIMARY KEY<br>FOREIGN KEY -> USERS(USER_ID) | Related parent id |
| student_id | Number |  | PRIMARY KEY<br>FOREIGN KEY -> STUDENTS(STUDENT_ID) | Related student id |
| relation | String | 50 | NOT NULL | Relationship with student |

## 3.6.10 Question Papers Structure:

**Table 3.10: Question Papers Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| qp_id | Number |  | PRIMARY KEY | Unique qp id |
| class_id | Number |  | NOT NULL<br>UNIQUE: CLASS_ID, SUBJECT_ID, EXAM_DATE<br>FOREIGN KEY -> CLASSES(CLASS_ID) | Related class id |
| subject_id | Number |  | NOT NULL<br>UNIQUE: CLASS_ID, SUBJECT_ID, EXAM_DATE<br>FOREIGN KEY -> SUBJECTS(SUBJECT_ID) | Related subject id |
| exam_type | String | 50 | - | Type or title of exam |
| exam_date | Date |  | NOT NULL<br>UNIQUE: CLASS_ID, SUBJECT_ID, EXAM_DATE | Date of examination |
| max_marks | Number |  | NOT NULL<br>CHECK (MAX_MARKS > 0) | Maximum marks for exam |
| exam_description | String | 500 | - | Description of the exam |
| created_by_teacher_id | Number |  | FOREIGN KEY -> TEACHERS(TEACHER_ID) | Related created by teacher id |

## 3.6.11 Question Bank Structure:

**Table 3.11: Question Bank Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| question_id | Number |  | PRIMARY KEY | Unique question id |
| class_id | Number |  | NOT NULL<br>FOREIGN KEY -> CLASSES(CLASS_ID) | Related class id |
| subject_id | Number |  | NOT NULL<br>FOREIGN KEY -> SUBJECTS(SUBJECT_ID) | Related subject id |
| teacher_id | Number |  | FOREIGN KEY -> TEACHERS(TEACHER_ID) | Related teacher id |
| title | String | 200 | NOT NULL | Title of assignment or question paper |
| academic_year | String | 10 | NOT NULL | Academic year |
| original_file_name | String | 255 | - | Original uploaded file name |
| uploaded_at | Date |  | DEFAULT SYSDATE | File uploaded date |

## 3.6.12 Assignments Structure:

**Table 3.12: Assignments Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| assignment_id | Number |  | PRIMARY KEY | Unique assignment id |
| class_id | Number |  | FOREIGN KEY -> CLASSES(CLASS_ID) | Related class id |
| subject_id | Number |  | FOREIGN KEY -> SUBJECTS(SUBJECT_ID) | Related subject id |
| title | String | 200 | NOT NULL | Title of assignment or question paper |
| description | String | 500 | - | Assignment description |
| due_date | Date |  | NOT NULL | Assignment due date |

## 3.6.13 Marks Structure:

**Table 3.13: Marks Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| mark_id | Number |  | PRIMARY KEY | Unique mark id |
| student_id | Number |  | NOT NULL<br>UNIQUE: STUDENT_ID, QP_ID<br>FOREIGN KEY -> STUDENTS(STUDENT_ID) | Related student id |
| qp_id | Number |  | NOT NULL<br>UNIQUE: STUDENT_ID, QP_ID<br>FOREIGN KEY -> QUESTION_PAPERS(QP_ID) | Related qp id |
| marks_obtained | Number |  | CHECK (MARKS_OBTAINED >= 0) | Marks obtained by student |

## 3.6.14 Submissions Structure:

**Table 3.14: Submissions Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| submission_id | Number |  | PRIMARY KEY | Unique submission id |
| assignment_id | Number |  | UNIQUE: ASSIGNMENT_ID, STUDENT_ID<br>FOREIGN KEY -> ASSIGNMENTS(ASSIGNMENT_ID) | Related assignment id |
| student_id | Number |  | UNIQUE: ASSIGNMENT_ID, STUDENT_ID<br>FOREIGN KEY -> STUDENTS(STUDENT_ID) | Related student id |
| submitted_on | Date |  | - | Submission date |
| marks | Number |  | CHECK (MARKS >= 0) | Marks awarded for submission |

## 3.6.15 Attendance Structure:

**Table 3.15: Attendance Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| attendance_id | Number |  | PRIMARY KEY | Unique attendance id |
| user_id | Number |  | NOT NULL<br>UNIQUE: USER_ID, CLASS_ID, ATTENDANCE_DATE, SESSION_TYPE<br>FOREIGN KEY -> USERS(USER_ID) | Related user id |
| class_id | Number |  | UNIQUE: USER_ID, CLASS_ID, ATTENDANCE_DATE, SESSION_TYPE<br>FOREIGN KEY -> CLASSES(CLASS_ID) | Related class id |
| attendance_date | Date |  | NOT NULL<br>UNIQUE: USER_ID, CLASS_ID, ATTENDANCE_DATE, SESSION_TYPE | Attendance date |
| session_type | String | 2 | NOT NULL<br>UNIQUE: USER_ID, CLASS_ID, ATTENDANCE_DATE, SESSION_TYPE<br>CHECK (SESSION_TYPE IN ('FN','AN')) | Attendance session type |
| status | String | 10 | NOT NULL<br>CHECK (STATUS IN ('Present','Absent','Leave')) | Current status |
| leave_reason | String | 200 | - | Reason for leave |
| approval_status | String | 10 | CHECK (APPROVAL_STATUS IN ('Pending','Approved','Rejected')) | Leave approval status |
| approved_by | Number |  | FOREIGN KEY -> USERS(USER_ID) | Related approved by |

## 3.6.16 Counselling Structure:

**Table 3.16: Counselling Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| session_id | Number |  | PRIMARY KEY | Unique session id |
| student_id | Number |  | FOREIGN KEY -> STUDENTS(STUDENT_ID) | Related student id |
| counsellor_id | Number |  | FOREIGN KEY -> USERS(USER_ID) | Related counsellor id |
| session_date | Date |  | - | Counselling session date |
| notes | String | 500 | - | Counselling notes |
| status | String | 15 | NOT NULL<br>DEFAULT 'Pending'<br>CHECK (STATUS IN ('Pending','Scheduled','Completed')) | Current status |
| category | String | 20 | NOT NULL<br>DEFAULT 'Academic'<br>CHECK (CATEGORY IN ('Academic','Personal','Career')) | Counselling category |

## 3.6.17 Communication Structure:

**Table 3.17: Communication Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| message_id | Number |  | PRIMARY KEY | Unique message id |
| sender_id | Number |  | FOREIGN KEY -> USERS(USER_ID) | Related sender id |
| receiver_id | Number |  | FOREIGN KEY -> USERS(USER_ID) | Related receiver id |
| student_id | Number |  | FOREIGN KEY -> STUDENTS(STUDENT_ID) | Related student id |
| message_text | String | 1000 | NOT NULL | Message content |
| sent_at | Timestamp |  | DEFAULT CURRENT_TIMESTAMP | Message sent timestamp |

## 3.6.18 Communication Read State Structure:

**Table 3.18: Communication Read State Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| viewer_id | Number |  | NOT NULL<br>PRIMARY KEY<br>FOREIGN KEY -> USERS(USER_ID) | Related viewer id |
| partner_id | Number |  | NOT NULL<br>PRIMARY KEY<br>FOREIGN KEY -> USERS(USER_ID) | Related partner id |
| student_id | Number |  | NOT NULL<br>PRIMARY KEY<br>FOREIGN KEY -> STUDENTS(STUDENT_ID) | Related student id |
| last_seen_message_id | Number |  | NOT NULL<br>DEFAULT 0<br>CHECK (LAST_SEEN_MESSAGE_ID >= 0) | Last message seen by viewer |
| last_seen_at | Timestamp |  | NOT NULL<br>DEFAULT CURRENT_TIMESTAMP | Last seen timestamp |

## 3.6.19 Login Audit Structure:

**Table 3.19: Login Audit Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| log_id | Number |  | PRIMARY KEY | Unique log id |
| user_id | Number |  | FOREIGN KEY -> USERS(USER_ID) | Related user id |
| login_time | Date |  | DEFAULT SYSDATE<br>CHECK (LOGOUT_TIME IS NULL OR LOGOUT_TIME >= LOGIN_TIME) | Login time |
| logout_time | Date |  | CHECK (LOGOUT_TIME IS NULL OR LOGOUT_TIME >= LOGIN_TIME) | Logout time |
| ip_address | String | 50 | - | Login IP address |

## 3.6.20 App Settings Structure:

**Table 3.20: App Settings Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| setting_key | String | 50 | PRIMARY KEY | Unique setting key |
| setting_value | String | 500 | - | Stored setting value |

## 3.6.21 User Profile Images Structure:

**Table 3.21: User Profile Images Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| user_id | Number |  | PRIMARY KEY<br>FOREIGN KEY -> USERS(USER_ID) | Related user id |
| image_data | BLOB |  | NOT NULL | Binary image data |
| mime_type | String | 80 | - | Image MIME type |
| updated_at | Date |  | NOT NULL<br>DEFAULT SYSDATE | Last updated date |

## 3.6.22 School Profile Images Structure:

**Table 3.22: School Profile Images Description**

| Attribute Name | Type | Width | Constraint(s) | Description |
|---|---|---:|---|---|
| image_id | Number |  | PRIMARY KEY<br>CHECK (IMAGE_ID = 1) | Unique image id |
| image_data | BLOB |  | NOT NULL | Binary image data |
| mime_type | String | 80 | - | Image MIME type |
| updated_at | Date |  | NOT NULL<br>DEFAULT SYSDATE | Last updated date |
