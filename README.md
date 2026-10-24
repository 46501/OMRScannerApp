# OMR Scanner App

A fully functional Optical Mark Recognition (OMR) scanner application for Android, built with Kotlin, Jetpack Compose, and OpenCV.

## Overview
This application empowers teachers and educators to instantly grade multiple-choice question (MCQ) exams using a smartphone camera. It dynamically utilizes OpenCV to extract contours, evaluate geometric boundaries, and calculate pixel density to determine which bubbles a student has filled in on an OMR sheet.

### Implemented Features
*   **Modern Jetpack Compose UI**: A beautiful, fully interactive dashboard, answer-key creator, dynamic answer review editor, and score breakdown.
*   **Live CameraX Capture**: Integrated native camera handling to scan and evaluate sheets right from the phone.
*   **Dynamic OpenCV Processing**:
    *   Adaptive Gaussian Thresholding (handles shadows and uneven lighting).
    *   Perspective Geometry Warping (handles slightly angled camera shots).
    *   Relative Fill Density (dynamically evaluates the darkest bubble against neighboring choices).
*   **Custom Answer Keys**: Create, configure (4-option or 5-option), and persistently save answer keys locally via JSON architecture.
*   **Grading Engine**: Completely native logic to calculate Correct, Incorrect, Unanswered, and Multiple-Marked answers. Supports custom Correct Marks and Negative Penalties per exam.
*   **Manual Review**: Intercepts the OpenCV output and allows the user to manually verify and fix erroneously detected (or smudged) answers before committing the score to the database.

### Planned Features (Future)
*   Subjective / Theory Answer evaluation using Gemini AI Vision.
*   Exporting evaluation metrics to CSV.
*   Complex multi-column physical OMR sheet calibration profiles.

## Experimental ML Research
This repository contains an isolated `ml/` directory representing an OpenCV + Convolutional Neural Network (CNN) research experiment. 
- **Findings**: The OpenCV algorithm's bottleneck is localized bounding-box perspective alignment (finding the paper edges). If the geometry fails, the CNN never receives isolated bubbles to evaluate. Thus, the production app relies exclusively on OpenCV mapping against the strictly enforced Standardized PDF generator limits rather than integrating a heavy TensorFlow/PyTorch CNN dependency.

## Prerequisites & Setup

### 1. OpenCV Android SDK
This project relies on native OpenCV bindings. To keep the GitHub repository clean and portable, the heavy OpenCV SDK binary is *not* included in this repository. 

You must download the SDK and link it to your local environment.
1. Download **OpenCV-4.14.0-android-sdk** from the official [OpenCV Releases Page](https://opencv.org/releases/).
2. Extract the archive to a safe location on your machine (e.g., `D:/opencv-4.14.0-android-sdk/`).
3. In the root directory of this Android project, create a file named `local.properties` (if it does not exist).
4. Add the following line to point to the `sdk` folder inside the extracted OpenCV directory:
   ```properties
   opencv.dir=D\:/opencv-4.14.0-android-sdk/OpenCV-android-sdk/sdk
   ```
   *(Note: Use forward slashes or escape backslashes depending on your OS).*

### 2. Building the Project
Open the project in **Android Studio** (Koala or newer recommended).
Ensure your Android SDK has NDK version `26.1.10909125` installed (required by OpenCV native bindings).

You can build the project via the terminal:
```bash
# Debug Build
gradlew assembleDebug

# Release Build (Requires signing configuration)
gradlew assembleRelease
```

### 3. Running Tests
The project contains isolated Unit tests and synthetic Android Instrumentation tests that validate the grading engine and the geometric OpenCV constraints without requiring a physical camera.

```bash
# Run grading logic tests
gradlew testDebugUnitTest

# Run on-device OpenCV synthetic bitmap tests (Requires Emulator/Device)
gradlew connectedAndroidTest
```

## Known Limitations
*   **Template Structure**: The current OpenCV extraction logic relies on a strictly rendered 4-point bounding box with highly normalized columns. Arbitrary complex layouts (e.g., zig-zag bubbles or missing anchor boxes) are not supported. It is strongly recommended to use the built-in **Print** functionality on the `KeysScreen` to generate fully compatible, standardized OMR PDFs for physical scanning.
*   **Physical Calibration**: The algorithm uses a relative density check with an absolute floor of `100.0`. Scanning under extreme shadows, crinkled paper, or colored paper stock may result in false negatives. Future versions will implement a UI-based calibration slider.

## Manual Physical Testing Checklist
Before enterprise deployment, the following physical tests must be manually completed since automated environments rely on synthetic bitmap rendering:
- [ ] Print a standard 50-Question Answer Key PDF at **100% Scale**.
- [ ] Correctly fill standard sequences (Q1=A, Q2=B, etc).
- [ ] Intentionally smudge/erase a bubble to test adaptive noise handling.
- [ ] Test in heavy indoor fluorescent lighting.
- [ ] Test in harsh outdoor natural lighting.
- [ ] Confirm no geometry warps occur and the Review screen matches physical ink natively.
