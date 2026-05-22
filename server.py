"""
QPhotos - Backend Server

This Flask-based server provides the backend functionality for the QPhotos Android application.
It handles file uploads, project management, and serves images and thumbnails.
The server organizes photos into a hierarchical structure based on month, project, and date.
"""

from flask import Flask, request, jsonify, send_from_directory, send_file, make_response
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
        latitude = request.form.get('latitude')
        longitude = request.form.get('longitude')

        location_str = ""
        if latitude and longitude:
            try:
                location_str = f" - Lat: {float(latitude):.4f}, Lon: {float(longitude):.4f}"
            except ValueError:
                pass

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
            watermark_text = f"{project_name} - {now.strftime('%Y-%m-%d %H:%M:%S')}{location_str}"
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
    Handles renaming (PUT) and deleting (DELETE) of folders on the server.
    """
    with file_processing_lock:
        target_path = os.path.join(UPLOAD_FOLDER, path)
        target_path = os.path.normpath(target_path)
        if not target_path.startswith(os.path.normpath(UPLOAD_FOLDER)):
            return jsonify({"error": "Invalid path"}), 403

        if request.method == 'DELETE':
            if os.path.exists(target_path):
                try:
                    shutil.rmtree(target_path)
                    return jsonify({"success": "Folder deleted"}), 200
                except Exception as e:
                    return jsonify({"error": f"Failed to delete: {e}"}), 500
            return jsonify({"error": "Folder not found"}), 404

        elif request.method == 'PUT':
            data = request.json
            new_name = data.get('new_name')
            if not new_name:
                return jsonify({"error": "Missing new_name"}), 400

            parent_dir = os.path.dirname(target_path)
            new_path = os.path.join(parent_dir, new_name)
            new_path = os.path.normpath(new_path)

            if not new_path.startswith(os.path.normpath(UPLOAD_FOLDER)):
                 return jsonify({"error": "Invalid path"}), 403

            if os.path.exists(target_path):
                try:
                    os.rename(target_path, new_path)
                    return jsonify({"success": "Folder renamed"}), 200
                except Exception as e:
                    return jsonify({"error": f"Failed to rename: {e}"}), 500
            return jsonify({"error": "Folder not found"}), 404

@app.route('/thumbnail/<path:filename>')
def get_thumbnail(filename):
    """
    Generates and returns a thumbnail for a given image file.
    Thumbnails are cached on disk to improve performance on subsequent requests.
    Includes HTTP cache headers for client-side caching.
    """
    full_path = os.path.join(UPLOAD_FOLDER, filename)
    full_path = os.path.normpath(full_path)
    if not full_path.startswith(os.path.normpath(UPLOAD_FOLDER)):
         return "Invalid path", 403

    if not os.path.exists(full_path):
        return "File not found", 404

    # The cache folder is placed parallel to the upload folder
    cache_dir = os.path.join(os.path.dirname(UPLOAD_FOLDER), '.thumbnails')
    os.makedirs(cache_dir, exist_ok=True)

    # Create a unique, safe filename for the cached thumbnail based on the original file's path
    safe_filename = filename.replace('/', '_').replace('\\', '_')
    cached_thumb_path = os.path.join(cache_dir, f"thumb_{safe_filename}")

    # Check if a cached thumbnail exists and is newer than the original file
    if os.path.exists(cached_thumb_path):
         if os.path.getmtime(cached_thumb_path) >= os.path.getmtime(full_path):
             response = make_response(send_file(cached_thumb_path, mimetype='image/jpeg'))
             # Tell the client to cache this thumbnail for 1 hour
             response.headers['Cache-Control'] = 'public, max-age=3600'
             return response

    # Generate a new thumbnail if it's not cached or the cache is stale
    try:
        img = Image.open(full_path)
        img.thumbnail((400, 400), Image.Resampling.LANCZOS)

        img.save(cached_thumb_path, 'JPEG', quality=85)

        response = make_response(send_file(cached_thumb_path, mimetype='image/jpeg'))
        response.headers['Cache-Control'] = 'public, max-age=3600'
        return response
    except Exception as e:
        print(f"Error generating thumbnail: {e}")
        return "Error generating thumbnail", 500

@app.route('/browse', defaults={'subpath': ''})
@app.route('/browse/<path:subpath>')
def browse(subpath):
    """
    Returns the contents of a directory.
    If the requested path is a date folder, it returns the list of files (images).
    Otherwise, it returns the list of subdirectories (months, projects, dates).
    """
    target_path = os.path.join(UPLOAD_FOLDER, subpath)
    target_path = os.path.normpath(target_path)

    if not target_path.startswith(os.path.normpath(UPLOAD_FOLDER)):
        return jsonify({"error": "Invalid path"}), 403

    if not os.path.exists(target_path) or not os.path.isdir(target_path):
        return jsonify({"error": "Directory not found"}), 404

    try:
        contents = os.listdir(target_path)
        folder_name = os.path.basename(target_path)

        if is_date_folder(folder_name):
            files = [f for f in contents if os.path.isfile(os.path.join(target_path, f))]
            return jsonify({"type": "files", "contents": files})
        else:
            dirs = [d for d in contents if os.path.isdir(os.path.join(target_path, d))]
            # Sort directories: reverse order for date folders so newest is first
            if all(is_date_folder(d) for d in dirs):
                 dirs.sort(reverse=True)
            else:
                 dirs.sort()
            return jsonify({"type": "folders", "contents": dirs})
    except Exception as e:
        print(f"Error browsing {subpath}: {e}")
        return jsonify({"error": "Internal server error"}), 500

@app.route('/projects_current_month', methods=['GET'])
def get_projects_current_month():
    """
    Returns a list of project folders within the current month's folder.
    Used to populate the dropdown in the Android app.
    """
    now = datetime.now()
    month_num = now.month
    month_name = MESES[month_num - 1]
    month_folder = f"{month_num:02d} {month_name}"
    target_path = os.path.join(UPLOAD_FOLDER, month_folder)

    if not os.path.exists(target_path) or not os.path.isdir(target_path):
        return jsonify([])

    try:
        contents = os.listdir(target_path)
        projects = [d for d in contents if os.path.isdir(os.path.join(target_path, d))]
        projects.sort()
        return jsonify(projects)
    except Exception as e:
        print(f"Error listing projects: {e}")
        return jsonify([])

@app.route('/last-project', methods=['GET'])
def get_last_project():
    """
    Returns the name of the last project that a file was uploaded to.
    """
    if os.path.exists(LAST_PROJECT_FILE):
        with open(LAST_PROJECT_FILE, 'r') as f:
            return f.read().strip()
    return ""

@app.route('/file/<path:filename>')
def serve_file(filename):
    """
    Serves a full-resolution image file.
    Includes HTTP cache headers so the client caches it for 24 hours.
    """
    full_path = os.path.join(UPLOAD_FOLDER, filename)
    full_path = os.path.normpath(full_path)

    if not full_path.startswith(os.path.normpath(UPLOAD_FOLDER)):
         return "Invalid path", 403

    if not os.path.exists(full_path):
        return "File not found", 404

    response = make_response(send_file(full_path, mimetype='image/jpeg'))
    # Tell the client to cache full-size images for 24 hours (86400 seconds)
    response.headers['Cache-Control'] = 'public, max-age=86400'
    return response

# --- Application Entry Point ---
if __name__ == '__main__':
    from waitress import serve
    print("Servidor de producción iniciado en http://0.0.0.0:5000")
    serve(app, host='0.0.0.0', port=5000, threads=4)
