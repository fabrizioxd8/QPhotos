from flask import Flask, request, jsonify, send_from_directory, send_file
import os
from datetime import datetime
from PIL import Image, ImageDraw, ImageFont
import threading
import io
import shutil

app = Flask(__name__)

# --- Configuration ---
UPLOAD_FOLDER = 'upload'
LAST_PROJECT_FILE = 'last_project.txt'
MESES = (
    "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
    "JULIO", "AGOSTO", "SETIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
)

file_processing_lock = threading.Lock()

if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# CAMBIO: The add_watermark function now accepts image data (bytes) and returns an Image object
def add_watermark(image_data, text):
    try:
        img = Image.open(io.BytesIO(image_data)).convert("RGBA")
        text_layer = Image.new("RGBA", img.size, (255, 255, 255, 0))
        draw = ImageDraw.Draw(text_layer)

        try:
            font = ImageFont.truetype("arial.ttf", size=40)
        except IOError:
            print(f"ADVERTENCIA: No se pudo cargar la fuente '{FONT_FILE}'. Usando fuente por defecto.")
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

        out = Image.alpha_composite(img, text_layer)
        return out.convert("RGB")

    except Exception as e:
        print(f"Error adding watermark: {e}")
        return None

@app.route('/upload', methods=['POST'])
def upload_file():
    with file_processing_lock:
        uuid = request.form.get('uuid')
        if not uuid:
            return jsonify({"error": "Missing unique ID (uuid)"}), 400

        if 'file' not in request.files:
            return jsonify({"error": "No file part in the request"}), 400

        file = request.files['file']
        project_name = request.form.get('project_name', 'Default_Project').strip()

        if file.filename == '' or project_name == '':
            return jsonify({"error": "Missing file or project name"}), 400

        try:
            now = datetime.now()
            today_folder = now.strftime("%Y-%m-%d")
            month_num = now.month
            month_name = MESES[month_num - 1]
            month_folder = f"{month_num:02d} {month_name}"

            project_path = os.path.join(UPLOAD_FOLDER, month_folder, project_name, today_folder)
            os.makedirs(project_path, exist_ok=True)

            filename = f"{uuid}.jpg"
            final_path = os.path.join(project_path, filename)

            if os.path.exists(final_path):
                print(f"Duplicate upload detected for UUID {uuid}. Ignoring.")
                return jsonify({"success": "Duplicate ignored."}), 200

            image_data = file.read()
            watermark_text = f"{project_name} - {now.strftime('%Y-%m-%d %H:%M:%S')}"
            watermarked_image = add_watermark(image_data, watermark_text)

            if watermarked_image:
                watermarked_image.save(final_path)

                with open(LAST_PROJECT_FILE, 'w') as f:
                    f.write(project_name)

                return jsonify({"success": "File uploaded and processed successfully."}), 200
            else:
                return jsonify({"error": "Failed to process watermark"}), 500

        except Exception as e:
            print(f"Error during upload process: {e}")
            return jsonify({"error": "An internal server error occurred"}), 500


@app.route('/project/<month>/<name>', methods=['PUT', 'DELETE'])
def manage_project(month, name):
    # --- Lógica para Renombrar (PUT) ---
    if request.method == 'PUT':
        try:
            # Obtenemos el nuevo nombre del cuerpo de la petición
            new_name = request.json.get('new_name', '').strip()
            if not new_name:
                return jsonify({"error": "New name not provided"}), 400

            old_path = os.path.join(UPLOAD_FOLDER, month, name)
            new_path = os.path.join(UPLOAD_FOLDER, month, new_name)

            if not os.path.isdir(old_path):
                return jsonify({"error": "Project not found"}), 404

            # Renombramos la carpeta
            os.rename(old_path, new_path)
            print(f"Renamed project '{name}' to '{new_name}'")
            return jsonify({"success": "Project renamed"}), 200

        except Exception as e:
            print(f"Error renaming project {name}: {e}")
            return jsonify({"error": "Could not rename project"}), 500

    # --- Lógica para Borrar (DELETE) ---
    if request.method == 'DELETE':
        try:
            project_path = os.path.join(UPLOAD_FOLDER, month, name)

            if not os.path.isdir(project_path):
                return jsonify({"error": "Project not found"}), 404

            shutil.rmtree(project_path)
            print(f"Successfully deleted project: {project_path}")
            return jsonify({"success": f"Project '{name}' deleted."}), 200

        except Exception as e:
            print(f"Error deleting project {name}: {e}")
            return jsonify({"error": "Could not delete project"}), 500

