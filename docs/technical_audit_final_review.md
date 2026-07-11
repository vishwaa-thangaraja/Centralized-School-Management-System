# CSMS Technical Audit for Final Review

This project is a Java desktop application built with JavaFX for the UI, Oracle Database for persistence, JDBC for database connectivity, Jakarta Mail/Angus Mail for OTP email, and iText for generating PDF reports. The code is organized in a layered style:

- `src/view`: JavaFX FXML layout files. These define screens, controls, tables, labels, charts, buttons, and forms.
- `src/controller`: Java controller classes. These receive UI events from FXML, call DAO/service methods, and update JavaFX controls.
- `src/dao`: Java DAO classes. These contain JDBC code and SQL queries for OracleDB.
- `src/service`: Java service/helper classes. These handle reusable logic such as authentication state, email, file copying, themes, settings, OTP, and exam grading.
- `src/model`: Java model/record classes. These are simple data containers used by JavaFX tables and controller logic.

Important review sentence:

> This is not a Swing project. There is no `JTabbedPane`. The UI is JavaFX, defined by FXML files and controlled by Java controller classes. Navigation is mostly sidebar/menu buttons that load another FXML screen using `FXMLLoader`.

## 1. Component Mapping

Every visual element below is JavaFX. The behavior behind it is Java controller code.

