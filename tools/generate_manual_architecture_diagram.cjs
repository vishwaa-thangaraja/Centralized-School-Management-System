const fs = require("node:fs");
const path = require("node:path");
const sharp = require("sharp");

const outDir = path.resolve("docs", "uml");
const svgPath = path.join(outDir, "manual_architecture.svg");
const pngPath = path.join(outDir, "manual_architecture.png");

const C = {
  bg: "#ffffff",
  ink: "#111111",
  muted: "#333333",
  line: "#111111",
  blue: "#111111",
  green: "#111111",
  cyan: "#111111",
  border: "#111111",
  panel: "#ffffff",
  header: "#ffffff",
  service: "#ffffff",
  data: "#ffffff",
  module: "#ffffff"
};

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function wrap(text, limit) {
  const words = String(text).split(/\s+/);
  const lines = [];
  let line = "";

  for (const word of words) {
    const next = `${line} ${word}`.trim();
    if (next.length > limit && line) {
      lines.push(line);
      line = word;
    } else {
      line = next;
    }
  }

  if (line) lines.push(line);
  return lines;
}

function textLines(x, y, lines, cls, anchor = "start", gap = 18) {
  return lines
    .map((line, index) => `<text x="${x}" y="${y + index * gap}" class="${cls}" text-anchor="${anchor}">${esc(line)}</text>`)
    .join("\n");
}

function box({ x, y, w, h, title, subtitle, items = [], fill = C.module, header = C.header, accent = C.blue }) {
  const itemText = items
    .map((item, index) => `<text x="${x + 28}" y="${y + 76 + index * 21}" class="item">${esc(item)}</text>`)
    .join("\n");

  return `
  <g>
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="${fill}" stroke="${C.border}" stroke-width="1.6"/>
    <rect x="${x}" y="${y}" width="${w}" height="40" rx="4" fill="${header}" stroke="${C.border}" stroke-width="1.6"/>
    <line x1="${x}" y1="${y + 40}" x2="${x + w}" y2="${y + 40}" stroke="${C.border}" stroke-width="1.4"/>
    <rect x="${x}" y="${y}" width="6" height="${h}" rx="3" fill="${accent}"/>
    <text x="${x + 20}" y="${y + 26}" class="head">${esc(title)}</text>
    ${subtitle ? `<text x="${x + 20}" y="${y + 58}" class="sub">${esc(subtitle)}</text>` : ""}
    ${itemText}
  </g>`;
}

function layer({ x, y, w, h, title, subtitle, fill, accent }) {
  return `
  <g>
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="${fill}" stroke="${C.border}" stroke-width="1.7"/>
    <rect x="${x}" y="${y}" width="6" height="${h}" rx="3" fill="${accent}"/>
    <text x="${x + w / 2}" y="${y + 29}" class="head" text-anchor="middle">${esc(title)}</text>
    ${textLines(x + w / 2, y + 54, wrap(subtitle, 30), "sub", "middle", 17)}
  </g>`;
}

function db({ x, y, w, h }) {
  const cx = x + w / 2;
  return `
  <g>
    <path d="M ${x} ${y + 18}
      C ${x} ${y - 6}, ${x + w} ${y - 6}, ${x + w} ${y + 18}
      L ${x + w} ${y + h - 18}
      C ${x + w} ${y + h + 6}, ${x} ${y + h + 6}, ${x} ${y + h - 18}
      Z" fill="${C.data}" stroke="${C.border}" stroke-width="1.7"/>
    <ellipse cx="${cx}" cy="${y + 18}" rx="${w / 2}" ry="22" fill="#ffffff" stroke="${C.border}" stroke-width="1.7"/>
    <path d="M ${x} ${y + 54} C ${x} ${y + 78}, ${x + w} ${y + 78}, ${x + w} ${y + 54}" fill="none" stroke="${C.border}" stroke-width="1.3"/>
    <path d="M ${x} ${y + 90} C ${x} ${y + 114}, ${x + w} ${y + 114}, ${x + w} ${y + 90}" fill="none" stroke="${C.border}" stroke-width="1.3"/>
    <text x="${cx}" y="${y + 61}" class="head" text-anchor="middle">Oracle</text>
    <text x="${cx}" y="${y + 86}" class="sub" text-anchor="middle">Database</text>
  </g>`;
}

function marker(name, color) {
  return `<marker id="${name}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto">
    <path d="M 0 0 L 10 5 L 0 10 z" fill="${color}"/>
  </marker>`;
}

function line(points, color = C.line, markerId = "arrowLine", dashed = false) {
  return `<polyline points="${points}" fill="none" stroke="${color}" stroke-width="2.2" ${dashed ? 'stroke-dasharray="7 6"' : ""} marker-end="url(#${markerId})"/>`;
}

function legend(x, y, label, color, dashed = false) {
  return `
  <g>
    <line x1="${x}" y1="${y}" x2="${x + 72}" y2="${y}" stroke="${color}" stroke-width="2.4" ${dashed ? 'stroke-dasharray="7 6"' : ""}/>
    <path d="M ${x + 72} ${y - 5} L ${x + 82} ${y} L ${x + 72} ${y + 5} z" fill="${color}"/>
    <text x="${x + 100}" y="${y + 5}" class="legend">${esc(label)}</text>
  </g>`;
}

