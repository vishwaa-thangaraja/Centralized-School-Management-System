import fs from "node:fs";
import path from "node:path";

const canvas = { width: 2560, height: 1600 };
const margin = 32;
const headerHeight = 34;
const rowHeight = 22;
const colors = {
  bg: "#f6f8fb",
  panel: "#ffffff",
  line: "#5b6677",
  lineSoft: "#8c97a8",
  text: "#1f2937",
  muted: "#556274",
  border: "#c8d1de",
  shadow: "rgba(24, 39, 75, 0.14)",
  identity: "#2563eb",
  academic: "#0f766e",
  assessment: "#b45309",
  support: "#7c3aed"
};

const groupPanels = [
  { id: "identity", title: "Identity, Roles & Settings", x: 24, y: 88, w: 620, h: 1488, color: colors.identity },
  { id: "academic", title: "Academic Structure & Mapping", x: 668, y: 88, w: 760, h: 520, color: colors.academic },
  { id: "assessment", title: "Exams, Assignments & Tracking", x: 668, y: 632, w: 1058, h: 944, color: colors.assessment },
  { id: "support", title: "Counselling & Communication", x: 1750, y: 88, w: 786, h: 1488, color: colors.support }
];

const tables = {
  ROLES: {
    group: "identity",
    x: 48,
    y: 132,
    width: 248,
    columns: ["PK role_id", "role_name UQ"]
  },
  APP_SETTINGS: {
    group: "identity",
    x: 322,
    y: 132,
    width: 292,
    columns: ["PK setting_key", "setting_value"]
  },
  USERS: {
    group: "identity",
    x: 148,
    y: 286,
    width: 360,
    columns: [
      "PK user_id",
      "name",
      "email UQ",
      "password_hash",
      "FK role_id",
      "phone",
      "created_at",
      "is_active"
    ]
  },
  TEACHERS: {
    group: "identity",
    x: 48,
    y: 564,
    width: 274,
    columns: ["PK teacher_id", "FK user_id UQ", "qualification", "experience"]
  },
  STUDENTS: {
    group: "identity",
    x: 344,
    y: 564,
    width: 274,
    columns: ["PK student_id", "FK user_id UQ", "dob", "gender", "conduct", "conduct_remarks"]
  },
  USER_PROFILE_IMAGES: {
    group: "identity",
    x: 48,
    y: 762,
    width: 274,
    columns: ["PK/FK user_id", "image_data", "mime_type", "updated_at"]
  },
  LOGIN_AUDIT: {
    group: "identity",
    x: 344,
    y: 762,
    width: 274,
    columns: ["PK log_id", "FK user_id", "login_time", "logout_time", "ip_address"]
  },
  SCHOOL_PROFILE_IMAGES: {
    group: "identity",
    x: 182,
    y: 980,
    width: 300,
    columns: ["PK image_id", "image_data", "mime_type", "updated_at"]
  },
  CLASSES: {
    group: "academic",
    x: 712,
    y: 138,
    width: 278,
    columns: ["PK class_id", "class_name", "section", "academic_year"]
  },
  SUBJECTS: {
    group: "academic",
    x: 1030,
    y: 138,
    width: 278,
    columns: ["PK subject_id", "subject_name UQ"]
  },
  CLASS_SUBJECT_TEACHER: {
    group: "academic",
    x: 834,
    y: 324,
    width: 352,
    columns: ["PK/FK class_id", "PK/FK subject_id", "PK/FK teacher_id"]
  },
  PARENT_STUDENT: {
    group: "academic",
    x: 712,
    y: 468,
    width: 278,
    columns: ["PK/FK parent_id", "PK/FK student_id", "relation"]
  },
  STUDENT_CLASS: {
    group: "academic",
    x: 1030,
    y: 468,
    width: 278,
    columns: ["PK/FK student_id", "PK/FK class_id"]
  },
  QUESTION_PAPERS: {
    group: "assessment",
    x: 700,
    y: 686,
    width: 334,
    columns: [
      "PK qp_id",
      "FK class_id",
      "FK subject_id",
      "exam_type",
      "exam_date",
      "max_marks",
      "exam_description",
      "FK created_by_teacher_id"
    ]
  },
  QUESTION_BANK: {
    group: "assessment",
    x: 1074,
    y: 686,
    width: 356,
    columns: [
      "PK question_id",
      "FK class_id",
      "FK subject_id",
      "FK teacher_id",
      "title",
      "academic_year",
      "original_file_name",
      "uploaded_at"
    ]
  },
  ASSIGNMENTS: {
    group: "assessment",
    x: 1470,
    y: 686,
    width: 308,
    columns: ["PK assignment_id", "FK class_id", "FK subject_id", "title", "description", "due_date"]
  },
  MARKS: {
    group: "assessment",
    x: 776,
    y: 1058,
    width: 278,
    columns: ["PK mark_id", "FK student_id", "FK qp_id", "marks_obtained"]
  },
  SUBMISSIONS: {
    group: "assessment",
    x: 1094,
    y: 1058,
    width: 286,
    columns: ["PK submission_id", "FK assignment_id", "FK student_id", "submitted_on", "marks"]
  },
  ATTENDANCE: {
    group: "assessment",
    x: 1420,
    y: 1038,
    width: 342,
    columns: [
      "PK attendance_id",
      "FK user_id",
      "FK class_id",
      "attendance_date",
      "session_type",
      "status",
      "leave_reason",
      "approval_status",
      "FK approved_by"
    ]
  },
  COUNSELLING: {
    group: "support",
    x: 1888,
    y: 138,
    width: 378,
    columns: ["PK session_id", "FK student_id", "FK counsellor_id", "session_date", "notes", "status", "category"]
  },
  COMMUNICATION: {
    group: "support",
    x: 1848,
    y: 430,
    width: 460,
    columns: ["PK message_id", "FK sender_id", "FK receiver_id", "FK student_id", "message_text", "sent_at"]
  },
  COMMUNICATION_READ_STATE: {
    group: "support",
    x: 1810,
    y: 696,
    width: 516,
    columns: ["PK/FK viewer_id", "PK/FK partner_id", "PK/FK student_id", "last_seen_message_id", "last_seen_at"]
  }
};