| Screen / FXML | Java Controller | Visual JavaFX Components | Java Logic Behind It | What To Say In Review |
|---|---|---|---|---|
| Login screen: `login.fxml` | `LoginController.java` | `StackPane`, `VBox`, `HBox`, `TextField`, `PasswordField`, `Button`, `Hyperlink`, `ProgressIndicator`, `Label` | Java controller validates email/password through `UserDAO.validateUser`, stores session in `AuthService`, then loads the correct dashboard FXML. | "The login UI is JavaFX FXML. The logic is Java code in `LoginController`; it hashes the password and validates it using Oracle through JDBC." |
| Forgot password: `forgot_password.fxml` | `ForgotPasswordController.java` | `TextField`, `PasswordField`, `Button`, `Hyperlink`, `Label`, `VBox/HBox` | Java controller generates OTP using `OTPGenerator`, sends email through `EmailSender`, then updates password using `UserDAO.updatePassword`. | "OTP reset is a JavaFX form controlled by Java; email is handled through Jakarta Mail SMTP." |
| Admin dashboard: `admin_dashboard.fxml` | `AdminDashboardController.java` | `StackPane`, `ScrollPane`, sidebar `VBox`, overlay `Pane`, stat-card `VBox`, `TilePane`, `Label`, `Button` | Java controller loads admin stats using `AdminStatsDAO`, toggles sidebar animation, and loads subviews with `FXMLLoader`. | "Dashboard cards are JavaFX `VBox/Label` components styled with CSS, not custom Swing widgets." |
| Admin user management: `admin_user_management.fxml` | `AdminUserManagementController.java` | `ComboBox`, `TextField`, `PasswordField`, `DatePicker`, `TextArea`, `TableView`, `TableColumn`, `Button` | Java code creates users, edits selected users, toggles active status, and fills tables through `UserDAO`. | "The table is JavaFX `TableView`; records come from Oracle and are represented by `AdminUserRecord` model objects." |
| Admin academic mapping: `admin_academic_mapping.fxml` | `AdminAcademicMappingController.java` | `ComboBox`, `TextField`, multiple `TableView`s, `Button`, `Label` | Java controller maps teachers to subjects/classes, moves students between classes, links parents to students, and deletes users through `UserDAO`. | "Mapping screens are JavaFX controls, while actual relationships are saved in Oracle join tables." |
| Admin logs: `admin_logs.fxml` | `AdminLogsController.java` | `TableView`, `TableColumn`, `Button`, `Label` | Java controller displays login audit records from `LoginAuditDAO`, supports delete selected and clear all. | "System logs are database records displayed in a JavaFX table." |
| Admin settings: `admin_settings.fxml` | `AdminSettingsController.java` | `GridPane`, `TextField`, `TextArea`, `MenuButton`, `MenuItem`, `Button` | Java code saves school name/address/phone/email/theme into `APP_SETTINGS` via `AppSettingsDAO`. | "Settings are not hardcoded; they are persisted in Oracle and cached by `SchoolSettingsService`." |
| Student dashboard: `student_dashboard.fxml` | `StudentDashboardController.java` | `StackPane`, `ScrollPane`, sidebar `VBox`, profile `GridPane`, stat `FlowPane`, `LineChart`, `Button`, `Label` | Java controller loads student profile, attendance percentage, marks, pending assignments, upcoming exams, chat badges. | "Charts are JavaFX `LineChart`; values come from DAO queries." |
| Student attendance: `attendance_view.fxml` | `AttendanceController.java` | `BarChart`, `CategoryAxis`, `NumberAxis`, `TableView`, `GridPane`, labels | Java controller reads attendance history from `UserDAO.getSchoolAttendance`, calculates total/present/percentage, fills chart and table. | "Attendance is shown through JavaFX chart/table, backed by Oracle `ATTENDANCE` records." |
| Student performance: `performance_view.fxml` | `PerformanceController.java` | `LineChart`, `TableView`, `Button` | Java controller loads marks using `UserDAO.getStudentMarks`, displays graph/table, and generates a PDF using iText. | "PDF download is generated dynamically from table data using iText." |
| Student assignments: `student_assignments_view.fxml` | `StudentAssignmentsController.java` | `TableView`, details `GridPane`, action `Button`s, `Label`s | Java controller lists assignments, opens assignment PDFs, uploads solution PDFs, and records submission metadata through `UserDAO`. | "PDF files are stored in project folders; only assignment/submission metadata is in Oracle." |
| Student question bank: `student_question_bank_view.fxml` | `StudentQuestionBankController.java` | `TableView`, `GridPane`, `Button`, `Label` | Java controller lists available question papers using `QuestionBankDAO`, opens/downloads files through `QuestionBankFileService`. | "Question bank metadata is in DB; actual PDF is a file under `folders/question_bank`." |
| Student counsellor connect: `student_counsellor_connect_view.fxml` | `StudentCounsellorConnectController.java` | `TableView`, `ScrollPane`, chat `VBox`, `TextArea`, `Button` | Java controller loads counsellors, reads/writes messages through `COMMUNICATION`, and updates read state. | "Chat is database-backed, not WebSocket." |
| Counselling request: `counselling_request_view.fxml` | `CounsellingRequestController.java` | `DatePicker`, `ComboBox`, `TextArea`, `Button`, `Label` | Java controller validates selected date/category/concern and inserts a request into `COUNSELLING`. | "Counselling requests are normal Oracle rows with status values like Pending/Accepted/Completed." |
| Teacher dashboard: `teacher_dashboard.fxml` | `TeacherDashboardController.java` | `StackPane`, `ScrollPane`, sidebar `VBox`, profile `GridPane`, stat `FlowPane`, labels/buttons | Java controller loads teacher profile, student/class/assignment counts, upcoming exam notifications, and parent-chat badge. | "The dashboard is JavaFX FXML; dashboard numbers are SQL counts." |
| Teacher attendance: `teacher_attendance_view.fxml` | `TeacherAttendanceController.java` | `ComboBox`, `DatePicker`, editable `TableView`, `ComboBoxTableCell`, `TextFieldTableCell`, `BarChart` | Java code allows selecting students, queueing attendance for FN/AN, editing existing rows, and saving to `ATTENDANCE`. | "Editable table cells are JavaFX table cell factories; saving uses JDBC updates/inserts." |
| Teacher student management: `teacher_students_view.fxml` | `TeacherStudentManagementController.java` | `TableView`, `TextField`, `ComboBox`, `TextArea`, `Button` | Java controller lists students in teacher scope and updates class/conduct through `UserDAO`. | "Teacher can edit only students mapped to their assigned class/subject scope." |
| Teacher student performance: `teacher_performance_view.fxml` | `TeacherStudentPerformanceController.java` | `TableView`, summary `GridPane`, `Button`, `Label` | Java controller loads student marks/attendance/conduct and generates selected/all PDF reports using iText. | "Performance report generation is local Java code, not a stored DB file." |
| Teacher assignments: `teacher_assignments_view.fxml` | `TeacherAssignmentsController.java` | `ComboBox`, `TextField`, `TextArea`, multiple `TableView`s, PDF action `Button`s | Java controller creates assignments, chooses PDF through `FileChooser`, stores PDF in folder, and saves metadata in Oracle. | "Teacher assignment upload is file-system copy plus DB insert." |
| Teacher question bank: `teacher_question_bank_view.fxml` | `TeacherQuestionBankController.java` | `ComboBox`, `TextField`, `TableView`, `Button`, `Label` | Java controller chooses a PDF, copies it to question-bank folder, then inserts question metadata using `QuestionBankDAO`. | "This separates large binary file storage from searchable database metadata." |
| Teacher parent chat: `teacher_connect_view.fxml` | `TeacherConnectController.java` | `TableView`, `ScrollPane`, chat `VBox`, `TextArea`, `Button` | Java controller loads parent threads, messages, unread badges, and sends replies through `UserDAO`. | "Teacher chat is stored in `COMMUNICATION`; unread count uses `COMMUNICATION_READ_STATE`." |
| Parent dashboard: `parent_dashboard.fxml` | `ParentDashboardController.java` | `ComboBox`, stat labels, `TableView`s, sidebar, badge buttons, `Button` for PDF | Java controller selects ward, loads attendance/alerts/conduct, shows chat badges, and exports performance PDF. | "Parent dashboard changes context based on selected ward." |
| Parent teacher connect: `parent_teacher_connect.fxml` | `ParentTeacherConnectController.java` | `TableView`, `ScrollPane`, chat `VBox`, `TextArea`, `Button` | Java controller loads ward's teachers, shows message history, inserts messages, and clears chat. | "Parent-to-teacher communication is DB insert/select logic." |
| Parent counsellor connect: `parent_counsellor_connect_view.fxml` | `ParentCounsellorConnectController.java` | `TableView`, `ScrollPane`, chat `VBox`, `TextArea`, `Button` | Java controller loads counsellors for selected ward and exchanges messages using the shared communication table. | "Same chat architecture, different participant roles." |
| Counsellor dashboard: `counsellor_dashboard.fxml` | `CounsellorDashboardController.java` | `StackPane`, sidebar, profile `GridPane`, stat `FlowPane`, badge labels | Java controller loads profile, pending requests, active cases, and chat inbox notification counts. | "Counsellor dashboard numbers come from `COUNSELLING` and `COMMUNICATION` queries." |
| Counsellor requests: `counsellor_requests_view.fxml` | `CounsellorRequestsController.java` | `TableView`, `TableRow`, action `Button`s | Java controller loads counselling requests and updates status to Accepted/Completed. | "Status transitions are controlled in Java and persisted in Oracle." |
| Counsellor chat: `counsellor_chat_view.fxml` | `CounsellorChatController.java` | `TableView`, `ScrollPane`, chat `VBox`, `TextArea`, `Button` | Java controller loads parent/student participants, message history, unread counts, and sends replies. | "Counsellor chat reuses the same `COMMUNICATION` table." |

