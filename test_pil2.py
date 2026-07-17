import sys
import io
import urllib.request
from PIL import Image, ImageDraw, ImageFont

# download a dummy image
urllib.request.urlretrieve("https://via.placeholder.com/800x600.jpg", "dummy.jpg")

with open("dummy.jpg", "rb") as f:
    image_data = f.read()

text = "Project Alpha - 2023-10-26 14:00:00\nGPS: -12.04318, -77.02824\nAv. Lima 123, Peru"

img = Image.open(io.BytesIO(image_data)).convert("RGBA")
text_layer = Image.new("RGBA", img.size, (255, 255, 255, 0))
draw = ImageDraw.Draw(text_layer)
font = ImageFont.load_default()

bbox = draw.textbbox((0, 0), text, font=font)
text_width = bbox[2] - bbox[0]
text_height = bbox[3] - bbox[1]
width, height = img.size
x = width - text_width - 20
y = height - text_height - 20

rect_coords = (x - 10, y - 10, x + text_width + 10, y + text_height + 10)
draw.rectangle(rect_coords, fill=(0, 0, 0, 102))
draw.text((x, y), text, font=font, fill=(255, 255, 255, 255))
out = Image.alpha_composite(img, text_layer).convert("RGB")
out.save("watermarked_dummy.jpg")
print("Saved watermarked_dummy.jpg successfully.")
