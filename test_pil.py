from PIL import Image, ImageDraw, ImageFont
img = Image.new('RGB', (500, 500))
draw = ImageDraw.Draw(img)
text = "Line 1\nLine 2\nLine 3"
font = ImageFont.load_default()
bbox = draw.textbbox((0, 0), text, font=font)
print("textbbox:", bbox)
draw.text((0, 0), text, font=font, fill=(255,255,255))
img.save("test.jpg")
