"""
QPhotos - Backend Server

This Flask-based server provides the backend functionality for the QPhotos Android application.
It handles file uploads, project management, and serves images and thumbnails.
The server organizes photos into a hierarchical structure based on month, project, and date.
"""

from flask import Flask, request, jsonify, send_from_directory, send_file
import os
from datetime import datetime
from PIL import Image, ImageDraw, ImageFont
import threading
import io
import shutil
import re

app = Flask(__name__)

# --- Configuration ---
UPLOAD_FOLDER = 'upload'
LAST_PROJECT_FILE = 'last_project.txt'
MESES = (
    "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
    "JULIO", "AGOSTO", "SETIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
)

# A lock to ensure thread-safe file processing, preventing race conditions.
file_processing_lock = threading.Lock()

# --- Initialization ---
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# --- Helper Functions ---

def is_date_folder(name):
    """Checks if a string matches the YYYY-MM-DD format."""
    return re.match(r'^\d{4}-\d{2}-\d{2}$', name) is not None

def add_watermark(image_data, text):
    """
    Adds a text watermark with a semi-transparent background to an image.
    """
    try:
        img = Image.open(io.BytesIO(image_data)).convert("RGBA")
        text_layer = Image.new("RGBA", img.size, (255, 255, 255, 0))
        draw = ImageDraw.Draw(text_layer)

        try:
            font = ImageFont.truetype("arial.ttf", size=40)
        except IOError:
            print("ADVERTENCIA: No se pudo cargar la fuente 'arial.ttf'. Usando fuente por defecto.")
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

# --- API Endpoints ---

@app.route('/upload', methods=['POST'])
def upload_file():
    """
    Handles file uploads from the Android app.
    Expects a multipart form with 'file', 'project_name', and 'uuid'.
    """
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
                return jsonify({"success": "Duplicate ignored."}), 200

            image_data = file.read()
            watermark_text = f"{project_name} - {now.strftime('%Y-%m-%d %H:%M:%S')}"
            watermarked_image = add_watermark(image_data, watermark_text)

            if watermarked_image:
                watermarked_image.save(final_path, 'JPEG', quality=95)
                with open(LAST_PROJECT_FILE, 'w') as f:
                    f.write(project_name)
                return jsonify({"success": "File uploaded successfully."}), 200
            else:
                return jsonify({"error": "Failed to process watermark"}), 500
        except Exception as e:
            print(f"Error during upload: {e}")
            return jsonify({"error": "An internal server error occurred"}), 500

@app.route('/project/<path:path>', methods=['PUT', 'DELETE'])
def manage_project(path):
    """
    Manages project folders, including nested ones. Allows renaming (PUT) and deleting (DELETE).

    This endpoint uses a <path:path> converter to capture the full relative path
    of the target folder from the URL (e.g., "05 MAYO/My Project/My Sub-Project").
    It's designed to be platform-independent.

    - PUT: Renames the target folder. Expects a JSON body: {"new_name": "New Name"}
    - DELETE: Recursively deletes the target folder and all its contents.
    """
    base_path = os.path.abspath(UPLOAD_FOLDER)
    
    # Platform-Independent Path Construction:
    # The incoming 'path' from the URL uses forward slashes ('/').
    # To work correctly on any OS (like Windows, which uses backslashes '\\'),
    # we replace them with the OS-specific separator.
    safe_path_fragment = path.replace('/', os.sep)
    target_path = os.path.abspath(os.path.join(base_path, safe_path_fragment))

    # Security check to prevent directory traversal attacks.
    if not target_path.startswith(base_path):
        return jsonify({"error": "Forbidden path"}), 403
    if not os.path.exists(target_path):
        print(f"Attempted to access non-existent path: {target_path}")
        return jsonify({"error": "Project not found"}), 404

    if request.method == 'PUT':
        try:
            new_name = request.json.get('new_name', '').strip()
            if not new_name:
                return jsonify({"error": "New name not provided"}), 400

            parent_dir = os.path.dirname(target_path)
            new_path = os.path.join(parent_dir, new_name)

            os.rename(target_path, new_path)
            print(f"Renamed from '{target_path}' to '{new_path}'")
            return jsonify({"success": "Project renamed"}), 200
        except Exception as e:
            print(f"Error renaming project {path}: {e}")
            return jsonify({"error": "Could not rename project"}), 500

    if request.method == 'DELETE':
        try:
            shutil.rmtree(target_path)
            print(f"Successfully deleted: {target_path}")
            return jsonify({"success": f"Project '{path}' deleted."}), 200
        except Exception as e:
            print(f"Error deleting project {path}: {e}")
            return jsonify({"error": "Could not delete project"}), 500

