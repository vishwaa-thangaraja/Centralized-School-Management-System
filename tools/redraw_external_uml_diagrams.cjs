const fs = require("node:fs");
const path = require("node:path");
const sharp = require("sharp");

const outDir = "D:\\Documents\\SEM IV\\Mini-II\\uml\\redrawn_crisp_uml";
fs.mkdirSync(outDir, { recursive: true });

const C = {
  bg: "#ffffff",
  ink: "#111827",
  muted: "#374151",
  line: "#1f2937",
  grid: "#d1d5db",
  blue: "#1d4ed8",
  teal: "#0f766e",
  orange: "#c2410c",
  violet: "#7c3aed",
  red: "#be123c",
  green: "#15803d",
  gold: "#a16207",
  gray: "#4b5563",
  paleBlue: "#eff6ff",
  paleTeal: "#ecfdf5",
  paleOrange: "#fff7ed",
  paleViolet: "#f5f3ff",
  paleRed: "#fff1f2",
  paleGray: "#f8fafc"
};

function esc(s) {
  return String(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

function wrapText(str, max = 26) {
  const words = String(str).split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    if ((line + " " + word).trim().length > max) {
      if (line) lines.push(line);
      line = word;
    } else {
      line = (line + " " + word).trim();
    }
  }
  if (line) lines.push(line);
  return lines;
}

function text(x, y, lines, cls = "small", anchor = "middle", gap = 28) {
  if (!Array.isArray(lines)) lines = [lines];
  return lines.map((l, i) => `<text x="${x}" y="${y + i * gap}" text-anchor="${anchor}" class="${cls}">${esc(l)}</text>`).join("");
}

function base(w, h, title, subtitle, body) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">
<defs>
  <marker id="arrow" viewBox="0 0 12 12" refX="10" refY="6" markerWidth="12" markerHeight="12" orient="auto">
    <path d="M 0 0 L 12 6 L 0 12 z" fill="${C.line}"/>
  </marker>
  <marker id="arrowBlue" viewBox="0 0 12 12" refX="10" refY="6" markerWidth="12" markerHeight="12" orient="auto">
    <path d="M 0 0 L 12 6 L 0 12 z" fill="${C.blue}"/>
  </marker>
  <filter id="shadow" x="-10%" y="-10%" width="130%" height="130%">
    <feDropShadow dx="0" dy="8" stdDeviation="8" flood-color="#000000" flood-opacity="0.10"/>
  </filter>
  <style>
    .title{font:800 46px Arial, Helvetica, sans-serif; fill:${C.ink};}
    .subtitle{font:500 24px Arial, Helvetica, sans-serif; fill:${C.muted};}
    .head{font:800 27px Arial, Helvetica, sans-serif; fill:${C.ink};}
    .label{font:700 23px Arial, Helvetica, sans-serif; fill:${C.ink};}
    .small{font:600 20px Arial, Helvetica, sans-serif; fill:${C.ink};}
    .usecase{font:700 25px Arial, Helvetica, sans-serif; fill:${C.ink};}
    .tiny{font:600 17px Arial, Helvetica, sans-serif; fill:${C.muted};}
    .white{fill:#fff;}
    .line{stroke:${C.line}; stroke-width:3; fill:none; marker-end:url(#arrow);}
    .dash{stroke-dasharray:10 8;}
  </style>
</defs>
<rect width="${w}" height="${h}" fill="${C.bg}"/>
<text x="70" y="70" class="title">${esc(title)}</text>
<text x="70" y="112" class="subtitle">${esc(subtitle)}</text>
${body}
</svg>`;
}

function line(x1, y1, x2, y2, label = "", dashed = false) {
  const mx = (x1 + x2) / 2;
  const my = (y1 + y2) / 2;
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" class="line ${dashed ? "dash" : ""}"/>
  ${label ? `<rect x="${mx - label.length * 6 - 22}" y="${my - 31}" width="${label.length * 12 + 44}" height="34" rx="17" fill="#fff" stroke="${C.grid}"/><text x="${mx}" y="${my - 8}" text-anchor="middle" class="tiny">${esc(label)}</text>` : ""}`;
}

function rect(x, y, w, h, label, stroke = C.blue, fill = "#fff", cls = "small", r = 24) {
  const lines = wrapText(label, Math.max(16, Math.floor(w / 13)));
  const yy = y + h / 2 - (lines.length - 1) * 13 + 7;
  return `<g filter="url(#shadow)"><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}" stroke="${stroke}" stroke-width="4"/>${text(x + w / 2, yy, lines, cls)}</g>`;
}

function pill(x, y, w, h, label, stroke = C.blue, fill = "#fff") {
  return rect(x, y, w, h, label, stroke, fill, "small", h / 2);
}

function usePill(x, y, w, h, label, stroke = C.blue, fill = "#fff") {
  return rect(x, y, w, h, label, stroke, fill, "usecase", h / 2);
}

function classicBox(x, y, w, h, label) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" class="actBox"/>
  ${text(x + w / 2, y + h / 2 + 11, wrapText(label, Math.floor(w / 18)), "actText", "middle", 34)}`;
}

function classicDiamond(cx, cy, w, h, label) {
  const pts = `${cx},${cy - h / 2} ${cx + w / 2},${cy} ${cx},${cy + h / 2} ${cx - w / 2},${cy}`;
  return `<polygon points="${pts}" fill="#d1d5db" stroke="#111827" stroke-width="5"/>
  ${label ? text(cx, cy + 10, wrapText(label, 16), "actText") : ""}`;
}

function classicArrow(x1, y1, x2, y2) {
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>`;
}

function stateBox(x, y, w, h, label) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="18" class="stateBox"/>
  ${text(x + w / 2, y + h / 2 + 13, wrapText(label, Math.floor(w / 17)), "stateText", "middle", 38)}`;
}

function stateArrow(x1, y1, x2, y2) {
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>`;
}

function diamond(cx, cy, w, h, label, stroke = C.blue) {
  const pts = `${cx},${cy - h / 2} ${cx + w / 2},${cy} ${cx},${cy + h / 2} ${cx - w / 2},${cy}`;
  return `<g filter="url(#shadow)"><polygon points="${pts}" fill="#fff" stroke="${stroke}" stroke-width="4"/>${text(cx, cy + 7, wrapText(label, 18), "small")}</g>`;
}

function actor(x, y, label, color) {
  return `<g>
    <circle cx="${x}" cy="${y}" r="28" fill="#fff" stroke="${color}" stroke-width="5"/>
    <line x1="${x}" y1="${y + 28}" x2="${x}" y2="${y + 126}" stroke="${color}" stroke-width="5" stroke-linecap="round"/>
    <line x1="${x - 58}" y1="${y + 70}" x2="${x + 58}" y2="${y + 70}" stroke="${color}" stroke-width="5" stroke-linecap="round"/>
    <line x1="${x}" y1="${y + 126}" x2="${x - 48}" y2="${y + 195}" stroke="${color}" stroke-width="5" stroke-linecap="round"/>
    <line x1="${x}" y1="${y + 126}" x2="${x + 48}" y2="${y + 195}" stroke="${color}" stroke-width="5" stroke-linecap="round"/>
    ${text(x, y + 240, label, "head")}
  </g>`;
}

function useCase() {
  const body = `
  <rect x="470" y="180" width="2520" height="1590" rx="36" fill="${C.paleGray}" stroke="${C.grid}" stroke-width="4"/>
  ${text(1730, 230, "Centralized School Management System", "head")}
  ${actor(200, 320, "Admin", C.blue)}
  ${actor(200, 610, "Teacher", C.teal)}
  ${actor(200, 900, "Student", C.orange)}
  ${actor(3260, 610, "Parent", C.violet)}
  ${actor(3260, 1060, "Counsellor", C.red)}
  ${actor(3260, 1455, "All Users", C.gray)}

  ${usePill(620, 320, 500, 110, "Manage Users", C.blue)}
  ${usePill(1230, 320, 500, 110, "Manage Academic Setup", C.blue)}
  ${usePill(1840, 320, 500, 110, "Monitor System", C.blue)}
  ${usePill(2450, 320, 420, 110, "Manage Settings", C.blue)}

  ${usePill(620, 610, 500, 110, "Manage Students", C.teal)}
  ${usePill(1230, 610, 500, 110, "Manage Attendance", C.teal)}
  ${usePill(1840, 610, 500, 110, "Manage Exams and Marks", C.teal)}
  ${usePill(2450, 610, 420, 110, "Manage Resources", C.teal)}
  ${usePill(1535, 760, 540, 110, "Communicate with Parents", C.teal)}

  ${usePill(620, 900, 500, 110, "Track Academic Progress", C.orange)}
  ${usePill(1230, 900, 500, 110, "View Attendance", C.orange)}
  ${usePill(1840, 900, 500, 110, "Submit Assignments", C.orange)}
  ${usePill(2450, 900, 420, 110, "Access Support", C.orange)}

  ${usePill(620, 1190, 500, 110, "Monitor Ward Progress", C.violet)}
  ${usePill(1230, 1190, 500, 110, "Download Reports", C.violet)}
  ${usePill(1840, 1190, 500, 110, "Communicate with School", C.violet)}
  ${usePill(2450, 1190, 420, 110, "Request Support", C.violet)}

  ${usePill(790, 1440, 560, 110, "Manage Counselling Cases", C.red)}
  ${usePill(1510, 1440, 560, 110, "Provide Student Support", C.red)}
  ${usePill(2230, 1440, 560, 110, "Monitor Counselling Activity", C.red)}

  ${usePill(820, 1650, 430, 86, "Secure Login", C.gray)}
  ${usePill(1310, 1650, 430, 86, "Reset Password", C.gray)}
  ${usePill(1800, 1650, 430, 86, "Use Theme Engine", C.gray)}
  ${usePill(2290, 1650, 430, 86, "Maintain Audit", C.gray)}

  ${line(280, 320, 620, 375, "")}
  ${line(280, 610, 620, 665, "")}
  ${line(280, 900, 620, 955, "")}
  ${line(3180, 610, 2870, 1245, "")}
  ${line(3180, 1060, 2790, 1495, "")}
  ${line(3180, 1455, 2720, 1693, "")}
  `;
  return base(3560, 1880, "CSMS Use Case Diagram", "Generic role-based functional scope with clear actor-to-use-case mapping.", body);
}

function activity() {
  const body = `
  <style>
    .actBox{fill:#fff;stroke:#111827;stroke-width:5;}
    .actText{font:700 31px Georgia, 'Times New Roman', serif;fill:#111827;}
    .actLabel{font:600 24px Georgia, 'Times New Roman', serif;fill:#111827;}
  </style>
  <circle cx="1750" cy="175" r="28" fill="#000"/>
  ${classicBox(1470, 250, 560, 92, "Open CSMS application")}
  ${classicBox(1470, 425, 560, 92, "Enter login credentials")}
  ${classicBox(1450, 600, 600, 92, "Validate user account")}
  ${classicDiamond(1750, 800, 230, 140, "")}
  ${classicBox(2220, 755, 470, 92, "Show login error")}
  ${classicBox(1470, 965, 560, 92, "Load role dashboard")}
  ${classicDiamond(1750, 1190, 250, 150, "")}
  <text x="1875" y="770" class="actLabel">invalid</text>
  <text x="1850" y="910" class="actLabel">valid</text>
  <text x="1885" y="1175" class="actLabel">select role</text>

  ${classicBox(290, 1420, 460, 100, "Admin operations")}
  ${classicBox(890, 1420, 500, 100, "Teacher operations")}
  ${classicBox(1530, 1420, 500, 100, "Student operations")}
  ${classicBox(2170, 1420, 500, 100, "Parent operations")}
  ${classicBox(2790, 1420, 500, 100, "Counsellor operations")}
  <line x1="360" y1="1715" x2="3140" y2="1715" stroke="#000" stroke-width="8"/>
  ${classicBox(1350, 1855, 800, 100, "Save changes or retrieve information")}
  ${classicBox(1450, 2050, 600, 100, "Update database records")}
  ${classicBox(1410, 2245, 680, 100, "Refresh screen and notifications")}
  <circle cx="1750" cy="2490" r="38" fill="#fff" stroke="#000" stroke-width="6"/>
  <circle cx="1750" cy="2490" r="22" fill="#000"/>

  ${classicArrow(1750,203,1750,250)}
  ${classicArrow(1750,342,1750,425)}
  ${classicArrow(1750,517,1750,600)}
  ${classicArrow(1750,692,1750,730)}
  ${classicArrow(1865,800,2220,800)}
  <polyline points="2455,755 2455,465 2030,465" fill="none" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>
  ${classicArrow(1750,870,1750,965)}
  ${classicArrow(1750,1057,1750,1115)}

  <polyline points="1750,1265 1750,1325 520,1325 520,1420" fill="none" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>
  <polyline points="1750,1265 1750,1325 1140,1325 1140,1420" fill="none" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>
  ${classicArrow(1750,1265,1780,1420)}
  <polyline points="1750,1265 1750,1325 2420,1325 2420,1420" fill="none" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>
  <polyline points="1750,1265 1750,1325 3040,1325 3040,1420" fill="none" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>
  <text x="530" y="1300" class="actLabel">Admin</text>
  <text x="1135" y="1300" class="actLabel">Teacher</text>
  <text x="1740" y="1380" class="actLabel">Student</text>
  <text x="2410" y="1300" class="actLabel">Parent</text>
  <text x="3020" y="1300" class="actLabel">Counsellor</text>

  ${classicArrow(520,1520,520,1715)}
  ${classicArrow(1140,1520,1140,1715)}
  ${classicArrow(1780,1520,1780,1715)}
  ${classicArrow(2420,1520,2420,1715)}
  ${classicArrow(3040,1520,3040,1715)}
  ${classicArrow(1750,1715,1750,1855)}
  ${classicArrow(1750,1955,1750,2050)}
  ${classicArrow(1750,2150,1750,2245)}
  ${classicArrow(1750,2345,1750,2452)}
  `;
  return base(3500, 2620, "CSMS Activity Diagram", "Classic activity flow for login, role selection, module work, data update, and final refresh.", body);
}

function sequence() {
  const xs = [180, 620, 1060, 1500, 1940, 2380, 2820];
  const names = ["User", "Login Screen", "Auth Manager", "User Directory", "Dashboard", "Module Screen", "Database"];
  let body = names.map((n, i) => rect(xs[i]-155, 175, 310, 70, n, [C.gray,C.blue,C.teal,C.violet,C.blue,C.orange,C.green][i], "#fff", "small", 14)).join("");
  body += xs.map(x => `<line x1="${x}" y1="245" x2="${x}" y2="1530" stroke="${C.grid}" stroke-width="4" stroke-dasharray="12 12"/>`).join("");
  const msg = (a,b,y,l,d=false)=>line(xs[a],y,xs[b],y,l,d);
  body += `
  ${msg(0,1,340,"1. submit credentials")}
  ${msg(1,2,455,"2. verify password")}
  ${msg(2,3,570,"3. validate role and status")}
  ${msg(3,6,685,"4. read user record")}
  ${msg(6,3,800,"5. return match",true)}
  ${msg(3,2,915,"6. authenticated user",true)}
  ${msg(2,4,1030,"7. open role dashboard")}
  ${msg(4,6,1145,"8. load counts, alerts, messages")}
  ${msg(4,5,1260,"9. open selected module")}
  ${msg(5,6,1375,"10. save / fetch module data")}
  ${msg(6,5,1490,"11. refresh results",true)}
  `;
  return base(3050, 1640, "CSMS Sequence Diagram", "Clear interaction sequence for login, dashboard routing, module access, and persistence.", body);
}

function stateMachine() {
  const body = `
  <style>
    .stateBox{fill:#fff;stroke:#111827;stroke-width:4;}
    .stateText{font:700 38px Arial, Helvetica, sans-serif;fill:#111827;}
    .transText{font:800 25px Arial, Helvetica, sans-serif;fill:#15803d;}
  </style>
  <circle cx="1560" cy="185" r="30" fill="#000"/>
  <text x="1630" y="175" class="transText">Application</text>
  <text x="1630" y="207" class="transText">Started</text>

  ${stateBox(1250, 295, 620, 110, "Unauthenticated")}
  ${stateBox(1250, 545, 620, 110, "Authenticating")}
  ${stateBox(185, 770, 520, 110, "Login Failed")}
  ${stateBox(1185, 825, 750, 110, "Authenticated Session")}
  ${stateBox(250, 1120, 560, 110, "Admin Dashboard")}
  ${stateBox(940, 1120, 560, 110, "Teacher Dashboard")}
  ${stateBox(1630, 1120, 560, 110, "Student Dashboard")}
  ${stateBox(2320, 1120, 560, 110, "Parent Dashboard")}
  ${stateBox(1280, 1435, 650, 110, "Counsellor Dashboard")}
  ${stateBox(1185, 1720, 750, 110, "Module Active")}
  ${stateBox(1185, 1985, 750, 110, "Data Saved / View Refreshed")}
  ${stateBox(1250, 2260, 620, 110, "Logged Out")}
  <circle cx="1560" cy="2500" r="40" fill="#fff" stroke="#000" stroke-width="7"/>
  <circle cx="1560" cy="2500" r="23" fill="#000"/>

  ${stateArrow(1560,215,1560,295)}
  ${stateArrow(1560,405,1560,545)}
  <text x="1605" y="485" class="transText">Credentials Submitted</text>
  ${stateArrow(1250,600,705,825)}
  <text x="875" y="705" class="transText">[invalid] Checked</text>
  <polyline points="445,770 445,350 1250,350" fill="none" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>
  <text x="520" y="505" class="transText">Retry Login</text>
  ${stateArrow(1560,655,1560,825)}
  <text x="1605" y="745" class="transText">[valid] Role Loaded</text>

  ${stateArrow(1300,935,530,1120)}
  <text x="680" y="1030" class="transText">Admin</text>
  ${stateArrow(1430,935,1220,1120)}
  <text x="1210" y="1045" class="transText">Teacher</text>
  ${stateArrow(1690,935,1910,1120)}
  <text x="1890" y="1045" class="transText">Student</text>
  ${stateArrow(1845,935,2600,1120)}
  <text x="2350" y="1030" class="transText">Parent</text>
  ${stateArrow(1560,935,1605,1435)}
  <text x="1630" y="1330" class="transText">Counsellor</text>

  ${stateArrow(530,1230,1300,1720)}
  ${stateArrow(1220,1230,1420,1720)}
  ${stateArrow(1910,1230,1720,1720)}
  ${stateArrow(2600,1230,1925,1720)}
  ${stateArrow(1605,1545,1605,1720)}
  <text x="1995" y="1615" class="transText">Open Module</text>
  ${stateArrow(1560,1830,1560,1985)}
  <text x="1605" y="1915" class="transText">Save / Fetch Data</text>
  <polyline points="1935,2040 2810,2040 2810,1765 1935,1765" fill="none" stroke="#111827" stroke-width="4" marker-end="url(#arrow)"/>
  <text x="2320" y="1990" class="transText">Continue Work</text>
  ${stateArrow(1560,2095,1560,2260)}
  <text x="1605" y="2190" class="transText">Logout</text>
  ${stateArrow(1560,2370,1560,2460)}
  `;
  return base(3200, 2620, "CSMS State Machine Diagram", "Classic state transitions for login, role sessions, module activity, refresh, and logout.", body);
}

function component() {
  const comp = (x, y, w, h, label, stroke = "#22c7bd") => {
    const lines = wrapText(label, Math.floor(w / 17));
    return `<g>
      <rect x="${x}" y="${y}" width="${w}" height="${h}" fill="#ffffff" stroke="${stroke}" stroke-width="5"/>
      <rect x="${x + w - 72}" y="${y + 22}" width="44" height="44" fill="none" stroke="${stroke}" stroke-width="4"/>
      <rect x="${x + w - 88}" y="${y + 34}" width="34" height="14" fill="#ffffff" stroke="${stroke}" stroke-width="4"/>
      <rect x="${x + w - 88}" y="${y + 58}" width="34" height="14" fill="#ffffff" stroke="${stroke}" stroke-width="4"/>
      ${text(x + w / 2, y + h / 2 - (lines.length - 1) * 18 + 12, lines, "darkComp", "middle", 40)}
    </g>`;
  };
  const arrow = (points, dashed = true, color = "#ff9900") =>
    `<polyline points="${points}" fill="none" stroke="${color}" stroke-width="5" ${dashed ? 'stroke-dasharray="14 10"' : ""} marker-end="url(#orangeArrow)"/>`;
  const body = `
  <defs>
    <marker id="orangeArrow" viewBox="0 0 12 12" refX="10" refY="6" markerWidth="12" markerHeight="12" orient="auto">
      <path d="M 0 0 L 12 6 L 0 12 z" fill="#ff9900"/>
    </marker>
    <marker id="cyanArrow" viewBox="0 0 12 12" refX="10" refY="6" markerWidth="12" markerHeight="12" orient="auto">
      <path d="M 0 0 L 12 6 L 0 12 z" fill="#22c7bd"/>
    </marker>
    <style>
      .darkTitle{font:900 58px Georgia, 'Times New Roman', serif;fill:#4b4b4b;letter-spacing:1px;}
      .darkComp{font:700 32px Georgia, 'Times New Roman', serif;fill:#1f2937;}
      .darkLabel{font:800 26px Arial, Helvetica, sans-serif;fill:#ffb020;}
    </style>
  </defs>
  <rect width="3400" height="2050" fill="#000000"/>
  <rect x="810" y="34" width="1780" height="92" fill="#ffffff"/>
  <text x="1700" y="98" text-anchor="middle" class="darkTitle">CENTRALIZED SCHOOL MANAGEMENT SYSTEM</text>

  ${comp(70, 930, 500, 150, "CSMS Users", "#22c7bd")}
  ${comp(760, 930, 500, 150, "Authentication Component", "#22c7bd")}
  ${comp(1500, 250, 520, 170, "Admin Module", "#22c7bd")}
  ${comp(1500, 620, 520, 170, "Teacher Module", "#22c7bd")}
  ${comp(1500, 930, 520, 170, "Student Module", "#22c7bd")}
  ${comp(1500, 1280, 520, 170, "Parent Module", "#22c7bd")}
  ${comp(1500, 1600, 520, 170, "Counsellor Module", "#22c7bd")}

  ${comp(2450, 360, 520, 160, "User and Academic Data", "#6b7ee8")}
  ${comp(2450, 680, 520, 160, "Attendance and Marks Data", "#6b7ee8")}
  ${comp(2450, 1000, 520, 160, "Assignments and Resources", "#6b7ee8")}
  ${comp(2450, 1320, 520, 160, "Communication Service", "#6b7ee8")}
  ${comp(2450, 1640, 520, 160, "Reports and Notification Service", "#6b7ee8")}

  ${arrow("570,1005 760,1005")}
  ${arrow("1260,1005 1375,1005 1375,335 1500,335")}
  ${arrow("1260,1005 1375,1005 1375,705 1500,705")}
  ${arrow("1260,1005 1500,1005")}
  ${arrow("1260,1005 1375,1005 1375,1365 1500,1365")}
  ${arrow("1260,1005 1375,1005 1375,1685 1500,1685")}

  ${arrow("2020,335 2450,440", false)}
  ${arrow("2020,705 2450,760", false)}
  ${arrow("2020,1005 2450,1080", false)}
  ${arrow("2020,1365 2450,1400", false)}
  ${arrow("2020,1685 2450,1720", false)}

  ${arrow("2020,705 2240,705 2240,440 2450,440")}
  ${arrow("2020,1005 2240,1005 2240,760 2450,760")}
  ${arrow("2020,1365 2240,1365 2240,1720 2450,1720")}
  ${arrow("2020,1685 2240,1685 2240,1400 2450,1400")}

  <text x="970" y="900" class="darkLabel">login / role validation</text>
  <text x="1275" y="785" class="darkLabel">route to role module</text>
  <text x="2180" y="290" class="darkLabel">uses</text>
  <text x="2180" y="620" class="darkLabel">updates</text>
  <text x="2180" y="945" class="darkLabel">accesses</text>
  <text x="2180" y="1285" class="darkLabel">messages</text>
  `;
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="3400" height="2050" viewBox="0 0 3400 2050">
${body}
</svg>`;
}

function deployment() {
  const node = (x,y,w,h,t,items,c)=>`<g filter="url(#shadow)"><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="28" fill="#fff" stroke="${c}" stroke-width="5"/><text x="${x+35}" y="${y+55}" class="head">${esc(t)}</text>${items.map((it,i)=>`<text x="${x+45}" y="${y+105+i*36}" class="small">• ${esc(it)}</text>`).join("")}</g>`;
  const body = `
  ${node(150,220,720,540,"Client Device",["Desktop application","Login and role dashboards","Local file upload/download","User interaction"],C.blue)}
  ${node(1130,220,720,540,"Application Runtime",["Controllers and services","Authentication and session control","Report and file handling","Theme and notifications"],C.teal)}
  ${node(2110,220,720,540,"Database Server",["Central data storage","User and role directory","Academic and communication records","Audit and settings"],C.green)}
  ${node(1130,920,720,330,"External Services",["Email service for OTP","PDF/resource file processing","Network access controlled by configuration"],C.orange)}
  ${line(870,490,1130,490,"application actions")}${line(1850,490,2110,490,"secure data access")}${line(1490,760,1490,920,"OTP / reports")}
  `;
  return base(3000, 1380, "CSMS Deployment Diagram", "Generic deployment view with client, application runtime, database, and external support services.", body);
}

function communication() {
  const obj=(x,y,t,items,c)=>rect(x,y,410,150,`${t}\n${items.join("\\n")}`,c,"#fff","small",18);
  const body = `
  ${obj(120,220,"1 User",["selects role action"],C.gray)}
  ${obj(700,220,"2 Login / Dashboard",["routes workflow"],C.blue)}
  ${obj(1280,220,"3 Role Module",["executes use case"],C.teal)}
  ${obj(1860,220,"4 Service Manager",["validates and processes"],C.orange)}
  ${obj(2440,220,"5 Data Store",["persists records"],C.green)}
  ${obj(700,620,"6 File / Report Handler",["uploads, downloads, PDFs"],C.violet)}
  ${obj(1280,620,"7 Notification Manager",["badges, alerts, OTP"],C.gold)}
  ${obj(1860,620,"8 Message Center",["teacher, parent, counsellor chat"],C.red)}
  ${line(530,295,700,295,"1. request")}
  ${line(1110,295,1280,295,"2. open module")}
  ${line(1690,295,1860,295,"3. process")}
  ${line(2270,295,2440,295,"4. save/read")}
  ${line(1485,370,905,620,"5. files/reports")}
  ${line(1485,370,1485,620,"6. update badges")}
  ${line(2065,370,2065,620,"7. send message")}
  ${line(2440,390,2065,620,"8. conversation data")}
  ${line(1485,770,905,770,"9. refresh UI",true)}
  `;
  return base(2920, 980, "CSMS Communication Diagram", "Numbered collaboration among UI, modules, services, storage, files, notifications, and messaging.", body);
}

function packageDiagram() {
  const pkg=(x,y,w,h,t,items,c)=>`<g filter="url(#shadow)"><path d="M ${x} ${y+42} L ${x} ${y+h} L ${x+w} ${y+h} L ${x+w} ${y+42} L ${x+220} ${y+42} L ${x+190} ${y} L ${x} ${y} Z" fill="#fff" stroke="${c}" stroke-width="5"/><text x="${x+28}" y="${y+31}" class="head">${esc(t)}</text>${items.map((it,i)=>`<text x="${x+36}" y="${y+95+i*34}" class="small">• ${esc(it)}</text>`).join("")}</g>`;
  const body = `
  ${pkg(120,180,520,320,"main",["application bootstrap","startup and shutdown"],C.gray)}
  ${pkg(800,140,580,360,"view",["login views","dashboards","module screens"],C.blue)}
  ${pkg(1540,140,620,360,"controller",["role controllers","event handlers","navigation"],C.teal)}
  ${pkg(120,650,580,340,"model",["users and roles","academic records","messages and cases"],C.violet)}
  ${pkg(860,650,580,340,"service",["authentication","reports and files","theme and OTP"],C.orange)}
  ${pkg(1600,650,620,340,"dao",["data queries","audit and settings","profile images"],C.red)}
  ${pkg(2360,650,500,340,"database",["schema tables","constraints","seed data"],C.green)}
  ${pkg(860,1120,580,250,"resources",["styles","PDF folders","generated reports"],C.gold)}
  ${line(640,340,800,315,"loads")}${line(1380,315,1540,315,"controlled by")}
  ${line(1850,500,1150,650,"uses services")}${line(1910,500,1910,650,"uses data access")}
  ${line(1600,820,1440,820,"maps models")}${line(2220,820,2360,820,"persists")}
  ${line(1150,990,1150,1120,"reads/writes")}
  `;
  return base(3000, 1500, "CSMS Package Diagram", "Source package organization and dependency direction in the CSMS application.", body);
}

function erDiagram() {
  const groups = [
    ["Identity", 80, 190, 620, 740, C.blue, ["ROLES", "USERS", "TEACHERS", "STUDENTS", "LOGIN_AUDIT", "USER_PROFILE_IMAGES"]],
    ["Academics", 790, 190, 620, 740, C.teal, ["CLASSES", "SUBJECTS", "CLASS_SUBJECT_TEACHER", "STUDENT_CLASS", "PARENT_STUDENT"]],
    ["Assessment", 1500, 190, 660, 740, C.orange, ["QUESTION_PAPERS", "QUESTION_BANK", "ASSIGNMENTS", "MARKS", "SUBMISSIONS", "ATTENDANCE"]],
    ["Support & Settings", 2250, 190, 660, 740, C.violet, ["COUNSELLING", "COMMUNICATION", "COMMUNICATION_READ_STATE", "APP_SETTINGS", "SCHOOL_PROFILE_IMAGES"]]
  ];
  let body = "";
  for (const [title,x,y,w,h,c,items] of groups) {
    body += `<g filter="url(#shadow)"><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="28" fill="#fff" stroke="${c}" stroke-width="5"/><text x="${x+30}" y="${y+55}" class="head">${esc(title)}</text>`;
    items.forEach((it,i)=>{ body += rect(x+50, y+100+i*96, w-100, 66, it, c, "#fff", "small", 12); });
    body += `</g>`;
  }
  body += `${line(700,360,790,360,"role/user")}${line(1410,455,1500,455,"class/subject")}${line(2160,550,2250,550,"student/user")}${line(2160,745,2250,745,"messages")}`;
  return base(3000, 1040, "CSMS ER Diagram", "Readable grouped entity overview for identity, academics, assessment, communication, and settings.", body);
}

async function write(name, svg) {
  const svgPath = path.join(outDir, `${name}.svg`);
  const pngPath = path.join(outDir, `${name}.png`);
  fs.writeFileSync(svgPath, svg, "utf8");
  const rasterSvg = svg.replaceAll(' filter="url(#shadow)"', "");
  await sharp(Buffer.from(rasterSvg), { density: 96 }).png({ compressionLevel: 6, adaptiveFiltering: true }).toFile(pngPath);
  const meta = await sharp(pngPath).metadata();
  console.log(`${name}.png ${meta.width}x${meta.height}`);
}

(async () => {
  const diagrams = {
    "01_use_case": useCase(),
    "02_sequence": sequence(),
    "03_activity": activity(),
    "04_state_machine": stateMachine(),
    "05_component": component(),
    "06_deployment": deployment(),
    "07_communication": communication(),
    "08_package": packageDiagram(),
    "09_er": erDiagram(),
  };
  for (const [name, svg] of Object.entries(diagrams)) await write(name, svg);
  console.log(`Generated redrawn diagrams in ${outDir}`);
})();
