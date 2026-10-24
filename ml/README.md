# OpenCV + CNN Hybrid OMR

This directory contains an experimental machine learning pipeline to classify OMR bubble marks. It compares a pure OpenCV baseline against a hybrid OpenCV + CNN approach.

## Dataset
The dataset contains 145 images of OMR sheets and `OMR_Ans.csv` with the ground truth of student answers. 
- Image format: JPEG, resized to 1024x556.
- The dataset lacks standard paper boundaries/anchors, causing traditional 4-point perspective warp algorithms to fail.

## Setup
```bash
pip install -r requirements.txt
```

## Running the Pipeline

1. **Inspect Dataset**
```bash
python inspect_dataset.py
```

2. **Run OpenCV Baseline**
```bash
python opencv_baseline.py
```
*Evaluates the standard OpenCV approach (similar to the Kotlin Android implementation). Yields 1.29% accuracy due to localization failures.*

3. **Extract Crops and Create Splits**
```bash
python extract_bubbles.py
```
*Uses K-Means clustering to localize bubbles and extract 32x32 crops. Generates `splits.json`.*

4. **Train CNN**
```bash
python train_cnn.py
```
*Trains a small PyTorch CNN to classify crops as unmarked (0) or marked (1).*

5. **Evaluate Hybrid Pipeline**
```bash
python evaluate_hybrid.py
```
*Evaluates the CNN on the localized test set.*

## Results
- **OpenCV Baseline Accuracy:** 1.29%
- **Localization Success Rate:** 15.17% (22/145 images)
- **CNN Crop Accuracy:** 86.46% (Test Set)
