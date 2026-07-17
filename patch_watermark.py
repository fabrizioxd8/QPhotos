with open('server.py', 'r') as f:
    content = f.read()

content = content.replace('draw.textbbox((0, 0), text, font=font)', 'draw.multiline_textbbox((0, 0), text, font=font)')
content = content.replace('draw.text((x, y), text, font=font, fill=(255, 255, 255, 255))', 'draw.multiline_text((x, y), text, font=font, fill=(255, 255, 255, 255))')

with open('server.py', 'w') as f:
    f.write(content)

print("Patched multiline text in server.py")