const tableNames = Object.keys(tables);
for (const name of tableNames) {
  tables[name].name = name;
  tables[name].height = headerHeight + tables[name].columns.length * rowHeight;
}

const relations = [
  { from: "ROLES", to: "USERS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "role_id" },
  { from: "USERS", to: "TEACHERS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "0..1", label: "user_id" },
  { from: "USERS", to: "STUDENTS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "0..1", label: "user_id" },
  { from: "USERS", to: "USER_PROFILE_IMAGES", fromSide: "left", toSide: "top", fromCard: "1", toCard: "0..1", label: "user_id", via: [{ x: 116, y: 520 }, { x: 116, y: 746 }] },
  { from: "USERS", to: "LOGIN_AUDIT", fromSide: "right", toSide: "top", fromCard: "1", toCard: "N", label: "user_id", via: [{ x: 564, y: 520 }, { x: 564, y: 746 }] },
  { from: "TEACHERS", to: "CLASS_SUBJECT_TEACHER", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "teacher_id", via: [{ x: 620, y: 626 }, { x: 620, y: 375 }] },
  { from: "STUDENTS", to: "STUDENT_CLASS", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "student_id", via: [{ x: 852, y: 628 }, { x: 852, y: 519 }] },
  { from: "USERS", to: "PARENT_STUDENT", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "parent_id", via: [{ x: 610, y: 392 }, { x: 610, y: 523 }] },
  { from: "STUDENTS", to: "PARENT_STUDENT", fromSide: "right", toSide: "bottom", fromCard: "1", toCard: "N", label: "student_id", via: [{ x: 646, y: 630 }, { x: 850, y: 630 }] },
  { from: "CLASSES", to: "CLASS_SUBJECT_TEACHER", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "class_id" },
  { from: "SUBJECTS", to: "CLASS_SUBJECT_TEACHER", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "subject_id" },
  { from: "CLASSES", to: "STUDENT_CLASS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "class_id", via: [{ x: 914, y: 458 }, { x: 1148, y: 458 }] },
  { from: "CLASSES", to: "QUESTION_PAPERS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "class_id", via: [{ x: 810, y: 616 }, { x: 810, y: 670 }] },
  { from: "SUBJECTS", to: "QUESTION_PAPERS", fromSide: "left", toSide: "top", fromCard: "1", toCard: "N", label: "subject_id", via: [{ x: 1168, y: 250 }, { x: 1168, y: 670 }, { x: 866, y: 670 }] },
  { from: "TEACHERS", to: "QUESTION_PAPERS", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "created_by_teacher_id", via: [{ x: 652, y: 626 }, { x: 652, y: 808 }] },
  { from: "CLASSES", to: "QUESTION_BANK", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "class_id", via: [{ x: 850, y: 616 }, { x: 1186, y: 616 }, { x: 1186, y: 670 }] },
  { from: "SUBJECTS", to: "QUESTION_BANK", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "subject_id", via: [{ x: 1168, y: 616 }, { x: 1252, y: 616 }, { x: 1252, y: 670 }] },
  { from: "TEACHERS", to: "QUESTION_BANK", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "teacher_id", via: [{ x: 636, y: 606 }, { x: 636, y: 874 }] },
  { from: "CLASSES", to: "ASSIGNMENTS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "class_id", via: [{ x: 872, y: 616 }, { x: 1618, y: 616 }, { x: 1618, y: 670 }] },
  { from: "SUBJECTS", to: "ASSIGNMENTS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "subject_id", via: [{ x: 1206, y: 616 }, { x: 1648, y: 616 }, { x: 1648, y: 670 }] },
  { from: "QUESTION_PAPERS", to: "MARKS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "qp_id" },
  { from: "STUDENTS", to: "MARKS", fromSide: "bottom", toSide: "left", fromCard: "1", toCard: "N", label: "student_id", via: [{ x: 482, y: 980 }, { x: 760, y: 980 }] },
  { from: "ASSIGNMENTS", to: "SUBMISSIONS", fromSide: "bottom", toSide: "top", fromCard: "1", toCard: "N", label: "assignment_id", via: [{ x: 1624, y: 1006 }, { x: 1236, y: 1006 }] },
  { from: "STUDENTS", to: "SUBMISSIONS", fromSide: "bottom", toSide: "left", fromCard: "1", toCard: "N", label: "student_id", via: [{ x: 526, y: 1120 }, { x: 1080, y: 1120 }] },
  { from: "USERS", to: "ATTENDANCE", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "user_id", via: [{ x: 632, y: 430 }, { x: 632, y: 1180 }] },
  { from: "CLASSES", to: "ATTENDANCE", fromSide: "right", toSide: "top", fromCard: "1", toCard: "N", label: "class_id", via: [{ x: 1370, y: 210 }, { x: 1370, y: 1022 }, { x: 1518, y: 1022 }] },
  { from: "USERS", to: "ATTENDANCE", fromSide: "right", toSide: "top", fromCard: "1", toCard: "0..N", label: "approved_by", via: [{ x: 650, y: 352 }, { x: 650, y: 930 }, { x: 1680, y: 930 }, { x: 1680, y: 1022 }] },
  { from: "STUDENTS", to: "COUNSELLING", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "student_id", via: [{ x: 680, y: 626 }, { x: 680, y: 346 }, { x: 1872, y: 346 }] },
  { from: "USERS", to: "COUNSELLING", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "counsellor_id", via: [{ x: 632, y: 330 }, { x: 632, y: 228 }, { x: 1872, y: 228 }] },
  { from: "USERS", to: "COMMUNICATION", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "sender_id / receiver_id", via: [{ x: 632, y: 392 }, { x: 632, y: 546 }, { x: 1832, y: 546 }] },
  { from: "STUDENTS", to: "COMMUNICATION", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "student_id", via: [{ x: 680, y: 670 }, { x: 680, y: 616 }, { x: 1832, y: 616 }] },
  { from: "USERS", to: "COMMUNICATION_READ_STATE", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "viewer_id / partner_id", via: [{ x: 650, y: 454 }, { x: 650, y: 802 }, { x: 1794, y: 802 }] },
  { from: "STUDENTS", to: "COMMUNICATION_READ_STATE", fromSide: "right", toSide: "left", fromCard: "1", toCard: "N", label: "student_id", via: [{ x: 680, y: 694 }, { x: 680, y: 900 }, { x: 1794, y: 900 }] }
];

function esc(text) {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function anchor(tableName, side) {
  const table = tables[tableName];
  if (side === "top") return { x: table.x + table.width / 2, y: table.y };
  if (side === "bottom") return { x: table.x + table.width / 2, y: table.y + table.height };
  if (side === "left") return { x: table.x, y: table.y + table.height / 2 };
  return { x: table.x + table.width, y: table.y + table.height / 2 };
}

function polylinePoints(rel) {
  const start = anchor(rel.from, rel.fromSide);
  const end = anchor(rel.to, rel.toSide);
  return [start, ...(rel.via || []), end];
}

function pointAt(points, t) {
  const segments = [];
  let total = 0;
  for (let i = 0; i < points.length - 1; i += 1) {
    const a = points[i];
    const b = points[i + 1];
    const length = Math.hypot(b.x - a.x, b.y - a.y);
    segments.push({ a, b, length });
    total += length;
  }
  let cursor = total * t;
  for (const segment of segments) {
    if (cursor <= segment.length) {
      const ratio = segment.length === 0 ? 0 : cursor / segment.length;
      return {
        x: segment.a.x + (segment.b.x - segment.a.x) * ratio,
        y: segment.a.y + (segment.b.y - segment.a.y) * ratio
      };
    }
    cursor -= segment.length;
  }
  return points.at(-1);
}

function renderRelation(rel) {
  const points = polylinePoints(rel);
  const path = points.map((point) => `${point.x},${point.y}`).join(" ");
  const labelPoint = pointAt(points, 0.52);
  const fromPoint = pointAt(points, 0.06);
  const toPoint = pointAt(points, 0.94);
  return `
    <polyline points="${path}" fill="none" stroke="${colors.lineSoft}" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" />
    <circle cx="${points[0].x}" cy="${points[0].y}" r="3.2" fill="${colors.lineSoft}" />
    <circle cx="${points.at(-1).x}" cy="${points.at(-1).y}" r="3.2" fill="${colors.lineSoft}" />
    <text x="${fromPoint.x - 10}" y="${fromPoint.y - 8}" font-size="12" font-weight="700" fill="${colors.line}">${esc(rel.fromCard)}</text>
    <text x="${toPoint.x + 6}" y="${toPoint.y - 8}" font-size="12" font-weight="700" fill="${colors.line}">${esc(rel.toCard)}</text>
    ${rel.label ? `<rect x="${labelPoint.x - (rel.label.length * 3.7 + 10)}" y="${labelPoint.y - 15}" width="${rel.label.length * 7.4 + 20}" height="20" rx="10" fill="white" opacity="0.94" />
    <text x="${labelPoint.x}" y="${labelPoint.y}" text-anchor="middle" font-size="12" fill="${colors.muted}">${esc(rel.label)}</text>` : ""}
  `;
}

function renderTable(table) {
  const group = groupPanels.find((item) => item.id === table.group);
  const headerFill = group.color;
  const rows = table.columns
    .map((column, index) => {
      const y = table.y + headerHeight + index * rowHeight;
      return `
        <line x1="${table.x}" y1="${y}" x2="${table.x + table.width}" y2="${y}" stroke="#edf1f6" stroke-width="1" />
        <text x="${table.x + 14}" y="${y + 15}" font-size="13" fill="${colors.text}">${esc(column)}</text>
      `;
    })
    .join("");

  return `
    <g filter="url(#shadow)">
      <rect x="${table.x}" y="${table.y}" width="${table.width}" height="${table.height}" rx="12" fill="${colors.panel}" stroke="${colors.border}" stroke-width="1.3" />
      <rect x="${table.x}" y="${table.y}" width="${table.width}" height="${headerHeight}" rx="12" fill="${headerFill}" />
      <rect x="${table.x}" y="${table.y + 16}" width="${table.width}" height="${headerHeight - 16}" fill="${headerFill}" />
    </g>
    <text x="${table.x + 14}" y="${table.y + 22}" font-size="16" font-weight="700" fill="white">${esc(table.name)}</text>
    ${rows}
  `;
}

function renderGroupPanel(panel) {
  return `
    <rect x="${panel.x}" y="${panel.y}" width="${panel.w}" height="${panel.h}" rx="24" fill="white" opacity="0.55" stroke="${panel.color}" stroke-width="1.4" stroke-dasharray="8 8" />
    <text x="${panel.x + 18}" y="${panel.y - 12}" font-size="18" font-weight="700" fill="${panel.color}">${esc(panel.title)}</text>
  `;
}

const relationLayer = relations.map(renderRelation).join("\n");
const tableLayer = tableNames.map((name) => renderTable(tables[name])).join("\n");
const panelLayer = groupPanels.map(renderGroupPanel).join("\n");

const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${canvas.width}" height="${canvas.height}" viewBox="0 0 ${canvas.width} ${canvas.height}">
  <defs>
    <filter id="shadow" x="-10%" y="-10%" width="130%" height="130%">
      <feDropShadow dx="0" dy="10" stdDeviation="12" flood-color="${colors.shadow}" />
    </filter>
    <linearGradient id="bgGrad" x1="0" x2="1" y1="0" y2="1">
      <stop offset="0%" stop-color="#f8fafc" />
      <stop offset="100%" stop-color="#eef2ff" />
    </linearGradient>
  </defs>

  <rect x="0" y="0" width="${canvas.width}" height="${canvas.height}" fill="url(#bgGrad)" />
  <text x="${margin}" y="40" font-size="28" font-weight="800" fill="${colors.text}">CSMS Project ER Diagram</text>
  <text x="${margin}" y="60" font-size="13" fill="${colors.muted}">Oracle schema from database/csms_full_setup_with_data.sql | 22 tables | PK = Primary Key, FK = Foreign Key, UQ = Unique</text>
  <rect x="2042" y="24" width="486" height="52" rx="14" fill="white" stroke="${colors.border}" stroke-width="1.2" />
  <text x="2062" y="47" font-size="13" fill="${colors.muted}">Includes: identity, academics, exams, assignments,</text>
  <text x="2062" y="64" font-size="13" fill="${colors.muted}">attendance, counselling, messaging, branding and audit tables</text>

  ${panelLayer}
  <g id="relations">${relationLayer}</g>
  <g id="tables">${tableLayer}</g>
</svg>
`;

const outputDir = path.resolve("docs");
fs.mkdirSync(outputDir, { recursive: true });
fs.writeFileSync(path.join(outputDir, "csms_er_diagram.svg"), svg, "utf8");
console.log(`Generated ${path.join(outputDir, "csms_er_diagram.svg")}`);