## 2. Workflow Logic

### 2.1 Application Start and Navigation

- `Main.java` extends JavaFX `Application`.
- `start(Stage primaryStage)` loads `/view/login.fxml` using `FXMLLoader`.
- The root JavaFX node is placed inside a `Scene`.
- The scene is shown on the primary `Stage`.
- After login, `LoginController` chooses the dashboard based on role:
  - Admin -> `admin_dashboard.fxml`
  - Student -> `student_dashboard.fxml`
  - Teacher -> `teacher_dashboard.fxml`
  - Parent -> `parent_dashboard.fxml`
  - Counsellor -> `counsellor_dashboard.fxml`
- Dashboards do not use tab panes. They use sidebar/menu `Button`s.
- When a sidebar button is clicked, the dashboard controller calls `FXMLLoader.load(...)` and replaces dashboard content with the selected subview.
- If the subview needs logged-in user data, the controller calls methods like `initData(...)`, `initForStudent(...)`, or `updateContext(...)`.

Why this design:

- FXML keeps UI layout separate from Java logic.
- Controllers keep each screen's behavior isolated.
- Reusing `FXMLLoader` makes navigation simple in a desktop JavaFX app.

### 2.2 Login and Session Workflow

- User enters email/password in `login.fxml`.
- `LoginController.handleLogin(...)` reads the JavaFX fields.
- Password is hashed using `AuthService.hashPassword(...)`.
- `UserDAO.validateUser(email, passwordHash)` runs a SQL query against `USERS` and `ROLES`.
- If valid, a `User` model is returned.
- `AuthService.setCurrentUser(user)` stores the logged-in user in a static variable.
- `LoginAuditDAO.logSuccessfulLogin(...)` records login in `LOGIN_AUDIT`.
- Controller loads role-specific dashboard FXML.
- Logout calls `AuthService.clearCurrentUser()`, which logs logout time.

Why this design:

- The UI does not directly know SQL details.
- `AuthService` acts as an application-level session holder.
- `LOGIN_AUDIT` creates reviewable system history.

### 2.3 Emailing / OTP Password Reset

Classes involved:

- `ForgotPasswordController.java`
- `OTPGenerator.java`
- `EmailSender.java`
- `UserDAO.java`

Library/protocol involved:

- Protocol: SMTP.
- Host: `smtp.gmail.com`.
- Port: `587`.
- Security: STARTTLS.
- Java libraries: `jakarta.mail-api-2.1.2.jar`, `angus-mail-2.0.5.jar`, `jakarta.activation-api-2.1.2.jar`, `angus-activation-2.0.2.jar`.

Step-by-step:

- User enters email on forgot-password screen.
- `ForgotPasswordController.handleSendOTP(...)` checks whether email exists using `UserDAO.emailExists(...)`.
- `OTPGenerator.generateOTP()` creates a temporary OTP.
- `EmailSender.sendOTP(recipientEmail, otp)` prepares SMTP properties:
  - `mail.smtp.auth = true`
  - `mail.smtp.starttls.enable = true`
  - `mail.smtp.host = smtp.gmail.com`
  - `mail.smtp.port = 587`
  - timeout values are set to 10 seconds.
- `Session.getInstance(props, new Authenticator(){...})` creates an authenticated mail session.
- `MimeMessage` builds the email body.
- `Transport.send(message)` performs the SMTP connection, authentication handshake, STARTTLS negotiation, and message send.
- User enters OTP and new password.
- Controller validates OTP using `OTPGenerator.isOTPValid(...)`.
- New password is hashed and saved through `UserDAO.updatePassword(...)`.

What to say if asked about handshake:

> The SMTP handshake is handled by Jakarta Mail with Angus Mail as the implementation. Our code configures the server, port, authentication, and STARTTLS; `Transport.send()` performs the actual network-level SMTP conversation.

### 2.4 File Handling: PDF Upload, Open, Download

Important answer:

> Assignment PDFs, submission PDFs, and question-bank PDFs are not stored as Oracle BLOBs. They are stored as files in local folders. Oracle stores metadata such as IDs, title, subject, class, due date, and submission rows. Profile images are the part stored as BLOBs.

Assignment folders:

- Teacher assignment PDFs: `folders/assignments/assignment_<assignmentId>.pdf`
- Student submission PDFs: `folders/submissions/submission_<assignmentId>_<studentId>.pdf`

Question bank folder:

- Question paper PDFs: `folders/question_bank/question_<questionId>.pdf`