@app.route('/last-project', methods=['GET'])
def get_last_project():
    """Retrieves the last successfully used project name."""
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

@app.route('/projects_current_month', methods=['GET'])
def list_projects_current_month():
    """Returns a sorted list of all project names from the current month."""
    try:
        now = datetime.now()
        month_num = now.month
        month_name = MESES[month_num - 1]
        current_month_folder = f"{month_num:02d} {month_name}"
        month_path = os.path.join(UPLOAD_FOLDER, current_month_folder)
        if not os.path.isdir(month_path):
            return jsonify([])
        project_folders = [p for p in os.listdir(month_path) if os.path.isdir(os.path.join(month_path, p))]
        project_folders.sort()
        return jsonify(project_folders)
    except Exception as e:
        print(f"Error listing projects for current month: {e}")
        return jsonify({"error": "Could not list projects"}), 500

@app.route('/browse/', defaults={'path': ''})
@app.route('/browse/<path:path>')
def browse(path):
    """
    Acts as a file explorer for the `UPLOAD_FOLDER`.

    Based on the provided `path`, it returns a list of dictionary items,
    each representing a file or folder with its name, type, and relative path.
    The `type` can be "month", "project", "day", or "photo".
    """
    base_path = UPLOAD_FOLDER
    safe_path = os.path.normpath(os.path.join(base_path, path))
    base_path_abs = os.path.abspath(base_path)
    safe_path_abs = os.path.abspath(safe_path)

    if not safe_path_abs.startswith(base_path_abs) or not os.path.exists(safe_path_abs):
        return jsonify({"error": "Path not found or forbidden"}), 404

    items = []
    try:
        path_parts = [part for part in path.split('/') if part]
        depth = len(path_parts)
        is_browsing_date_folder = path and is_date_folder(path_parts[-1])
        dir_contents = os.listdir(safe_path_abs)

        # Sort month folders newest first; otherwise, sort alphabetically.
        if depth == 0:
            dir_contents.sort(reverse=True)
        else:
            dir_contents.sort()

        for entry_name in dir_contents:
            full_path_to_entry = os.path.join(safe_path_abs, entry_name)
            relative_path = os.path.relpath(full_path_to_entry, base_path_abs).replace(os.sep, '/')
            if os.path.isdir(full_path_to_entry):
                item_type = "project"
                if depth == 0:
                    item_type = "month"
                elif is_date_folder(entry_name):
                    item_type = "day"
                items.append({"name": entry_name, "path": relative_path, "type": item_type})
            elif is_browsing_date_folder and entry_name.lower().endswith(('.jpg', '.jpeg')):
                items.append({"name": entry_name, "path": relative_path, "type": "photo"})
        return jsonify(items)
    except Exception as e:
        print(f"Error browsing path '{path}': {e}")
        return jsonify({"error": "Could not browse path"}), 500

@app.route('/uploads/<path:filepath>')
def serve_photo(filepath):
    """Serves a full-resolution photo from the filesystem."""
    return send_from_directory(UPLOAD_FOLDER, filepath)

@app.route('/thumbnail/<path:filepath>')
def serve_thumbnail(filepath):
    """
    Generates and serves a 400x400 thumbnail of a photo on-the-fly.
    """
    try:
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

@app.route('/photo/<path:filepath>', methods=['DELETE'])
def delete_photo(filepath):
    """Deletes a single photo from the filesystem."""
    with file_processing_lock:
        try:
            safe_path_fragment = filepath.replace('/', os.sep)
            photo_path = os.path.abspath(os.path.join(UPLOAD_FOLDER, safe_path_fragment))

            if not os.path.exists(photo_path):
                return jsonify({"error": "Photo not found"}), 404

            os.remove(photo_path)
            print(f"Successfully deleted photo: {photo_path}")
            return jsonify({"success": f"Photo '{filepath}' deleted."}), 200
        except Exception as e:
            print(f"Error deleting photo {filepath}: {e}")
            return jsonify({"error": "Could not delete photo"}), 500

if __name__ == '__main__':
    from waitress import serve
    print("Servidor de producción iniciado en http://0.0.0.0:5000")
    serve(app, host='0.0.0.0', port=5000, threads=8)
