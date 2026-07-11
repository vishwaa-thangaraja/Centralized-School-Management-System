# Centralized School Management System

CSMS is a JavaFX desktop application for centralized school operations. It uses Oracle Database, JDBC, FXML/CSS, JavaMail/Jakarta Mail for OTP email, and iText libraries for PDF/report workflows.

## Current Modules

- Authentication, role routing, and forgot-password OTP
- Student portal: dashboard, attendance, performance, assignments, question bank, counsellor connect
- Teacher portal: dashboard, attendance editing, student management, performance reports, assignments, question bank, parent/student connect
- Parent portal: dashboard, teacher connect, counsellor connect
- Counsellor portal: dashboard, requests, student chat/support
- Admin portal: dashboard, user management, academic mapping, settings, login audit
- Database setup with current sample data
- ER/UML/reference documentation in `docs/` and `uml/`

## Repository Targets

Your repo:

```text
https://github.com/VishwaaT27/Centralized-School-Management-System
```

Friend repo:

```text
https://github.com/rragulramachandran/Centralized_School_Management_System
```

Both repos contain the same finalized application source and database setup. Local machine differences are handled through `config.local.bat`.

## Local Configuration

`config.local.bat` is intentionally ignored by Git. Create it from one of the templates below.

For Vishwaa's system:

```bat
copy config.vishwaa.example.bat config.local.bat
notepad config.local.bat
```

Expected values:

```bat
set "CSMS_DB_USER=CSMS"
set "CSMS_DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1"
set "CSMS_JAVAFX_LIB=E:\JavaFX\javafx-sdk-17.0.18\lib"
```

For Ragul's system:

```bat
copy config.ragul.example.bat config.local.bat
notepad config.local.bat
```

Expected values:

```bat
set "CSMS_DB_USER=C##CSMS"
set "CSMS_DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1"
set "CSMS_JAVAFX_LIB=C:\javafx\javafx-sdk-17.0.19\lib"
```

Set `CSMS_DB_PASSWORD` to the local Oracle password. OTP email is optional; enable `CSMS_EMAIL` and `CSMS_EMAIL_PASSWORD` only if forgot-password email must work on that machine.

## Database Setup

Connect to Oracle using the local app user, then run the full setup SQL.

Example for Ragul:

```bat
sqlplus C##CSMS/your_oracle_password@localhost:1521/XEPDB1
```

Inside SQL*Plus:

```sql
@"C:\Users\ragul\OneDrive\Desktop\csms\database\csms_full_setup_with_data.sql"
```

For this local project folder:

```sql
@"D:\Documents\SEM IV\Mini-II\CSMS_Project\database\csms_full_setup_with_data.sql"
```

The setup script recreates the schema, inserts current sample data, creates required sequences/triggers, and prints row-count verification.

## Run

From the project folder:

```bat
run.bat
```

`run.bat` loads `config.local.bat`, copies FXML/CSS/assets into `out`, compiles the JavaFX app, and launches `main.Main`. Startup logs are written to:

```text
build_verify\run.log
```

## Important Files

- `database/csms_full_setup_with_data.sql`: full schema and sample data
- `run.bat`: compile and launch helper
- `config.example.bat`: generic local configuration template
- `config.vishwaa.example.bat`: Vishwaa machine template
- `config.ragul.example.bat`: Ragul machine template
- `docs/`: ER diagram, table documentation, audit/review docs
- `uml/`: project UML reference image

## Notes

Do not commit `config.local.bat`, `users.txt`, `out/`, or `build_verify/`. They are local/generated files.