Classes involved:

- `AssignmentFileService.java`
- `QuestionBankFileService.java`
- `TeacherAssignmentsController.java`
- `StudentAssignmentsController.java`
- `TeacherQuestionBankController.java`
- `StudentQuestionBankController.java`
- `UserDAO.java`
- `QuestionBankDAO.java`

Assignment upload workflow:

- Teacher opens assignment screen.
- Teacher chooses PDF using JavaFX `FileChooser`.
- Controller requests a new assignment ID from `UserDAO.getNextAssignmentId()`.
- `AssignmentFileService.saveAssignmentPdf(...)` copies the selected file using `Files.copy(...)`.
- `UserDAO.createAssignmentForTeacher(...)` inserts metadata into `ASSIGNMENTS`.
- If DB insertion fails, controller deletes the copied PDF to avoid orphan files.

Student submission workflow:

- Student selects assignment from JavaFX `TableView`.
- Student chooses solution PDF using JavaFX `FileChooser`.
- Controller finds student ID using `UserDAO.getStudentIdByUserId(...)`.
- `AssignmentFileService.saveSubmissionPdf(...)` copies file into `folders/submissions`.
- `UserDAO.submitAssignment(...)` inserts/updates row in `SUBMISSIONS`.

Question-bank upload workflow:

- Teacher chooses class, subject, title, and PDF.
- Controller gets a question ID from `QuestionBankDAO.getNextQuestionId()`.
- `QuestionBankFileService.saveQuestionPdf(...)` copies the PDF to `folders/question_bank`.
- `QuestionBankDAO.createQuestionPaper(...)` inserts metadata into `QUESTION_BANK`.
- Student can open or download the PDF using the deterministic question ID filename.

PDF generation workflow:

- Performance reports are not uploaded files.
- They are generated at runtime using iText:
  - `PdfWriter`
  - `PdfDocument`
  - `Document`
  - `Paragraph`
  - `Table`
- Generated PDFs are saved to a location chosen through JavaFX `FileChooser`.

### 2.5 Real-Time Interaction: Chat Workflow

Important answer:

> The chat is not WebSocket-based and not true real-time push. It is database-backed messaging. Messages are inserted into OracleDB and loaded again from OracleDB when the screen opens, when a participant is selected, after sending, or when notification counts are refreshed.

Tables involved:

- `COMMUNICATION`
- `COMMUNICATION_READ_STATE`

Classes involved:

- `ParentTeacherConnectController.java`
- `TeacherConnectController.java`
- `ParentCounsellorConnectController.java`
- `StudentCounsellorConnectController.java`
- `CounsellorChatController.java`
- `UserDAO.java`
- Models: `CommunicationMessage`, `TeacherContactRecord`, `ParentChatThreadRecord`, `CounsellorContactRecord`, `CounsellorInboxRecord`

Message send workflow:

- User selects a receiver from a JavaFX `TableView`.
- User types into a JavaFX `TextArea`.
- Controller validates:
  - message must not be empty
  - message length must not exceed 1000 characters
  - selected teacher/parent/counsellor must exist
- Controller calls a role-specific DAO method:
  - `sendMessageToTeacher(...)`
  - `sendMessageToParent(...)`
  - `sendMessageToCounsellorFromParent(...)`
  - `sendMessageToCounsellorFromStudent(...)`
  - `sendMessageFromCounsellor(...)`
- DAO gets next message ID using `SequenceService`.
- DAO inserts into `COMMUNICATION`:
  - `MESSAGE_ID`
  - `SENDER_ID`
  - `RECEIVER_ID`
  - `STUDENT_ID`
  - `MESSAGE_TEXT`
  - `SENT_AT`
- Controller clears the input and reloads chat history.

Message load workflow:

- Controller calls a DAO method like:
  - `getChatHistoryForTeacher(...)`
  - `getChatHistory(...)`
  - `getCounsellorChatHistoryForParent(...)`
  - `getCounsellorChatHistoryForStudent(...)`
  - `getCounsellorChatHistoryForCounsellor(...)`
- DAO selects messages where sender/receiver pair matches both participants and student context.
- Messages are sorted by sent time/message ID.
- Controller creates JavaFX `Label` bubbles inside `chatMessagesBox`, which is a `VBox` inside a `ScrollPane`.
- Own messages and other person's messages use different background colors/styles.

Unread notification workflow:

- `COMMUNICATION_READ_STATE` stores last seen message ID per viewer/partner/student.
- When a conversation is opened, DAO updates read state using a SQL `MERGE`.
- Unread count is calculated by counting messages where `message_id > last_seen_message_id`.
- Dashboard badges are JavaFX `Label`s near chat buttons.

Why this design:

- It is simple and reliable for a local desktop mini project.
- Oracle becomes the source of truth.
- No server process, socket server, or WebSocket infrastructure is required.

### 2.6 OracleDB and JavaFX Data Management

Connection layer:

- `DBConnection.java` loads Oracle driver using `Class.forName("oracle.jdbc.driver.OracleDriver")`.
- It connects using JDBC URL `jdbc:oracle:thin:@localhost:1521/XEPDB1` unless environment variables override it.
- `DriverManager.getConnection(URL, USER, PASS)` opens a JDBC connection.
- DAO classes use `PreparedStatement` to avoid raw string concatenation for user values.
- Query results are read using `ResultSet`.
- JavaFX tables expect `ObservableList<T>`, so DAO methods usually return `ObservableList` of model objects.

