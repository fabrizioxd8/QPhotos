# QPhotos: Field Photography and Organization App

## Overview

QPhotos is a dedicated Android application designed for professionals in the field who need a fast and reliable way to take photos, organize them by project, and upload them to a local server. The app is built to streamline the process of documenting fieldwork, inspections, or any on-site job, ensuring that all photos are correctly labeled, watermarked, and stored in the right place.

The system consists of two main components:
1.  **The Android App**: A Kotlin-based application for capturing, selecting, and queuing photos for upload.
2.  **The Python Server**: A Flask-based server that receives the uploaded images, processes them, and organizes them into a clear folder structure.

## Key Features

### Android Application
- **Camera and Gallery Integration**: Take photos directly within the app or select multiple images from the device's gallery.
- **Project-Based Organization**: Assign a project name to every photo before it's taken or uploaded. The app remembers the last used project name for convenience.
- **Robust Upload Queue**: Photos are added to a background queue, ensuring that uploads can happen reliably even with intermittent network connectivity. The queue is managed by `WorkManager` for robust, battery-efficient background processing.
- **Visual Queue List**: The queue screen provides a clear, visual list of all pending uploads, including a thumbnail of the photo and its project name.
- **Flash Control**: Full control over the camera's flash with "On," "Off," and "Auto" modes.
- **Full-Screen Photo Viewer**: Browse photos within a project in a full-screen, swipeable viewer.
- **Smart Cache Management**: The app intelligently manages its cache size to prevent using excessive storage on the device, with a fixed limit of 250MB.
- **Dark Mode Support**: The project list screen is designed with a dark theme for better viewing in various lighting conditions.

### Python Server
- **Reliable File Uploads**: The server uses a `threading.Lock` and unique IDs (UUIDs) for each photo to prevent race conditions, ensuring that no photos are lost even during rapid, simultaneous uploads.
- **Automatic Folder Structure**: The server automatically organizes photos into a clean folder structure based on the month, project name, and date (`/Month/Project Name/YYYY-MM-DD/`).
- **Automatic Watermarking**: Every uploaded photo is automatically watermarked with the project name and a timestamp.
- **Thumbnail Generation**: The server provides a dedicated endpoint to generate thumbnails on-the-fly, which allows the app's gallery view to load quickly without sacrificing performance.
- **Project Management**: Includes API endpoints for managing projects, such as renaming or deleting them.

## 10 Recommendations for Future Improvements

Here is a list of potential features and enhancements to consider for the future development of QPhotos:

### Functionality Recommendations
1.  **Video Recording**: Add video capture capabilities to allow for more detailed documentation that can't be captured with still photos.
2.  **Advanced Queue Management**: Enhance the "Queue" screen to allow users to delete specific photos, retry failed uploads individually, or re-prioritize the upload order.
3.  **QR Code Project Input**: Implement a feature to scan a QR code or barcode to instantly populate the project name, eliminating typos and speeding up the workflow.
4.  **Geolocation Tagging (Geotagging)**: Automatically capture and save the GPS coordinates when a photo is taken to provide valuable location context.
5.  **Multi-Project Dropdown**: Change the project name input field to a dropdown menu populated with existing project names, making it much faster to switch between active projects.

### UI/UX Recommendations
6.  **A True In-App Gallery**: Transform the `ProjectsActivity` into a more visually appealing, grid-based gallery. When a user taps a project, it could open a dedicated screen showing all photos in that project.
7.  **Detailed Upload Progress**: In the `QueueActivity`, show a real-time progress bar for the currently uploading photo and a clear status (e.g., "Uploading," "Completed," "Failed") for each item.
8.  **Customizable Watermarks**: Add a section in the "Settings" screen to allow users to customize the watermark's position, color, or even disable it entirely.
9.  **Image Quality Settings**: Add an option in "Settings" to choose the upload quality (e.g., High, Medium, Low) to help users manage data usage.
10. **Modern Iconography and Theming**: A full refresh of the app's icons using a consistent, modern icon pack and defining a more distinct color palette in your `themes.xml` file could give the app a unique and professional brand identity.