# QPhotos User Guide

Welcome to QPhotos! This guide will walk you through the key features of the app and help you get started with organizing your field photography.

## Getting Started

### 1. Setting Up the Server Connection
Before you can upload photos, you need to connect the app to your local server.
-   Open the app and go to the **Settings** screen.
-   Enter the IP address of the computer running the QPhotos server.
-   Save the settings. The app is now ready to communicate with your server.

### 2. Taking Your First Photo
-   From the main screen, tap the **Camera** button.
-   Enter a **Project Name** for the photos you are about to take. The app will remember this name for future photos in the same session.
-   Use the camera interface to take your photos. You can control the flash using the icons at the top.
-   When you take a photo, it is automatically added to the **Upload Queue**.

## Core Features

### The File Explorer
The main screen of the app is a powerful file explorer that helps you navigate through all your projects.
-   **Months**: The top level shows folders for each month, with the most recent month at the top.
-   **Projects**: Tap on a month to see all the projects you worked on during that time. If you have nested projects (e.g., a "Florida" sub-project within a main "USA" project), you can navigate into them just like regular folders.
-   **Daily Folders**: Inside each project, you will find folders for each day you took photos, named with the date (e.g., `2025-10-04`).

### Photo Gallery
-   When you tap on a **daily folder**, the app will open a grid view showing all the photos taken on that day.
-   Tap on any photo to view it in a full-screen gallery.
-   **Gestures**:
    -   **Pinch** to zoom in and out.
    -   **Swipe** left and right to navigate between photos.
    -   **Swipe down** to dismiss the gallery and return to the grid view.
-   **Deleting Photos**: While viewing a photo full-screen, tap the **trash can icon** in the corner to delete it. You will be asked to confirm before the photo is permanently removed from the server.

### The Upload Queue
-   The **Queue** screen shows you a list of all photos that are waiting to be uploaded to the server.
-   Uploads happen automatically in the background, so you can continue working even if your network connection is unstable. `WorkManager` ensures the uploads will proceed whenever a connection is available.
-   Once a photo is successfully uploaded, it will disappear from the queue.

We hope this guide helps you make the most of QPhotos!