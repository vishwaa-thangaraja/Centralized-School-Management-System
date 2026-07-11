from pathlib import Path
import math

from PIL import Image, ImageDraw


render_dir = Path("build_verify/table_structures_render")
files = sorted(render_dir.glob("page-*.png"), key=lambda p: int(p.stem.split("-")[1]))
thumb_width = 360
pad = 22
label_h = 24
row_h = int(thumb_width * 1.42) + label_h + pad
cols = 3
rows = math.ceil(len(files) / cols)
canvas = Image.new("RGB", (cols * (thumb_width + pad) + pad, rows * row_h + pad), "white")
draw = ImageDraw.Draw(canvas)

for i, file in enumerate(files):
    image = Image.open(file).convert("RGB")
    thumb_height = int(thumb_width * image.height / image.width)
    image = image.resize((thumb_width, thumb_height))
    x = pad + (i % cols) * (thumb_width + pad)
    y = pad + (i // cols) * row_h
    draw.text((x, y), file.name, fill=(20, 20, 20))
    canvas.paste(image, (x, y + label_h))

canvas.save("build_verify/table_structures_contact_sheet.png")
print("Generated build_verify/table_structures_contact_sheet.png")
