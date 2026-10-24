import cv2
import numpy as np
import glob
import os

def debug_bubbles(img_path):
    img = cv2.imread(img_path)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    circles = cv2.HoughCircles(
        gray, cv2.HOUGH_GRADIENT, dp=1.2, minDist=20,
        param1=50, param2=30, minRadius=5, maxRadius=30
    )
    if circles is None:
        return []
    return np.round(circles[0, :]).astype("int")

dataset_path = r"D:\OMRScannerApp\ml_dataset"
image_dir = os.path.join(dataset_path, "OMR_Image_Resized")
images = glob.glob(os.path.join(image_dir, "*.jpg"))[:5]
for img in images:
    circles = debug_bubbles(img)
    xs = sorted([c[0] for c in circles])
    print(f"{os.path.basename(img)}: {len(circles)} circles. Min X: {xs[0] if xs else 'None'}, Max X: {xs[-1] if xs else 'None'}")
