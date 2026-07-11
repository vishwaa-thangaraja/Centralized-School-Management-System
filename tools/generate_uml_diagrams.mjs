import fs from "node:fs";
import path from "node:path";

const outDir = path.resolve("docs", "uml");
fs.mkdirSync(outDir, { recursive: true });

const theme = {
  bg1: "#f8fafc",
  bg2: "#eef2f7",
  ink: "#172033",
  muted: "#536176",
  soft: "#d7deea",
  panel: "#ffffff",
  blue: "#2563eb",
  teal: "#0f766e",
  amber: "#b45309",
  violet: "#7c3aed",
  red: "#be123c",
  green: "#15803d",
  slate: "#334155"
};

const files = [
  "01_use_case_diagram.svg",
  "02_sequence_diagram.svg",
  "03_activity_diagram.svg",
  "04_state_machine_diagram.svg",
  "05_component_diagram.svg",
  "06_deployment_diagram.svg",
  "07_communication_diagram.svg",
  "08_package_diagram.svg"
];

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function base(width, height, title, subtitle, body) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="${theme.bg1}"/>
      <stop offset="100%" stop-color="${theme.bg2}"/>
    </linearGradient>
    <filter id="shadow" x="-20%" y="-20%" width="150%" height="150%">
      <feDropShadow dx="0" dy="12" stdDeviation="14" flood-color="#18274b" flood-opacity="0.14"/>
    </filter>
    <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="${theme.slate}"/>
    </marker>
    <marker id="arrowBlue" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="${theme.blue}"/>
    </marker>
    <style>
      .title { font: 800 34px Inter, Segoe UI, Arial, sans-serif; fill: ${theme.ink}; }
      .subtitle { font: 500 15px Inter, Segoe UI, Arial, sans-serif; fill: ${theme.muted}; }
      .label { font: 700 16px Inter, Segoe UI, Arial, sans-serif; fill: ${theme.ink}; }
      .small { font: 500 13px Inter, Segoe UI, Arial, sans-serif; fill: ${theme.muted}; }
      .tiny { font: 600 12px Inter, Segoe UI, Arial, sans-serif; fill: ${theme.muted}; }
      .white { fill: #fff; }
      .line { stroke: ${theme.slate}; stroke-width: 2.4; fill: none; marker-end: url(#arrow); }
      .line-soft { stroke: #8792a6; stroke-width: 2; fill: none; marker-end: url(#arrow); }
      .dash { stroke-dasharray: 8 7; }
    </style>
  </defs>
  <rect width="${width}" height="${height}" fill="url(#bg)"/>
  <text x="42" y="52" class="title">${esc(title)}</text>
  <text x="42" y="78" class="subtitle">${esc(subtitle)}</text>
  ${body}
</svg>
`;
}

function text(x, y, lines, cls = "small", anchor = "middle", gap = 18) {
  return lines.map((line, i) => `<text x="${x}" y="${y + i * gap}" class="${cls}" text-anchor="${anchor}">${esc(line)}</text>`).join("\n");
}

function actor(x, y, label, color = theme.slate) {
  return `
  <g>
    <circle cx="${x}" cy="${y}" r="20" fill="white" stroke="${color}" stroke-width="3"/>
    <line x1="${x}" y1="${y + 20}" x2="${x}" y2="${y + 86}" stroke="${color}" stroke-width="3" stroke-linecap="round"/>
    <line x1="${x - 38}" y1="${y + 44}" x2="${x + 38}" y2="${y + 44}" stroke="${color}" stroke-width="3" stroke-linecap="round"/>
    <line x1="${x}" y1="${y + 86}" x2="${x - 34}" y2="${y + 132}" stroke="${color}" stroke-width="3" stroke-linecap="round"/>
    <line x1="${x}" y1="${y + 86}" x2="${x + 34}" y2="${y + 132}" stroke="${color}" stroke-width="3" stroke-linecap="round"/>
    ${text(x, y + 160, [label], "label")}
  </g>`;
}

function ellipse(x, y, w, h, lines, color = theme.blue) {
  return `
  <g filter="url(#shadow)">
    <ellipse cx="${x}" cy="${y}" rx="${w / 2}" ry="${h / 2}" fill="white" stroke="${color}" stroke-width="2.4"/>
    ${text(x, y - (lines.length - 1) * 8 + 5, lines, "small")}
  </g>`;
}

function box(x, y, w, h, title, lines = [], color = theme.blue, cls = "") {
  const rowText = lines.map((line, i) => `<text x="${x + 18}" y="${y + 56 + i * 21}" class="small">${esc(line)}</text>`).join("\n");
  return `
  <g filter="url(#shadow)" class="${cls}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="white" stroke="${theme.soft}" stroke-width="1.4"/>
    <rect x="${x}" y="${y}" width="${w}" height="36" rx="10" fill="${color}"/>
    <rect x="${x}" y="${y + 18}" width="${w}" height="18" fill="${color}"/>
    <text x="${x + 18}" y="${y + 24}" class="label white">${esc(title)}</text>
    ${rowText}
  </g>`;
}

function line(x1, y1, x2, y2, label = "", color = theme.slate, dashed = false) {
  const midX = (x1 + x2) / 2;
  const midY = (y1 + y2) / 2;
  return `
  <line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${color}" stroke-width="2.3" ${dashed ? 'stroke-dasharray="8 7"' : ""} marker-end="url(#arrow)"/>
  ${label ? `<rect x="${midX - label.length * 3.8 - 12}" y="${midY - 22}" width="${label.length * 7.6 + 24}" height="24" rx="12" fill="white" opacity="0.96"/><text x="${midX}" y="${midY - 5}" class="tiny" text-anchor="middle">${esc(label)}</text>` : ""}`;
}

function useCaseDiagram() {
  const body = `
  <rect x="320" y="116" width="1460" height="1190" rx="24" fill="white" opacity="0.64" stroke="${theme.soft}" stroke-width="2"/>
  <text x="1050" y="154" class="label" text-anchor="middle">Centralized School Management System</text>
  ${actor(110, 238, "Admin", theme.blue)}
  ${actor(110, 560, "Teacher", theme.teal)}
  ${actor(110, 900, "Student", theme.amber)}
  ${actor(1985, 420, "Parent", theme.violet)}
  ${actor(1985, 830, "Counsellor", theme.red)}
  ${ellipse(545, 250, 250, 76, ["Authenticate", "and recover password"], theme.slate)}
  ${ellipse(855, 250, 270, 76, ["Maintain users", "roles and profiles"], theme.blue)}
  ${ellipse(1190, 250, 280, 76, ["Manage classes,", "subjects and mappings"], theme.blue)}
  ${ellipse(1485, 250, 250, 76, ["View audit logs", "and settings"], theme.blue)}
  ${ellipse(610, 520, 270, 76, ["Manage students", "and conduct"], theme.teal)}
  ${ellipse(930, 520, 270, 76, ["Mark attendance", "FN / AN sessions"], theme.teal)}
  ${ellipse(1250, 520, 270, 76, ["Create exams", "and enter marks"], theme.teal)}
  ${ellipse(1570, 520, 270, 76, ["Publish assignments", "and question papers"], theme.teal)}
  ${ellipse(600, 810, 270, 76, ["View dashboard", "performance and alerts"], theme.amber)}
  ${ellipse(935, 810, 270, 76, ["Submit assignments", "and view feedback"], theme.amber)}
  ${ellipse(1265, 810, 280, 76, ["Download reports", "and question papers"], theme.amber)}
  ${ellipse(1590, 810, 260, 76, ["Request counselling"], theme.red)}
  ${ellipse(720, 1080, 290, 76, ["Monitor ward attendance,", "marks and alerts"], theme.violet)}
  ${ellipse(1090, 1080, 280, 76, ["Chat with teacher", "or counsellor"], theme.violet)}
  ${ellipse(1460, 1080, 300, 76, ["Review cases", "and update status"], theme.red)}
  ${ellipse(1110, 1215, 300, 76, ["Send / receive", "contextual messages"], theme.violet)}
  ${line(178, 320, 435, 250, "", theme.blue)}
  ${line(178, 320, 745, 250, "", theme.blue)}
  ${line(178, 320, 1050, 250, "", theme.blue)}
  ${line(178, 320, 1360, 250, "", theme.blue)}
  ${line(178, 642, 480, 520, "", theme.teal)}
  ${line(178, 642, 795, 520, "", theme.teal)}
  ${line(178, 642, 1120, 520, "", theme.teal)}
  ${line(178, 642, 1440, 520, "", theme.teal)}
  ${line(178, 982, 468, 810, "", theme.amber)}
  ${line(178, 982, 810, 810, "", theme.amber)}
  ${line(178, 982, 1130, 810, "", theme.amber)}
  ${line(178, 982, 1460, 810, "", theme.amber)}
  ${line(1918, 502, 865, 1080, "", theme.violet)}
  ${line(1918, 502, 1230, 1080, "", theme.violet)}
  ${line(1918, 912, 1600, 1080, "", theme.red)}
  ${line(1918, 912, 1240, 1215, "", theme.red)}
  ${line(1918, 502, 1240, 1215, "", theme.violet)}
  ${line(1110, 1177, 1110, 845, "<<include>>", theme.slate, true)}
  ${line(1590, 850, 1460, 1042, "<<include>>", theme.red, true)}
  `;
  return base(2100, 1360, "CSMS Use Case Diagram", "Actors and major system goals derived from JavaFX controllers, UserDAO operations, and Oracle entities.", body);
}

function sequenceDiagram() {
  const xs = [120, 400, 700, 1000, 1300, 1600, 1880];
  const names = ["User", "LoginController", "AuthService", "UserDAO", "Oracle DB", "Dashboard Controller", "Feature View"];
  let body = names.map((n, i) => box(xs[i] - 95, 120, 190, 52, n, [], [theme.slate, theme.blue, theme.teal, theme.violet, theme.amber, theme.blue, theme.green][i])).join("\n");
  body += xs.map((x) => `<line x1="${x}" y1="172" x2="${x}" y2="1190" stroke="#9aa5b5" stroke-width="1.7" stroke-dasharray="7 8"/>`).join("\n");
  const msg = (i, j, y, label, dashed = false) => `${line(xs[i], y, xs[j], y, label, dashed ? theme.muted : theme.slate, dashed)}`;
  body += `
  ${msg(0, 1, 235, "enter email + password")}
  ${msg(1, 2, 300, "hashPassword(password)")}
  ${msg(2, 1, 355, "SHA-256 hash", true)}
  ${msg(1, 3, 420, "validateUser(email, hash)")}
  ${msg(3, 4, 485, "SELECT user + role + active flag")}
  ${msg(4, 3, 550, "User record / no match", true)}
  ${msg(3, 1, 615, "validated User", true)}
  ${msg(1, 2, 680, "setCurrentUser(user)")}
  ${msg(2, 4, 745, "insert LOGIN_AUDIT")}
  ${msg(1, 5, 820, "route by Role")}
  ${msg(5, 3, 885, "load dashboard metrics")}
  ${msg(3, 4, 950, "queries: attendance, marks, alerts, inbox")}
  ${msg(5, 6, 1015, "FXMLLoader.load(feature.fxml)")}
  ${msg(6, 3, 1080, "feature CRUD / chat / files")}
  ${msg(3, 4, 1145, "commit domain changes")}
  <rect x="74" y="204" width="1875" height="990" rx="18" fill="none" stroke="${theme.soft}" stroke-width="2"/>
  <text x="96" y="1225" class="small">Primary flow covers all roles: Admin, Teacher, Student, Parent, and Counsellor share authentication, then dashboards load role-specific FXML controllers and DAO operations.</text>`;
  return base(2000, 1280, "CSMS Sequence Diagram", "Authentication, role routing, dashboard loading, and feature persistence sequence.", body);
}

function activityNode(x, y, w, h, label, color = theme.blue) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${h / 2}" fill="white" stroke="${color}" stroke-width="2.3" filter="url(#shadow)"/>${text(x + w / 2, y + h / 2 + 5, [label], "small")}`;
}

function decision(x, y, w, h, label, color = theme.slate) {
  const points = `${x + w / 2},${y} ${x + w},${y + h / 2} ${x + w / 2},${y + h} ${x},${y + h / 2}`;
  return `<polygon points="${points}" fill="white" stroke="${color}" stroke-width="2.3" filter="url(#shadow)"/>${text(x + w / 2, y + h / 2 + 4, [label], "tiny")}`;
}

function activityDiagram() {
  const body = `
  <circle cx="1010" cy="130" r="17" fill="${theme.ink}"/>
  ${activityNode(860, 185, 300, 58, "Launch JavaFX app", theme.slate)}
  ${activityNode(860, 285, 300, 58, "Enter credentials", theme.slate)}
  ${decision(885, 385, 250, 105, "Valid active user?", theme.slate)}
  ${activityNode(520, 515, 280, 58, "Show login error", theme.red)}
  ${decision(885, 535, 250, 105, "Role?", theme.blue)}
  ${activityNode(140, 710, 260, 58, "Admin dashboard", theme.blue)}
  ${activityNode(470, 710, 260, 58, "Teacher dashboard", theme.teal)}
  ${activityNode(800, 710, 260, 58, "Student dashboard", theme.amber)}
  ${activityNode(1130, 710, 260, 58, "Parent dashboard", theme.violet)}
  ${activityNode(1460, 710, 260, 58, "Counsellor dashboard", theme.red)}
  ${activityNode(115, 850, 310, 58, "Users, mappings, logs, settings", theme.blue)}
  ${activityNode(435, 850, 330, 58, "Attendance, exams, assignments", theme.teal)}
  ${activityNode(775, 850, 310, 58, "View marks, submit work, request help", theme.amber)}
  ${activityNode(1110, 850, 310, 58, "Monitor ward and communicate", theme.violet)}
  ${activityNode(1450, 850, 310, 58, "Manage cases and counselling chat", theme.red)}
  ${activityNode(850, 1005, 320, 58, "DAO validates scope and updates Oracle", theme.slate)}
  ${activityNode(850, 1110, 320, 58, "Refresh dashboard / tables / badges", theme.green)}
  <circle cx="1010" cy="1245" r="18" fill="white" stroke="${theme.ink}" stroke-width="4"/><circle cx="1010" cy="1245" r="10" fill="${theme.ink}"/>
  ${line(1010, 147, 1010, 185)}
  ${line(1010, 243, 1010, 285)}
  ${line(1010, 343, 1010, 385)}
  ${line(885, 438, 660, 515, "No", theme.red)}
  ${line(660, 573, 860, 314, "Retry", theme.red)}
  ${line(1010, 490, 1010, 535, "Yes", theme.green)}
  ${line(885, 587, 270, 710, "Admin", theme.blue)}
  ${line(960, 640, 600, 710, "Teacher", theme.teal)}
  ${line(1010, 640, 930, 710, "Student", theme.amber)}
  ${line(1060, 640, 1260, 710, "Parent", theme.violet)}
  ${line(1135, 587, 1590, 710, "Counsellor", theme.red)}
  ${line(270, 768, 270, 850)}
  ${line(600, 768, 600, 850)}
  ${line(930, 768, 930, 850)}
  ${line(1260, 768, 1260, 850)}
  ${line(1590, 768, 1590, 850)}
  ${line(270, 908, 850, 1034)}
  ${line(600, 908, 895, 1005)}
  ${line(930, 908, 990, 1005)}
  ${line(1260, 908, 1090, 1005)}
  ${line(1590, 908, 1170, 1034)}
  ${line(1010, 1063, 1010, 1110)}
  ${line(1010, 1168, 1010, 1227)}
  `;
  return base(1900, 1320, "CSMS Activity Diagram", "End-to-end activity flow from login to role-specific operations and persistence.", body);
}

function stateDiagram() {
  const state = (x, y, w, h, title, lines, color) => box(x, y, w, h, title, lines, color);
  const body = `
  <circle cx="980" cy="125" r="17" fill="${theme.ink}"/>
  ${state(830, 185, 300, 90, "Unauthenticated", ["Login screen visible", "No current user"], theme.slate)}
  ${state(830, 340, 300, 90, "Authenticating", ["Password hashed", "UserDAO validates"], theme.blue)}
  ${state(80, 570, 300, 110, "Admin Session", ["User management", "Academic mapping", "Logs and settings"], theme.blue)}
  ${state(430, 570, 300, 110, "Teacher Session", ["Class scope loaded", "Marks, attendance, files"], theme.teal)}
  ${state(780, 570, 300, 110, "Student Session", ["Performance view", "Assignments and requests"], theme.amber)}
  ${state(1130, 570, 300, 110, "Parent Session", ["Selected ward context", "Monitoring and chat"], theme.violet)}
  ${state(1480, 570, 300, 110, "Counsellor Session", ["Inbox and cases", "Status updates"], theme.red)}
  ${state(560, 840, 320, 100, "Feature Active", ["FXML child view loaded", "DAO queries / updates"], theme.green)}
  ${state(1020, 840, 320, 100, "Persisted", ["Oracle committed", "Tables and badges refreshed"], theme.slate)}
  ${state(830, 1055, 300, 90, "Logged Out", ["Audit logout written", "Session cleared"], theme.slate)}
  <circle cx="980" cy="1240" r="18" fill="white" stroke="${theme.ink}" stroke-width="4"/><circle cx="980" cy="1240" r="10" fill="${theme.ink}"/>
  ${line(980, 142, 980, 185)}
  ${line(980, 275, 980, 340, "submit credentials")}
  ${line(980, 430, 230, 570, "Admin")}
  ${line(980, 430, 580, 570, "Teacher")}
  ${line(980, 430, 930, 570, "Student")}
  ${line(980, 430, 1280, 570, "Parent")}
  ${line(980, 430, 1630, 570, "Counsellor")}
  ${line(830, 385, 600, 260, "Invalid / inactive", theme.red)}
  ${line(230, 680, 690, 840, "open module")}
  ${line(580, 680, 720, 840, "open module")}
  ${line(930, 680, 780, 840, "open module")}
  ${line(1280, 680, 1110, 840, "open module")}
  ${line(1630, 680, 1230, 840, "open module")}
  ${line(880, 890, 1020, 890, "save / send / upload")}
  ${line(1180, 940, 980, 1055, "logout")}
  ${line(980, 1145, 980, 1222)}
  `;
  return base(1860, 1300, "CSMS State Machine Diagram", "Session and feature states for the JavaFX application lifecycle.", body);
}

function componentDiagram() {
  const body = `
  ${box(70, 150, 300, 160, "JavaFX Shell", ["main.Main", "Stage + Scene", "FXML navigation"], theme.slate)}
  ${box(470, 110, 360, 240, "FXML Views", ["login.fxml", "role dashboards", "feature screens", "CSS themes"], theme.blue)}
  ${box(930, 110, 390, 240, "Controllers", ["LoginController", "Admin / Teacher / Student", "Parent / Counsellor", "Profile image support"], theme.teal)}
  ${box(1420, 110, 350, 240, "Services", ["AuthService", "EmailSender + OTP", "ExamService", "Theme / settings", "file services"], theme.amber)}
  ${box(490, 510, 350, 210, "Model Records", ["User, Student, Role", "Attendance / Performance", "Assignment / QuestionBank", "Communication / Counselling"], theme.violet)}
  ${box(980, 510, 350, 210, "DAO Layer", ["UserDAO", "AdminStatsDAO", "LoginAuditDAO", "ProfileImageDAO", "DBConnection"], theme.red)}
  ${box(1460, 510, 300, 210, "Oracle Schema", ["22 tables", "FK constraints", "seed roles and data", "audit + settings"], theme.green)}
  ${box(260, 840, 330, 170, "Local File Store", ["folders/assignments", "folders/submissions", "question bank PDFs"], theme.slate)}
  ${box(720, 840, 330, 170, "External Libraries", ["JavaFX", "ojdbc8", "Jakarta Mail", "iText PDF"], theme.blue)}
  ${box(1180, 840, 330, 170, "Email Provider", ["OTP password reset", "SMTP transport"], theme.amber)}
  ${line(370, 230, 470, 230, "loads")}
  ${line(830, 230, 930, 230, "fx:controller")}
  ${line(1320, 230, 1420, 230, "uses")}
  ${line(1125, 350, 1155, 510, "binds records")}
  ${line(1110, 350, 1155, 510, "calls DAO")}
  ${line(1330, 615, 1460, 615, "JDBC")}
  ${line(1420, 250, 1330, 540, "Auth / exams")}
  ${line(1420, 280, 1310, 510, "settings")}
  ${line(350, 840, 990, 720, "PDF upload / submit")}
  ${line(885, 840, 1420, 350, "runtime APIs")}
  ${line(1510, 840, 1505, 350, "SMTP")}
  `;
  return base(1840, 1080, "CSMS Component Diagram", "Static architecture of JavaFX UI, controllers, services, DAOs, Oracle DB, and external resources.", body);
}

function deploymentDiagram() {
  const node = (x, y, w, h, title, lines, color) => `
  <g filter="url(#shadow)">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="18" fill="white" stroke="${color}" stroke-width="2.5"/>
    <rect x="${x + 16}" y="${y + 16}" width="${w - 32}" height="${h - 32}" rx="12" fill="#f8fafc" stroke="${theme.soft}" stroke-width="1.3"/>
    <text x="${x + 34}" y="${y + 48}" class="label">${esc(title)}</text>
    ${lines.map((l, i) => `<text x="${x + 34}" y="${y + 85 + i * 24}" class="small">${esc(l)}</text>`).join("")}
  </g>`;
  const body = `
  ${node(80, 170, 520, 610, "Client Workstation", ["Windows / desktop environment", "JDK + JavaFX runtime", "CSMS compiled classes", "FXML + CSS resources", "local assignment/submission files"], theme.blue)}
  ${node(760, 170, 500, 610, "Oracle Database Host", ["Oracle XE / listener", "CSMS schema", "tables, constraints, seed data", "JDBC endpoint from env vars"], theme.green)}
  ${node(1420, 170, 430, 270, "SMTP Mail Service", ["Jakarta Mail transport", "OTP delivery for reset flow"], theme.amber)}
  ${node(1420, 510, 430, 270, "Git / Project Workspace", ["src, database, lib", "docs UML artifacts", "run.bat configuration"], theme.slate)}
  ${box(175, 360, 330, 160, "JavaFX Application", ["main.Main", "Controllers", "Services", "DAO layer"], theme.teal)}
  ${box(840, 360, 340, 160, "CSMS Oracle Schema", ["USERS, STUDENTS, TEACHERS", "ATTENDANCE, MARKS", "COMMUNICATION, COUNSELLING"], theme.green)}
  ${line(505, 440, 840, 440, "JDBC / ojdbc8")}
  ${line(505, 500, 1420, 305, "SMTP for OTP")}
  ${line(340, 520, 340, 660, "read/write PDFs")}
  ${line(1420, 645, 600, 700, "project files")}
  <text x="92" y="835" class="small">Deployment is desktop-style: the JavaFX client contains presentation and business interaction logic, while persistence is centralized in Oracle through JDBC.</text>
  `;
  return base(1920, 900, "CSMS Deployment Diagram", "Runtime nodes, deployed artifacts, and infrastructure connections.", body);
}

function communicationDiagram() {
  const obj = (x, y, title, lines, color) => box(x, y, 300, 120, title, lines, color);
  const body = `
  ${obj(80, 150, "1 User", ["Admin / Teacher / Student", "Parent / Counsellor"], theme.slate)}
  ${obj(450, 150, "2 LoginController", ["collects credentials", "loads dashboard"], theme.blue)}
  ${obj(820, 150, "3 AuthService", ["hashes password", "stores current user"], theme.teal)}
  ${obj(1190, 150, "4 UserDAO", ["validates user", "executes feature methods"], theme.red)}
  ${obj(1560, 150, "5 Oracle DB", ["users, roles, audit", "domain tables"], theme.green)}
  ${obj(450, 430, "6 Dashboard", ["role metrics", "FXML navigation"], theme.blue)}
  ${obj(820, 430, "7 Feature Controller", ["attendance, marks, chat", "assignments, counselling"], theme.violet)}
  ${obj(1190, 430, "8 Services", ["file storage", "email, theme, reports"], theme.amber)}
  ${obj(1560, 430, "9 Files / SMTP", ["PDF folders", "OTP email"], theme.slate)}
  ${line(380, 210, 450, 210, "1: submit credentials")}
  ${line(750, 210, 820, 210, "2: hash / set session")}
  ${line(1120, 210, 1190, 210, "3: validateUser")}
  ${line(1490, 210, 1560, 210, "4: query user + role")}
  ${line(1190, 250, 750, 430, "5: route by role")}
  ${line(750, 490, 820, 490, "6: open feature")}
  ${line(1120, 490, 1190, 490, "7: DAO operation")}
  ${line(1490, 490, 1560, 265, "8: SELECT/INSERT/UPDATE")}
  ${line(1120, 535, 1190, 535, "9: upload / email / theme")}
  ${line(1490, 535, 1560, 535, "10: file or SMTP")}
  ${line(1560, 585, 970, 550, "11: result / refresh", theme.muted, true)}
  ${line(820, 550, 600, 550, "12: update UI", theme.muted, true)}
  `;
  return base(1900, 760, "CSMS Communication Diagram", "Object collaboration and numbered messages for login, feature execution, persistence, and refresh.", body);
}

function packageDiagram() {
  const pkg = (x, y, w, h, title, lines, color) => `
  <g filter="url(#shadow)">
    <path d="M ${x} ${y + 28} L ${x} ${y + h} L ${x + w} ${y + h} L ${x + w} ${y + 28} L ${x + 160} ${y + 28} L ${x + 140} ${y} L ${x} ${y} Z" fill="white" stroke="${color}" stroke-width="2.2"/>
    <text x="${x + 18}" y="${y + 21}" class="label">${esc(title)}</text>
    ${lines.map((l, i) => `<text x="${x + 22}" y="${y + 66 + i * 23}" class="small">${esc(l)}</text>`).join("")}
  </g>`;
  const body = `
  ${pkg(80, 135, 330, 230, "main", ["Main.java", "Application bootstrap", "Loads login.fxml"], theme.slate)}
  ${pkg(520, 110, 400, 280, "view", ["FXML screens", "login + dashboards", "feature views per role"], theme.blue)}
  ${pkg(1030, 110, 430, 280, "controller", ["29 JavaFX controllers", "role dashboards", "feature event handlers"], theme.teal)}
  ${pkg(80, 500, 400, 260, "model", ["User, Student, Role", "record-style view models", "attendance, marks, chat, cases"], theme.violet)}
  ${pkg(590, 500, 390, 260, "service", ["AuthService", "EmailSender, OTPGenerator", "file and theme services"], theme.amber)}
  ${pkg(1090, 500, 390, 260, "dao", ["UserDAO", "DBConnection", "LoginAuditDAO", "settings and image DAOs"], theme.red)}
  ${pkg(1560, 500, 300, 260, "database", ["Oracle SQL setup", "22 tables", "constraints and seed data"], theme.green)}
  ${pkg(520, 850, 400, 190, "css", ["dashboard.css", "login.css", "theme classes"], theme.blue)}
  ${pkg(1030, 850, 430, 190, "folders", ["assignments PDFs", "submissions PDFs", "question bank files"], theme.slate)}
  ${line(245, 365, 720, 390, "loads")}
  ${line(920, 250, 1030, 250, "fx:controller")}
  ${line(1245, 390, 1245, 500, "uses")}
  ${line(1030, 250, 820, 500, "binds models")}
  ${line(1245, 390, 785, 500, "calls services")}
  ${line(1480, 630, 1560, 630, "JDBC schema")}
  ${line(785, 760, 785, 850, "applies style")}
  ${line(785, 630, 1030, 930, "file services")}
  `;
  return base(1940, 1120, "CSMS Package Diagram", "Source package dependencies and documentation/data packages in the workspace.", body);
}

const diagrams = [
  useCaseDiagram(),
  sequenceDiagram(),
  activityDiagram(),
  stateDiagram(),
  componentDiagram(),
  deploymentDiagram(),
  communicationDiagram(),
  packageDiagram()
];

for (const [index, svg] of diagrams.entries()) {
  fs.writeFileSync(path.join(outDir, files[index]), svg, "utf8");
}

console.log(`Generated ${diagrams.length} UML diagram images in ${outDir}`);
for (const file of files) {
  console.log(`- ${path.join(outDir, file)}`);
}
