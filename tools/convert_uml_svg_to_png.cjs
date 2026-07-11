const fs = require("node:fs");
const path = require("node:path");
const sharp = require("sharp");

const inputDir = path.resolve("docs", "uml");
const scale = 2;

async function main() {
  const svgFiles = fs
    .readdirSync(inputDir)
    .filter((name) => name.toLowerCase().endsWith(".svg"))
    .sort();

  for (const file of svgFiles) {
    const input = path.join(inputDir, file);
    const output = path.join(inputDir, file.replace(/\.svg$/i, ".png"));
    const svg = fs.readFileSync(input);
    await sharp(svg, { density: 72 * scale })
      .png({ compressionLevel: 9, adaptiveFiltering: true })
      .toFile(output);
    const metadata = await sharp(output).metadata();
    console.log(`${path.basename(output)} ${metadata.width}x${metadata.height}`);
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
