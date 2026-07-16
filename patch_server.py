with open('server.py', 'r') as f:
    content = f.read()

old_watermark_logic = """            image_data = file.read()
            watermark_text = f"{project_name} - {now.strftime('%Y-%m-%d %H:%M:%S')}"
            watermarked_image = add_watermark(image_data, watermark_text)"""

new_watermark_logic = """            capture_time = request.form.get('capture_time')
            gps_coords = request.form.get('gps_coords')
            address = request.form.get('address')

            image_data = file.read()

            watermark_time = capture_time if capture_time else now.strftime('%Y-%m-%d %H:%M:%S')
            watermark_text = f"{project_name} - {watermark_time}"
            if gps_coords:
                watermark_text += f"\\nGPS: {gps_coords}"
            if address:
                watermark_text += f"\\n{address}"

            watermarked_image = add_watermark(image_data, watermark_text)"""

content = content.replace(old_watermark_logic, new_watermark_logic)

with open('server.py', 'w') as f:
    f.write(content)

print("Patched server.py watermark logic")