Database table map:

| Table | Purpose | Frontend Usage |
|---|---|---|
| `ROLES` | Stores role names: Admin, Student, Teacher, Parent, Counsellor. | Login role routing and admin user creation. |
| `USERS` | Base account table with name, email, password hash, role, phone, active status. | Login, dashboards, admin user management, contact lists. |
| `CLASSES` | Class name, section, academic year. | Student/teacher mapping, assignments, exams, dashboards. |
| `SUBJECTS` | Subject names. | Teacher assignment, question bank, marks, exams. |
| `TEACHERS` | Teacher-specific profile: qualification and experience. | Teacher dashboard and admin/teacher mapping. |
| `STUDENTS` | Student-specific profile: DOB, gender, conduct, remarks. | Student dashboard, parent dashboard, teacher student management. |
| `CLASS_SUBJECT_TEACHER` | Join table mapping class + subject + teacher. | Controls teacher scope for attendance, assignments, exams, question bank. |
| `STUDENT_CLASS` | Join table mapping student to class. | Parent/teacher/student dashboards and class movement. |
| `PARENT_STUDENT` | Join table mapping parent to ward/student. | Parent dashboard ward selector and parent permissions. |
| `QUESTION_PAPERS` | Exam definitions: class, subject, date, max marks. | Student/teacher performance and upcoming exam notifications. |
| `QUESTION_BANK` | Uploaded question paper metadata. | Teacher upload list and student question-bank downloads. |
| `ASSIGNMENTS` | Assignment metadata. | Teacher assignment list and student pending assignments. |
| `SUBMISSIONS` | Student submission metadata and marks. | Student submission status and teacher completed assignment view. |
| `MARKS` | Student marks per question paper/exam. | Performance tables, charts, reports, academic alerts. |
| `ATTENDANCE` | FN/AN attendance per student/date/class. | Student attendance, teacher attendance, parent dashboard. |
| `COUNSELLING` | Counselling requests/cases. | Counselling request screen and counsellor request management. |
| `COMMUNICATION` | Chat messages between users in a student context. | Parent-teacher, parent-counsellor, student-counsellor, counsellor inbox. |
| `COMMUNICATION_READ_STATE` | Last seen message per conversation. | Unread badges and notification counts. |
| `LOGIN_AUDIT` | Login/logout/IP audit history. | Admin system logs and active sessions count. |
| `APP_SETTINGS` | School name/address/phone/email/theme. | Login title, dashboard brand, PDF headers, theme. |
| `USER_PROFILE_IMAGES` | User profile image BLOB. | Profile/avatar buttons on dashboards. |
| `SCHOOL_PROFILE_IMAGES` | School profile/logo image BLOB. | School avatar/profile button. |

Data flow pattern:

- JavaFX `TableView` is defined in FXML.
- Controller has `@FXML private TableView<ModelType> table;`.
- Controller configures columns with `PropertyValueFactory`.
- Controller calls DAO method.
- DAO runs SQL query through JDBC.
- DAO converts each `ResultSet` row into a model object.
- DAO returns `ObservableList<ModelType>`.
- Controller calls `table.setItems(list)`.

## 3. External JAR Mystery

| JAR / Library | Why It Exists | What It Does In This Project |
|---|---|---|
| `ojdbc8.jar` | Oracle JDBC driver | Allows Java code to connect to OracleDB using `jdbc:oracle:thin`. Without it, `DBConnection` cannot load `oracle.jdbc.driver.OracleDriver`. |
| `jakarta.mail-api-2.1.2.jar` | Mail API interfaces/classes | Provides `Session`, `Message`, `Transport`, `Authenticator`, and mail exceptions used by `EmailSender`. |
| `angus-mail-2.0.5.jar` | Jakarta Mail implementation | Performs the real SMTP work behind the API, including connection and send behavior. |
| `jakarta.activation-api-2.1.2.jar` | Activation API | Supports mail content/data handling required by Jakarta Mail. |
| `angus-activation-2.0.2.jar` | Activation implementation | Runtime implementation for activation support. |
| `kernel-9.6.0.jar` | iText core PDF engine | Low-level PDF writing/reading foundation used by `PdfWriter` and `PdfDocument`. |
| `layout-9.6.0.jar` | iText layout module | Provides high-level `Document`, `Paragraph`, `Table`, and layout APIs used in report PDFs. |
| `io-9.6.0.jar` | iText IO utilities | Supports streams, fonts, and lower-level resource handling for PDFs. |
| `commons-9.6.0.jar` | iText common utilities | Shared iText support classes used by other iText modules. |
| `forms-9.6.0.jar` | iText form support | Required if PDF form features are used or by bundled iText dependencies; not central to current screens. |
| `barcodes-9.6.0.jar` | iText barcode support | Available for barcode generation, but not a core feature currently. |
| `svg-9.6.0.jar` | iText SVG support | Supports SVG-to-PDF conversion if needed; not central to current workflow. |
| `styled-xml-parser-9.6.0.jar` | iText styled XML parsing | Used by iText modules for styled content parsing. |
| `font-asian-9.6.0.jar`, `hyph-9.6.0.jar` | iText font/hyphenation extras | Support better text rendering, fonts, and hyphenation in PDFs. |
| `pdfa-9.6.0.jar`, `pdfua-9.6.0.jar`, `sign-9.6.0.jar` | iText advanced PDF features | PDF/A, accessibility, and signing modules; included but not core to normal report generation. |
| `bouncy-castle-*.jar` | Cryptography support for iText | Supports encryption/signing/crypto features required by advanced iText modules. |
| `brotli-compressor-9.6.0.jar` | Compression support | Used by iText dependency stack for compressed content handling. |
| `slf4j-api-2.0.17.jar`, `slf4j-simple-2.0.17.jar` | Logging facade and simple logger | Supports libraries that log through SLF4J. |