const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1500" height="980" viewBox="0 0 1500 980">
<defs>
  ${marker("arrowLine", C.line)}
  ${marker("arrowReq", C.cyan)}
  ${marker("arrowSecure", C.green)}
  <style>
    .title { font: 700 30px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
    .caption { font: 400 14px Arial, Helvetica, sans-serif; fill: ${C.muted}; }
    .head { font: 700 17px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
    .sub { font: 400 14px Arial, Helvetica, sans-serif; fill: ${C.muted}; }
    .item { font: 400 13px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
    .legend { font: 400 13px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
  </style>
</defs>
<rect width="1500" height="980" fill="${C.bg}"/>
<text x="750" y="46" class="title" text-anchor="middle">Centralized School Management System - Unified Architecture</text>
<text x="750" y="72" class="caption" text-anchor="middle">Component view showing module access, service flow, and database integration</text>

${box({ x: 70, y: 160, w: 315, h: 205, title: "Administrative Module", subtitle: "System governance", accent: C.line, items: [
  "Infrastructure Management",
  "Identity & Access Management",
  "Academic Mapping Engine",
  "Security Auditing",
  "Communication Management",
  "Data Management",
  "System Reports"
] })}

${box({ x: 70, y: 410, w: 315, h: 230, title: "Faculty / Teacher Module", subtitle: "Teaching operations", accent: C.line, items: [
  "Attendance Registry",
  "Assignment Lifecycle",
  "Performance Evaluation",
  "Lesson Planning",
  "Parent-Teacher Connect",
  "Class Communication",
  "Resource Management",
  "Teacher Reports"
] })}

${layer({ x: 560, y: 105, w: 380, h: 82, title: "JavaFX Presentation Layer", subtitle: "FXML / CSS", fill: C.header, accent: C.line })}
${layer({ x: 595, y: 245, w: 310, h: 82, title: "Controller Layer", subtitle: "Action handling", fill: "#ffffff", accent: C.line })}
${layer({ x: 595, y: 385, w: 310, h: 104, title: "Application Services", subtitle: "SHA-256, SMTP / OTP, PDF Service", fill: C.service, accent: C.line })}
${layer({ x: 595, y: 550, w: 310, h: 82, title: "Data Access Layer", subtitle: "JDBC / DAO", fill: C.data, accent: C.line })}
${db({ x: 650, y: 710, w: 200, h: 132 })}

${box({ x: 1115, y: 135, w: 315, h: 185, title: "Student Module", subtitle: "Student self-service", accent: C.line, items: [
  "Profile Management",
  "Attendance & Timetable",
  "Academic Progress",
  "Assignments",
  "Resource Access",
  "Notifications"
] })}

${box({ x: 1115, y: 350, w: 315, h: 185, title: "Parent Module", subtitle: "Ward monitoring", accent: C.line, items: [
  "Ward Switching",
  "Real-time Alerts",
  "Attendance Tracker",
  "Academic Overview",
  "Fee Overview",
  "Communication"
] })}

${box({ x: 1115, y: 565, w: 315, h: 150, title: "Counsellor Module", subtitle: "Student support", accent: C.line, items: [
  "Student Counselling",
  "Wellness Tracking",
  "Appointment Management",
  "Reports & Insights"
] })}

${box({ x: 1115, y: 745, w: 315, h: 150, title: "Connect Module", subtitle: "Collaboration", accent: C.line, items: [
  "Chat Module",
  "Group Discussions",
  "File Sharing",
  "Meeting & Webinar"
] })}

${line("750,187 750,245", C.cyan, "arrowReq")}
${line("750,327 750,385", C.green, "arrowSecure")}
${line("750,489 750,550", C.cyan, "arrowReq")}
${line("750,632 750,710", C.cyan, "arrowReq")}

${line("595,286 435,286 435,263 385,263", C.green, "arrowSecure")}
${line("595,286 435,286 435,525 385,525", C.green, "arrowSecure")}
${line("905,286 1030,286 1030,228 1115,228", C.green, "arrowSecure")}
${line("905,437 1028,437 1028,442 1115,442", C.green, "arrowSecure")}
${line("905,437 1028,437 1028,640 1115,640", C.green, "arrowSecure")}
${line("905,437 1028,437 1028,820 1115,820", C.green, "arrowSecure")}

${line("385,263 505,263 505,437 595,437", C.line, "arrowLine", true)}
${line("385,525 505,525 505,437 595,437", C.line, "arrowLine", true)}
${line("1115,228 1000,228 1000,592 905,592", C.line, "arrowLine", true)}
${line("1115,442 1000,442 1000,437 905,437", C.line, "arrowLine", true)}
${line("1115,640 1000,640 1000,437 905,437", C.line, "arrowLine", true)}
${line("1115,820 1000,820 1000,437 905,437", C.line, "arrowLine", true)}

${legend(350, 930, "Request / response flow", C.cyan)}
${legend(620, 930, "Secure service flow", C.green)}
${legend(895, 930, "Data dependency", C.line, true)}
</svg>
`;

async function main() {
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(svgPath, svg, "utf8");
  await sharp(Buffer.from(svg), { density: 144 })
    .png({ compressionLevel: 9, adaptiveFiltering: true })
    .toFile(pngPath);

  const metadata = await sharp(pngPath).metadata();
  console.log(`wrote ${path.relative(process.cwd(), svgPath)}`);
  console.log(`wrote ${path.relative(process.cwd(), pngPath)} ${metadata.width}x${metadata.height}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
