# QPhotos - Complete User Guide

## 🚀 Quick Start Guide

**QPhotos** is your professional field photography companion. This guide will help you get started quickly and make the most of all features.

## 📱 What is QPhotos?

**QPhotos replaces our old WhatsApp photo sharing system.** Instead of photos getting lost in chat groups, QPhotos automatically organizes all field photos by project and date on our company server.

**Why we built this:**
- WhatsApp photos were disorganized and hard to find
- No professional documentation standards
- Photos would get lost in chat history
- Difficult to locate specific project photos when needed

**What QPhotos does:**
- Automatically organizes photos by project and date
- Adds professional watermarks with project info and timestamps
- Stores everything safely on our company server
- Makes it easy to find any photo from any project

## 🔧 Initial Setup

### Step 1: Start the Server
1. On your computer, run the Python server by double-clicking `server.py` or running:
   ```
   python server.py
   ```
2. You'll see: "Servidor de producción iniciado en http://0.0.0.0:5000"
3. Note your computer's IP address (e.g., 192.168.1.100)

### Step 2: Configure the App
1. Open QPhotos on your Android device
2. Tap the **Settings** icon (⚙️) on the main screen
3. Enter your computer's IP address in the "Server IP Address" field
4. The setting saves automatically

**Important**: Your phone and computer must be on the same Wi-Fi network!

## 📸 Taking and Uploading Photos

### Setting Your Project Name
Before taking photos, you need to set a project name. You have three convenient options:

1. **Type a New Project**: Tap the "Project Name" field and type your project name
2. **Use Last Project**: Tap the project name shown in the top-right corner to reuse it instantly
3. **Select from Dropdown**: Tap the dropdown arrow (▼) to see recent projects from this month

### Capturing Photos

#### Option 1: Camera
1. Aim your device at your subject
2. Tap the large **camera button** (⭕) at the bottom
3. You'll hear a confirmation sound
4. The photo is automatically added to the upload queue

#### Option 2: Gallery Import
1. Tap the **gallery icon** (🖼️) to the right of the camera button
2. Select one or multiple photos from your device
3. Tap "Done" to add them to the upload queue

### Understanding the Upload Process
- Photos are uploaded automatically in the background
- The main screen shows "X Photos in Queue" 
- You can continue taking photos while others upload
- Photos are organized automatically by Month → Project → Date

## 📁 Managing Your Photos

### Viewing Upload Queue
1. Tap the "X Photos in Queue" text on the main screen
2. See all pending uploads with their status:
   - **Pending**: Waiting to upload
   - **Uploading**: Currently being sent
   - **Failed**: Upload failed (will retry automatically)

### Browsing Uploaded Photos
1. Tap the **folder icon** (📁) on the main screen
2. Navigate through your organized photos:
   - **Months**: January, February, etc.
   - **Projects**: Your project names
   - **Dates**: Specific days (YYYY-MM-DD format)
   - **Photos**: Individual images

### Viewing Photos
1. Navigate to a specific date folder
2. Tap any photo to open the full-screen viewer
3. In the viewer:
   - **Pinch to zoom** in/out
   - **Swipe left/right** to see other photos
   - **Tap once** to show/hide controls
   - **Back button** to return to gallery

### Managing Projects and Folders
You can rename or delete any folder (projects, subfolders, etc.):

1. **Long-press** (tap and hold) on any folder name
2. Choose from the menu:
   - **Rename**: Give the folder a new name
   - **Delete**: Remove the folder and all its contents

**Warning**: Deleting a folder removes all photos inside it permanently!

## ⚡ Pro Tips for Efficient Use

### Speed Up Your Workflow
- **Use the Last Project button** for repeated work on the same project
- **Take multiple photos quickly** - they'll queue up automatically
- **Don't wait for uploads** - continue working while photos upload in background

### Project Naming Best Practices
- Use clear, descriptive names: "Building_Inspection_Site_A"
- Avoid special characters: stick to letters, numbers, and underscores
- Be consistent with your naming convention

### Network Tips
- **Stay on the same Wi-Fi network** as your server
- **Check your connection** if uploads seem slow
- **Photos are saved locally** until they can be uploaded

## 🔍 Understanding the Interface

### Main Screen Elements
- **Project Name Field**: Where you enter/select your current project
- **Last Project Button**: Quick access to your most recent project (top-right)
- **Settings Icon** (⚙️): Configure server connection
- **Folder Icon** (📁): Browse uploaded photos
- **Camera Button** (⭕): Take a photo
- **Gallery Icon** (🖼️): Import from device gallery
- **Queue Status**: Shows pending uploads count

### Photo Organization
Your photos are automatically organized as:
```
📁 01 JANUARY
  📁 My_Project_Name
    📁 2025-01-15
      📷 photo1.jpg (with watermark)
      📷 photo2.jpg (with watermark)
    📁 2025-01-16
      📷 photo3.jpg (with watermark)
```

### Automatic Features
- **Watermarks**: Every photo gets a watermark with project name and timestamp
- **Date Organization**: Photos are grouped by the date they were taken
- **Duplicate Prevention**: Same photo won't be uploaded twice
- **Background Uploads**: Continue working while photos upload

## 🛠️ Troubleshooting

### "Cannot Connect to Server"
- Check that the server is running on your computer
- Verify you're on the same Wi-Fi network
- Double-check the IP address in Settings
- Try restarting both the app and server

### "Photos Not Uploading"
- Check your Wi-Fi connection
- Ensure the server is still running
- Photos will retry automatically when connection is restored
- Check the Queue screen for specific error messages

### "Camera Not Working"
- Grant camera permission when prompted
- Restart the app if camera appears black
- Check that no other app is using the camera

### "Cannot See My Photos"
- Ensure photos have finished uploading (check queue)
- Refresh the Projects screen by going back and entering again
- Check that you're looking in the correct month/project folder

## 📋 Feature Summary

### What QPhotos Does (Replaces WhatsApp)
✅ **Organizes photos properly** - No more hunting through WhatsApp chats  
✅ **Professional documentation** - Watermarks with project name and date  
✅ **Easy to find photos** - Browse by project and date, not chat history  
✅ **Reliable storage** - Photos saved on company server, not lost in messages  
✅ **Simple to use** - Just select project and take photos  
✅ **Works in English and Spanish** - For our diverse team  
✅ **Secure** - Photos stay on our company network  

### What QPhotos Doesn't Do
❌ Replace WhatsApp for messaging (just for photo organization)  
❌ Upload to external cloud services (company data stays internal)  
❌ Edit or filter photos (focuses on documentation)  
❌ Record videos (photos only for now)  

## 🎯 Best Practices for Field Work

1. **Start each day** by setting your project name once
2. **Take photos freely** - the app handles organization
3. **Check the queue periodically** to ensure uploads are progressing
4. **Use descriptive project names** for easy identification later
5. **Keep your device charged** for extended field work
6. **Ensure stable Wi-Fi** at your base location for uploads

---

**Need Help?** This guide covers all major features. For technical issues, contact your system administrator or IT support.

**Version**: 1.0 - Complete User Guide  
**Last Updated**: January 2025