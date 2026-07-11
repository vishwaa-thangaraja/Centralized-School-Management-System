const fs = require("node:fs");
const path = require("node:path");
const sharp = require("sharp");

const outDir = path.resolve("docs", "uml");
const svgPath = path.join(outDir, "manual_package_diagram.svg");
const pngPath = path.join(outDir, "manual_package_diagram.png");

const C = {
  bg: "#ffffff",
  ink: "#111827",
  muted: "#4b5563",
  line: "#374151",
  border: "#111111",
  violet: "#ffffff",
  violetFill: "#ffffff",
  amber: "#ffffff",
  amberFill: "#ffffff",
  blue: "#ffffff",
  blueFill: "#ffffff",
  green: "#ffffff",
  greenFill: "#ffffff"
};

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function textLines(x, y, lines, cls = "pkgText", anchor = "middle", gap = 23) {
  return lines
    .map((line, i) => `<text x="${x}" y="${y + i * gap}" class="${cls}" text-anchor="${anchor}">${esc(line)}</text>`)
    .join("\n");
}

function pkg({ x, y, w, h, name, lines, tab = C.violet, fill = C.violetFill }) {
  return `
  <g>
    <path d="M ${x} ${y + 20}
      L ${x} ${y + 8}
      Q ${x} ${y} ${x + 8} ${y}
      L ${x + 78} ${y}
      Q ${x + 86} ${y} ${x + 86} ${y + 8}
      L ${x + 86} ${y + 20}
      L ${x + w} ${y + 20}
      Q ${x + w + 6} ${y + 20} ${x + w + 6} ${y + 26}
      L ${x + w + 6} ${y + h}
      Q ${x + w + 6} ${y + h + 6} ${x + w} ${y + h + 6}
      L ${x} ${y + h + 6}
      Q ${x - 6} ${y + h + 6} ${x - 6} ${y + h}
      L ${x - 6} ${y + 26}
      Q ${x - 6} ${y + 20} ${x} ${y + 20}
      Z" fill="${fill}" stroke="${C.border}" stroke-width="1.6"/>
    <path d="M ${x} ${y + 20} L ${x} ${y + 8} Q ${x} ${y} ${x + 8} ${y} L ${x + 78} ${y} Q ${x + 86} ${y} ${x + 86} ${y + 8} L ${x + 86} ${y + 20} Z" fill="${tab}" stroke="${C.border}" stroke-width="1.6"/>
    <text x="${x + w / 2}" y="${y + 58}" class="pkgName" text-anchor="middle">${esc(name)}</text>
    ${textLines(x + w / 2, y + 86, lines)}
  </g>`;
}

function marker(name, color) {
  return `<marker id="${name}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto">
    <path d="M 0 0 L 10 5 L 0 10 z" fill="${color}"/>
  </marker>`;
}

function dep(points, label, dashed = true) {
  const values = points.trim().split(/\s+/).map((pair) => pair.split(",").map(Number));
  const mid = values[Math.floor(values.length / 2)];
  const prev = values[Math.max(0, Math.floor(values.length / 2) - 1)];
  const x = (mid[0] + prev[0]) / 2;
  const y = (mid[1] + prev[1]) / 2;
  const width = Math.max(80, label.length * 9 + 24);
  return `
  <polyline points="${points}" fill="none" stroke="${C.line}" stroke-width="2" ${dashed ? 'stroke-dasharray="8 6"' : ""} marker-end="url(#arrow)"/>
  <rect x="${x - width / 2}" y="${y - 18}" width="${width}" height="26" rx="3" fill="${C.bg}"/>
  <text x="${x}" y="${y + 1}" class="stereo" text-anchor="middle">${esc(label)}</text>`;
}

function solid(points, label) {
  return dep(points, label, false).replace('stroke-dasharray="8 6"', "");
}

const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="940" viewBox="0 0 1600 940">
<defs>
  ${marker("arrow", C.line)}
  <style>
    .title { font: 700 34px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
    .subtitle { font: 400 17px Arial, Helvetica, sans-serif; fill: ${C.muted}; }
    .pkgName { font: 700 22px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
    .pkgText { font: 400 20px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
    .stereo { font: 700 16px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
    .legend { font: 400 15px Arial, Helvetica, sans-serif; fill: ${C.ink}; }
  </style>
</defs>
<rect width="1600" height="940" fill="${C.bg}"/>
<text x="800" y="55" class="title" text-anchor="middle">UML Package Diagram</text>
<text x="800" y="84" class="subtitle" text-anchor="middle">Centralized School Management System</text>

${pkg({ x: 100, y: 165, w: 250, h: 125, name: "main", lines: ["Application", "Launcher"], tab: C.violet, fill: C.violetFill })}
${pkg({ x: 480, y: 165, w: 280, h: 125, name: "view", lines: ["FXML Screens", "Dashboard UI"], tab: C.violet, fill: C.violetFill })}
${pkg({ x: 880, y: 165, w: 320, h: 125, name: "controller", lines: ["Login, Admin", "Teacher, Student"], tab: C.violet, fill: C.violetFill })}

${pkg({ x: 480, y: 405, w: 300, h: 125, name: "service", lines: ["Auth, OTP", "File + Theme"], tab: C.amber, fill: C.amberFill })}
${pkg({ x: 880, y: 405, w: 300, h: 125, name: "dao", lines: ["UserDAO", "JDBC Access"], tab: C.amber, fill: C.amberFill })}
${pkg({ x: 1280, y: 405, w: 250, h: 125, name: "resources", lines: ["CSS, Images", "PDF Folders"], tab: C.blue, fill: C.blueFill })}

${pkg({ x: 480, y: 675, w: 300, h: 125, name: "model", lines: ["User, Role", "Records"], tab: C.green, fill: C.greenFill })}
${pkg({ x: 880, y: 675, w: 300, h: 125, name: "database", lines: ["Oracle Schema", "setup.sql"], tab: C.green, fill: C.greenFill })}

${dep("356,230 480,230", "<<use>>")}
${solid("766,230 880,230", "<<merge>>")}
${dep("1040,296 660,405", "<<use>>")}
${dep("1095,296 1030,405", "<<access>>")}
${dep("1206,230 1322,405", "<<use>>")}
${dep("786,470 880,470", "<<use>>")}
${dep("630,536 630,675", "<<import>>")}
${dep("1030,536 740,675", "<<import>>")}
${dep("1030,536 1030,675", "<<access>>")}
${dep("1186,470 1280,470", "<<access>>")}

<g>
  <line x1="520" y1="875" x2="625" y2="875" stroke="${C.line}" stroke-width="2" stroke-dasharray="8 6" marker-end="url(#arrow)"/>
  <text x="650" y="880" class="legend">dependency</text>
  <line x1="780" y1="875" x2="885" y2="875" stroke="${C.line}" stroke-width="2" marker-end="url(#arrow)"/>
  <text x="910" y="880" class="legend">merge</text>
</g>
</svg>`;

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