Short review explanation:

> The JARs are not random. `ojdbc8` connects Java to Oracle. Jakarta/Angus Mail sends OTP email through SMTP. iText JARs generate downloadable PDF reports. Activation and SLF4J are supporting runtime dependencies required by those libraries.

## 4. Major Class Responsibilities

### Main

| Class | Position | Single Responsibility |
|---|---|---|
| `Main` | `src/main/Main.java` | Starts the JavaFX application and loads the login screen. It owns the primary stage setup, scene creation, and initial application title/icon behavior. |

### Controllers

| Class | Position | Single Responsibility |
|---|---|---|
| `LoginController` | `src/controller/LoginController.java` | Handles login form validation and role-based dashboard navigation. It connects the login UI with `UserDAO`, `AuthService`, and `FXMLLoader`. |
| `ForgotPasswordController` | `src/controller/ForgotPasswordController.java` | Controls the OTP password reset screen. It validates email, sends OTP, verifies OTP, and updates the password. |
| `LoadingScreen` | `src/controller/LoadingScreen.java` | Builds a loading/splash-style UI panel. It supports video/image-based loading visuals before or during app startup. |
| `AdminDashboardController` | `src/controller/AdminDashboardController.java` | Controls admin dashboard stats, sidebar, school branding, and subview navigation. It loads admin-only screens into the dashboard area. |
| `AdminUserManagementController` | `src/controller/AdminUserManagementController.java` | Handles admin creation/editing of users. It manages role-specific form fields and the users table. |
| `AdminAcademicMappingController` | `src/controller/AdminAcademicMappingController.java` | Handles academic structure management. It creates classes, assigns teachers, moves students, links parents, and deletes users. |
| `AdminLogsController` | `src/controller/AdminLogsController.java` | Displays and manages login audit records. It supports refresh, delete selected, and clear all operations. |
| `AdminSettingsController` | `src/controller/AdminSettingsController.java` | Manages school profile settings and theme selection. It persists settings through `AppSettingsDAO`. |
| `StudentDashboardController` | `src/controller/StudentDashboardController.java` | Controls student dashboard profile, metrics, chart, badges, and navigation. It gathers student data from DAO/service classes and displays it in JavaFX. |
| `AttendanceController` | `src/controller/AttendanceController.java` | Displays student attendance details. It fills attendance summary labels, chart, and table from database records. |
| `PerformanceController` | `src/controller/PerformanceController.java` | Displays student performance table/chart and exports a performance PDF. It uses iText for PDF creation. |
| `StudentAssignmentsController` | `src/controller/StudentAssignmentsController.java` | Manages the student's assignment view. It opens assignment PDFs, uploads submission PDFs, and records submissions. |
| `StudentQuestionBankController` | `src/controller/StudentQuestionBankController.java` | Displays question-bank papers available to a student. It opens and downloads question-paper PDFs. |
| `StudentCounsellorConnectController` | `src/controller/StudentCounsellorConnectController.java` | Handles student-to-counsellor chat. It loads counsellors, displays message history, sends messages, and updates read state. |
| `CounsellingRequestController` | `src/controller/CounsellingRequestController.java` | Handles submitting counselling requests from student or parent context. It validates category/date/concern and inserts counselling request rows. |
| `TeacherDashboardController` | `src/controller/TeacherDashboardController.java` | Controls teacher dashboard profile, counts, upcoming exams, chat badge, and navigation. It loads teacher subviews using `FXMLLoader`. |
| `TeacherAttendanceController` | `src/controller/TeacherAttendanceController.java` | Handles teacher attendance marking/editing for mapped students. It uses editable JavaFX table cells and saves changes to `ATTENDANCE`. |
| `TeacherStudentManagementController` | `src/controller/TeacherStudentManagementController.java` | Lets teachers view and update students in their academic scope. It manages conduct/class updates with DAO validation. |
| `TeacherStudentPerformanceController` | `src/controller/TeacherStudentPerformanceController.java` | Displays student performance to teachers and generates reports. It can download one selected PDF or all student PDFs. |
| `TeacherAssignmentsController` | `src/controller/TeacherAssignmentsController.java` | Handles teacher assignment publishing and completed assignment review. It combines PDF file storage with assignment/submission database metadata. |
| `TeacherQuestionBankController` | `src/controller/TeacherQuestionBankController.java` | Handles teacher upload and viewing of question-bank PDFs. It stores metadata in Oracle and files in a local folder. |
| `TeacherConnectController` | `src/controller/TeacherConnectController.java` | Handles teacher-side parent chat inbox. It shows parent threads, unread badges, chat history, sending, and clearing messages. |
| `ParentDashboardController` | `src/controller/ParentDashboardController.java` | Controls parent dashboard with ward selector, attendance, alerts, conduct, chat badges, and PDF report export. It changes dashboard data based on selected ward. |
| `ParentTeacherConnectController` | `src/controller/ParentTeacherConnectController.java` | Handles parent-to-teacher chat for a selected ward. It loads teacher contacts and exchanges messages through the database. |
| `ParentCounsellorConnectController` | `src/controller/ParentCounsellorConnectController.java` | Handles parent-to-counsellor chat. It uses ward context, counsellor contacts, message history, and unread state. |
| `ParentWardContextAware` | `src/controller/ParentWardContextAware.java` | Provides a small interface/contract for parent subviews that need parent and ward context. It lets parent dashboard pass context generically to child controllers. |
| `CounsellorDashboardController` | `src/controller/CounsellorDashboardController.java` | Controls counsellor dashboard profile, request counts, active cases, chat badge, theme, and navigation. |
| `CounsellorRequestsController` | `src/controller/CounsellorRequestsController.java` | Manages counselling requests/cases for counsellors. It accepts and completes requests through status updates. |
| `CounsellorChatController` | `src/controller/CounsellorChatController.java` | Handles counsellor chat inbox across parent/student participants. It loads participant threads, sends replies, updates unread state, and clears conversations. |
| `ProfileImageSupport` | `src/controller/ProfileImageSupport.java` | Centralizes profile/school image button behavior. It reads/writes images through `ProfileImageDAO` and updates JavaFX avatar buttons. |

