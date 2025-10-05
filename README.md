# QPhotos: Field Photography & Organization App

## Overview

QPhotos is a dedicated Android application designed for professionals in the field who need a fast, modern, and reliable way to take photos, organize them by project, and upload them to a local server. The app is built to streamline the process of documenting fieldwork, inspections, or any on-site job, ensuring that all photos are correctly labeled and stored in the right place.

This project follows modern Android development practices, featuring a clean, decoupled architecture that makes it easy to maintain and extend.

## Core Components

The system consists of two main parts:

1.  **The Android App**: A Kotlin-based application for capturing, selecting, and queuing photos for upload. It features a clean architecture that separates UI, networking, and camera logic.
2.  **The Python Server**: A Flask-based server that receives the uploaded images, processes them, and organizes them into a clear, hierarchical folder structure. It is designed to be platform-independent and can manage nested folder structures.

## App Architecture & Key Features

The QPhotos Android app is built with a focus on maintainability and modern development standards.

### Key Architectural Components

-   **`ApiClient` (Singleton)**: All networking logic is centralized in this singleton object. It handles all communication with the backend server, from fetching project lists to uploading files. This decouples the UI from network operations and ensures consistency across the app.
-   **`CameraHandler` (Helper Class)**: All camera functionality is encapsulated within this dedicated handler. It manages the camera lifecycle, captures images, and handles flash settings, all while being completely decoupled from `MainActivity`. This simplifies the Activity and makes camera logic reusable.
-   **Modern Concurrency & Permissions**: The app uses `WorkManager` for robust background uploads, `Coil` for efficient image loading, and the modern **Activity Result APIs** for handling permissions and gallery selection, eliminating the need for deprecated patterns.
-   **Room Database**: A local Room database is used to persist the upload queue, ensuring that pending uploads are not lost if the app is closed.

### User-Facing Features

-   **Effortless Photo Capture**: Take photos directly within the app using a modern CameraX implementation.
-   **Multiple Project Input Methods**: Users can type a new project name, select a recent project from a dropdown, or tap the "last project" label for maximum speed.
-   **Gallery Integration**: Select multiple images from the device's gallery to add to the upload queue.
-   **Robust Upload Queue**: Photos are added to a background queue managed by `WorkManager`, ensuring reliable uploads even with intermittent network connectivity.
-   **Hierarchical File Explorer**: Browse projects on the server through a clean, multi-level file explorer. Users can also **long-press** on any folder (including nested sub-folders) to **rename** or **delete** it.
-   **Full-Screen Photo Viewer**: View photos with a smooth, interactive, full-screen viewer.
-   **Bilingual Support**: The app is fully localized in both English and Spanish.

## Future Improvement Recommendations

Here is a list of potential features and enhancements to consider for the future:

1.  **Video Recording**: Extend `CameraHandler` to support video capture for more detailed documentation.
2.  **Advanced Queue Management**: Enhance the `QueueActivity` to allow users to delete specific photos from the queue or retry failed uploads individually.
3.  **QR Code Project Input**: Implement a feature to scan a QR code to instantly populate the project name, reducing manual entry and errors.
4.  **Geolocation Tagging**: Add functionality to `CameraHandler` to capture and save GPS coordinates when a photo is taken.
5.  **Offline Mode**: Enhance the `ApiClient` and Room database to cache server data, allowing users to browse already-synced projects even without a network connection.
6.  **Customizable Watermarks**: Add a settings option to allow users to customize or disable the server-side watermarking.
7.  **Image Quality Settings**: Add a setting to choose the upload quality (e.g., High, Medium, Low) to help manage data usage, which can be passed to the server during upload.
8.  **UI/UX Polish**: While functional, the UI could benefit from a design pass to improve button placement, iconography, and overall visual appeal, as suggested by the developer.
