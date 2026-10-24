import os
import cv2
import numpy as np
import pandas as pd
import glob
from tqdm import tqdm
import json
import random

def extract_bubbles_from_image(img_path):
    img = cv2.imread(img_path)
    if img is None:
        return None, None

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    circles = cv2.HoughCircles(
        gray, cv2.HOUGH_GRADIENT, dp=1.2, minDist=20,
        param1=50, param2=30, minRadius=8, maxRadius=25
    )

    if circles is None:
        return None, None

    circles = np.round(circles[0, :]).astype("int")

    # K-means clustering on X to find 12 columns
    xs = np.array([c[0] for c in circles], dtype=np.float32)

    if len(xs) < 100:
        return None, None

    criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 100, 0.2)
    _, x_labels, x_centers = cv2.kmeans(xs, 12, None, criteria, 10, cv2.KMEANS_RANDOM_CENTERS)

    x_centers = x_centers.flatten()
    sorted_x_centers = np.sort(x_centers)

    if np.any(np.diff(sorted_x_centers) < 20):
        return None, None

    # K-means clustering on Y to find 10 rows
    ys = np.array([c[1] for c in circles], dtype=np.float32)
    _, y_labels, y_centers = cv2.kmeans(ys, 10, None, criteria, 10, cv2.KMEANS_RANDOM_CENTERS)

    y_centers = y_centers.flatten()
    sorted_y_centers = np.sort(y_centers)

    if np.any(np.diff(sorted_y_centers) < 20):
        return None, None

    grid = []
    for i in range(12):
        col_points = []
        for j in range(10):
            cx = sorted_x_centers[i]
            cy = sorted_y_centers[j]

            # Find closest circle
            dists = [np.hypot(c[0] - cx, c[1] - cy) for c in circles]
            min_dist = min(dists) if dists else 1000

            if min_dist < 15:
                closest = circles[np.argmin(dists)]
                col_points.append(closest)
            else:
                col_points.append(np.array([int(cx), int(cy), 15]))
        grid.append(col_points)

    return grid, gray

if __name__ == "__main__":
    dataset_path = r"D:\OMRScannerApp\ml_dataset"
    image_dir = os.path.join(dataset_path, "OMR_Image_Resized")
    csv_path = os.path.join(dataset_path, "OMR_Ans.csv")
    crops_dir = os.path.join(dataset_path, "crops")
    os.makedirs(crops_dir, exist_ok=True)

    df = pd.read_csv(csv_path)
    df['Image_ID'] = df['Image_ID'].astype(str).str.replace(r'\.0$', '', regex=True)

    mapping = {'A': 0, 'B': 1, 'C': 2, 'D': 3}

    images = glob.glob(os.path.join(image_dir, "*.jpg"))

    # We want to create reproducible dataset splits based on sheet ID
    np.random.seed(42)
    random.seed(42)

    valid_images = []

    # Check which images can be processed
    for img_path in tqdm(images, desc="Extracting bubbles"):
        img_id = os.path.splitext(os.path.basename(img_path))[0]
        row = df[df['Image_ID'] == img_id]
        if row.empty:
            continue

        grid, gray = extract_bubbles_from_image(img_path)
        if grid is None:
            continue

        expected_ans = row.iloc[0, 1:31].tolist()

        # Save crops
        # grid has 12 columns, 10 rows
        # Questions 1-10: cols 0-3, rows 0-9
        # Questions 11-20: cols 4-7, rows 0-9
        # Questions 21-30: cols 8-11, rows 0-9

        valid_images.append(img_id)

        for q in range(30):
            ans = expected_ans[q]
            ans_idx = mapping.get(ans, -1)

            q_col_group = q // 10
            q_row = q % 10

            for opt in range(4):
                col_idx = q_col_group * 4 + opt
                circle = grid[col_idx][q_row]

                x, y, r = circle
                # crop 32x32 around x, y
                size = 16
                x1, x2 = max(0, x - size), min(gray.shape[1], x + size)
                y1, y2 = max(0, y - size), min(gray.shape[0], y + size)

                crop = gray[y1:y2, x1:x2]
                if crop.shape != (32, 32):
                    crop = cv2.resize(crop, (32, 32))

                is_marked = 1 if opt == ans_idx else 0
                crop_name = f"{img_id}_q{q+1}_opt{opt}_m{is_marked}.jpg"
                cv2.imwrite(os.path.join(crops_dir, crop_name), crop)

    print(f"Successfully extracted crops for {len(valid_images)} images.")

    # Create splits
    random.shuffle(valid_images)
    num_imgs = len(valid_images)
    train_end = int(num_imgs * 0.7)
    val_end = int(num_imgs * 0.85)

    train_ids = valid_images[:train_end]
    val_ids = valid_images[train_end:val_end]
    test_ids = valid_images[val_end:]

    splits = {
        'train': train_ids,
        'val': val_ids,
        'test': test_ids
    }

    with open(os.path.join(dataset_path, "splits.json"), "w") as f:
        json.dump(splits, f)

    print(f"Created splits: {len(train_ids)} train, {len(val_ids)} val, {len(test_ids)} test.")