### Services

| Class | Position | Single Responsibility |
|---|---|---|
| `AuthService` | `src/service/AuthService.java` | Stores current logged-in user and handles password hashing. It also records login/logout audit activity. |
| `EmailSender` | `src/service/EmailSender.java` | Sends OTP emails using SMTP through Jakarta Mail. It hides SMTP configuration and reports friendly error messages. |
| `OTPGenerator` | `src/service/OTPGenerator.java` | Creates and validates temporary OTP values. It supports forgot-password verification. |
| `AssignmentFileService` | `src/service/AssignmentFileService.java` | Manages assignment/submission PDF file paths and copying. It opens PDFs through the desktop default PDF viewer. |
| `QuestionBankFileService` | `src/service/QuestionBankFileService.java` | Manages question-bank PDF file paths, copying, opening, and downloading. It keeps PDF storage separate from database metadata. |
| `ExamService` | `src/service/ExamService.java` | Wraps exam-related retrieval and grade calculation. It delegates DB reads to `UserDAO` and adds display-level grading logic. |
| `SchoolSettingsService` | `src/service/SchoolSettingsService.java` | Caches school settings from the database. It provides school name/contact/theme values to controllers and PDF generators. |
| `ThemeService` | `src/service/ThemeService.java` | Applies and persists Light/Dark theme selection. It updates JavaFX root CSS style classes and theme button text. |

### DAO Classes

| Class | Position | Single Responsibility |
|---|---|---|
| `DBConnection` | `src/dao/DBConnection.java` | Owns Oracle JDBC connection configuration. It loads the Oracle driver and returns `Connection` objects. |
| `UserDAO` | `src/dao/UserDAO.java` | Main database access class for users, attendance, marks, assignments, chat, counselling, parent/teacher/student relationships, and admin actions. It contains most SQL business queries for the project. |
| `AdminStatsDAO` | `src/dao/AdminStatsDAO.java` | Provides dashboard counts for admin. It runs scalar count queries for students, teachers, counsellors, classes, sessions, and red flags. |
| `AppSettingsDAO` | `src/dao/AppSettingsDAO.java` | Reads and writes `APP_SETTINGS`. It also ensures settings infrastructure exists and validates admin save permission. |
| `LoginAuditDAO` | `src/dao/LoginAuditDAO.java` | Manages login audit records. It logs login/logout and provides admin audit table data. |
| `ProfileImageDAO` | `src/dao/ProfileImageDAO.java` | Manages user and school profile images as Oracle BLOBs. It creates required image tables if missing and reads/writes binary image bytes. |
| `QuestionBankDAO` | `src/dao/QuestionBankDAO.java` | Manages question-bank metadata. It creates records, lists teacher/student question papers, and ensures question-bank DB infrastructure. |
| `SequenceService` | `src/dao/SequenceService.java` | Centralizes Oracle sequence handling. It gets next IDs and creates sequences when needed. |

### Models