@app.route('/last-project', methods=['GET'])
def get_last_project():
    try:
        if os.path.exists(LAST_PROJECT_FILE):
            with open(LAST_PROJECT_FILE, 'r') as f:
                last_project = f.read().strip()
            return jsonify({"last_project": last_project})
        else:
            return jsonify({"last_project": ""})
    except Exception as e:
        print(f"Error reading last project file: {e}")
        return jsonify({"error": "Could not read last project file"}), 500

@app.route('/projects', methods=['GET'])
def list_projects():
    projects_data = []
    try:
        month_folders = [d for d in os.listdir(UPLOAD_FOLDER) if os.path.isdir(os.path.join(UPLOAD_FOLDER, d))]
        for month in month_folders:
            month_path = os.path.join(UPLOAD_FOLDER, month)
            project_folders = [p for p in os.listdir(month_path) if os.path.isdir(os.path.join(month_path, p))]
            for project in project_folders:
                project_dir = os.path.join(month_path, project)

                latest_photo_path = ""
                all_photos = []
                for root, _, files in os.walk(project_dir):
                    for file in files:
                        if file.lower().endswith('.jpg'):
                            all_photos.append(os.path.join(root, file))

                if all_photos:
                    latest_photo_full_path = max(all_photos, key=os.path.getmtime)
                    latest_photo_path = os.path.relpath(latest_photo_full_path, UPLOAD_FOLDER)
                    latest_photo_path = latest_photo_path.replace(os.sep, '/')

                projects_data.append({
                    "month": month,
                    "name": project,
                    "thumbnail": latest_photo_path
                })
        return jsonify(projects_data)
    except Exception as e:
        print(f"Error listing projects: {e}")
        return jsonify({"error": "Could not list projects"}), 500

# --- NUEVO: Endpoint para listar todas las fotos de un proyecto específico ---
@app.route('/photos/<month>/<project>', methods=['GET'])
def list_photos(month, project):
    photo_urls = []
    try:
        project_dir = os.path.join(UPLOAD_FOLDER, month, project)
        date_folders = [d for d in os.listdir(project_dir) if os.path.isdir(os.path.join(project_dir, d))]
        for date_folder in sorted(date_folders, reverse=True):
            photos = [f for f in os.listdir(os.path.join(project_dir, date_folder)) if f.endswith('.jpg')]
            for photo in sorted(photos, reverse=True):
                url = f"{month}/{project}/{date_folder}/{photo}"
                photo_urls.append(url)
        return jsonify(photo_urls)
    except Exception as e:
        print(f"Error listing photos for project {project}: {e}")
        return jsonify({"error": "Could not list photos"}), 500

# --- NUEVO: Endpoint para servir los archivos de imagen estáticos ---
# This allows the app to request an image like: http://.../uploads/09 SETIEMBRE/Project A/2025-09-10/14-30-05.jpg
@app.route('/uploads/<path:filepath>')
def serve_photo(filepath):
    return send_from_directory(UPLOAD_FOLDER, filepath)

@app.route('/thumbnail/<path:filepath>')
def serve_thumbnail(filepath):
    try:
        # THE FIX IS HERE: The path now starts from the correct UPLOAD_FOLDER
        image_path = os.path.join(UPLOAD_FOLDER, filepath)

        if not os.path.exists(image_path):
            return "File not found", 404

        img = Image.open(image_path)
        img.thumbnail((400, 400))

        img_io = io.BytesIO()
        img.save(img_io, 'JPEG', quality=85)
        img_io.seek(0)

        return send_file(img_io, mimetype='image/jpeg')

    except Exception as e:
        print(f"Error creating thumbnail for {filepath}: {e}")
        return "Error", 500


if __name__ == '__main__':
    from waitress import serve
    # We need to import send_file for the new thumbnail endpoint
    from flask import send_file
    print("Servidor de producción iniciado en http://0.0.0.0:5000")
    serve(app, host='0.0.0.0', port=5000, threads=8)