| Class | Position | Single Responsibility |
|---|---|---|
| `User` | `src/model/User.java` | Represents a logged-in account with ID, name, email, and role. Used by authentication and dashboards. |
| `Role` | `src/model/Role.java` | Represents a user role. Used for role-based UI routing and admin user management. |
| `Student` | `src/model/Student.java` | Represents student profile data shown in teacher/parent/student screens. Includes class and conduct fields. |
| `AttendanceRecord` | `src/model/AttendanceRecord.java` | Represents one day of FN/AN attendance for JavaFX tables. Used in student, parent, and teacher attendance screens. |
| `PerformanceRecord` | `src/model/PerformanceRecord.java` | Represents subject/exam/marks data. Used in performance tables, charts, and PDFs. |
| `ExamRecord` | `src/model/ExamRecord.java` | Represents exam/question-paper details and grade/display data. Used for upcoming exam and marks workflows. |
| `AssignmentRecord` | `src/model/AssignmentRecord.java` | Represents assignment metadata and submission status. Used by teacher/student assignment tables. |
| `QuestionBankRecord` | `src/model/QuestionBankRecord.java` | Represents uploaded question-paper metadata. Used by teacher and student question-bank tables. |
| `CommunicationMessage` | `src/model/CommunicationMessage.java` | Represents one chat message row. Used to render chat bubbles. |
| `TeacherContactRecord` | `src/model/TeacherContactRecord.java` | Represents a teacher contact visible to a parent. Includes subject/contact/unread display. |
| `ParentChatThreadRecord` | `src/model/ParentChatThreadRecord.java` | Represents one parent thread visible to a teacher. Includes parent, ward, contact, and unread count. |
| `CounsellorContactRecord` | `src/model/CounsellorContactRecord.java` | Represents a counsellor contact visible to student/parent. Includes email, phone, and unread count. |
| `CounsellorInboxRecord` | `src/model/CounsellorInboxRecord.java` | Represents a counsellor's chat participant thread. Includes participant type, student context, class, contact, and unread count. |
| `CounsellingRequestRecord` | `src/model/CounsellingRequestRecord.java` | Represents private counselling note/request data. Used for student/parent counselling history. |
| `CounsellingCaseRecord` | `src/model/CounsellingCaseRecord.java` | Represents a counselling case shown to counsellor. Includes session, student, class, date, category, status, and notes. |
| `AcademicAlertRecord` | `src/model/AcademicAlertRecord.java` | Represents low-mark academic alert data. Used in parent dashboard alert table. |
| `AdminUserRecord` | `src/model/AdminUserRecord.java` | Represents users shown in admin management table. Includes active display text. |
| `AdminAuditRecord` | `src/model/AdminAuditRecord.java` | Represents login audit table rows. Includes profile image bytes and login/logout/IP details. |
| `AdminTeacherMappingRecord` | `src/model/AdminTeacherMappingRecord.java` | Represents teacher-class-subject mappings for admin tables. |
| `AdminStudentClassRecord` | `src/model/AdminStudentClassRecord.java` | Represents student-class mappings for admin tables. |
| `AdminParentLinkRecord` | `src/model/AdminParentLinkRecord.java` | Represents parent-student relationship rows for admin tables. |

## 5. Coding Workflow: How The Project Works End To End

Use this as your explanation flow in review:

- The project starts from `Main.java`.
- `Main` loads `login.fxml`.
- `login.fxml` defines the JavaFX login UI.
- `LoginController` handles button clicks.
- When login is clicked, controller hashes password and calls `UserDAO`.
- `UserDAO` opens Oracle connection using `DBConnection`.
- SQL query checks `USERS` and `ROLES`.
- If successful, `AuthService` stores current user.
- Based on role, `LoginController` loads one dashboard FXML.
- Dashboard controller receives the `User` through `initData`.
- Dashboard displays data by calling DAO/service methods.
- Sidebar buttons call controller methods.
- Controller loads subview FXML using `FXMLLoader`.
- Subview controller configures JavaFX tables/fields/charts.
- Controller asks DAO for data.
- DAO executes SQL through JDBC.
- `ResultSet` rows become model objects.
- Model objects go into `ObservableList`.
- `TableView.setItems(...)` displays the data.
- When user performs an action, controller validates input first.
- Controller then calls DAO or service.
- DAO writes to Oracle using `PreparedStatement`.
- Service may copy files, send email, generate PDF, or update theme.
- Controller refreshes the JavaFX UI after success/failure.

Simple MVC-style explanation:

- View: FXML files and CSS.
- Controller: JavaFX controller classes.
- Model: Java model record classes.
- DAO: JDBC database access.
- Service: reusable non-UI logic.
- Database: Oracle tables.
- File system: PDF storage folders.

## 6. Review Questions You Can Answer Quickly

### Is this Java or JavaFX?

- The programming language is Java.
- The UI framework is JavaFX.
- FXML is JavaFX's XML layout format.
- Controllers are Java classes that control JavaFX UI.

### Are there tabs?

- There is no Swing `JTabbedPane`.
- There is also no JavaFX `TabPane` in the current FXML screens.
- Navigation is done using buttons in sidebars/top bars and loading FXML subviews.

### Is chat real-time?

- It is near-manual refresh/database-backed chat.
- No WebSockets.
- No socket server.
- No HTTP polling loop.
- Messages are inserted and selected from OracleDB.
- Notification badges are unread-count queries.

### Are PDFs stored in DB?

- Assignment/question-bank/submission PDFs: file system.
- Metadata: OracleDB.
- Profile/school images: Oracle BLOB.
- Report PDFs: generated on demand using iText and saved by user.

### Why Oracle?

- Oracle stores structured school data reliably.
- Foreign keys maintain relationships between users, roles, classes, subjects, students, parents, teachers, assignments, attendance, marks, counselling, and communication.

### Why external JARs?

- Java does not natively know how to connect to Oracle, send Gmail SMTP, or generate rich PDFs.
- JARs add those capabilities.

