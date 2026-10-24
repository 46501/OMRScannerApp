import os
import cv2
import numpy as np
import pandas as pd
import glob
from tqdm import tqdm

def process_omr_sheet(img_path, expected_questions=30, options_per_question=4):
    img = cv2.imread(img_path)
    if img is None:
        return None, "Failed to load image"

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edged = cv2.Canny(blurred, 75, 200)

    contours, hierarchy = cv2.findContours(edged, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        return None, "No contours found"

    contours = sorted(contours, key=cv2.contourArea, reverse=True)
    paper_contour = None
    for c in contours:
        peri = cv2.arcLength(c, True)
        approx = cv2.approxPolyDP(c, 0.02 * peri, True)
        if len(approx) == 4:
            paper_contour = approx
            break

    warped = gray
    if paper_contour is not None:
        # Sort points: top-left, top-right, bottom-right, bottom-left
        pts = paper_contour.reshape(4, 2)
        s = pts.sum(axis=1)
        diff = np.diff(pts, axis=1)
        tl = pts[np.argmin(s)]
        br = pts[np.argmax(s)]
        tr = pts[np.argmin(diff)]
        bl = pts[np.argmax(diff)]

        widthA = np.linalg.norm(br - bl)
        widthB = np.linalg.norm(tr - tl)
        maxWidth = max(int(widthA), int(widthB))

        heightA = np.linalg.norm(tr - br)
        heightB = np.linalg.norm(tl - bl)
        maxHeight = max(int(heightA), int(heightB))

        src = np.array([tl, tr, br, bl], dtype="float32")
        dst = np.array([
            [0, 0],
            [maxWidth - 1, 0],
            [maxWidth - 1, maxHeight - 1],
            [0, maxHeight - 1]], dtype="float32")

        M = cv2.getPerspectiveTransform(src, dst)
        warped = cv2.warpPerspective(gray, M, (maxWidth, maxHeight))

    thresh = cv2.adaptiveThreshold(warped, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY_INV, 21, 10.0)

    bubble_contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    questions = []
    for c in bubble_contours:
        x, y, w, h = cv2.boundingRect(c)
        ar = w / float(h)
        perimeter = cv2.arcLength(c, True)
        area = cv2.contourArea(c)
        circularity = 0.0 if perimeter == 0 else (4 * np.pi * area) / (perimeter * perimeter)

        # Keep limits close to Kotlin: width>=12, height>=12, ar 0.6-1.4, circularity>=0.6
        if w >= 12 and h >= 12 and 0.6 <= ar <= 1.4 and circularity >= 0.6:
            questions.append(c)

    if not questions:
        return [], "No bubbles found"

    # Group by X coordinate into columns
    questions = sorted(questions, key=lambda c: cv2.boundingRect(c)[0])
    columns = []
    if questions:
        current_column = [questions[0]]
        previous_x = cv2.boundingRect(questions[0])[0]

        widths = sorted([cv2.boundingRect(c)[2] for c in questions])
        median_width = widths[len(widths)//2] if widths else 20
        column_gap_threshold = median_width * 2.5

        for i in range(1, len(questions)):
            current_x = cv2.boundingRect(questions[i])[0]
            if current_x - previous_x > column_gap_threshold:
                columns.append(current_column)
                current_column = [questions[i]]
            else:
                current_column.append(questions[i])
            previous_x = current_x
        columns.append(current_column)

    columns.sort(key=lambda col: np.mean([cv2.boundingRect(c)[0] for c in col]))

    detected_answers = []
    for col in columns:
        col = sorted(col, key=lambda c: cv2.boundingRect(c)[1])
        rows = []
        if col:
            current_row = [col[0]]
            prev_y = cv2.boundingRect(col[0])[1]
            for i in range(1, len(col)):
                current_y = cv2.boundingRect(col[i])[1]
                if current_y - prev_y > 15:
                    rows.append(current_row)
                    current_row = [col[i]]
                else:
                    current_row.append(col[i])
                prev_y = current_y
            rows.append(current_row)

        for row_bubbles in rows:
            if len(detected_answers) >= expected_questions:
                break

            if len(row_bubbles) != options_per_question:
                detected_answers.append(-1)
                continue

            row_bubbles = sorted(row_bubbles, key=lambda c: cv2.boundingRect(c)[0])
            fill_values = []
            for j in range(options_per_question):
                mask = np.zeros(thresh.shape, dtype="uint8")
                cv2.drawContours(mask, [row_bubbles[j]], -1, 255, -1)
                mask_mean = cv2.mean(thresh, mask=mask)[0]
                fill_values.append(mask_mean)

            max_fill = max(fill_values)
            min_fill_threshold = 100.0
            relative_fill_ratio = 0.8

            if max_fill > min_fill_threshold:
                relative_threshold = max(max_fill * relative_fill_ratio, min_fill_threshold)
                marked_count = sum(1 for v in fill_values if v >= relative_threshold)
                selected_index = [idx for idx, v in enumerate(fill_values) if v >= relative_threshold]

                if marked_count == 1:
                    detected_answers.append(selected_index[0])
                else:
                    detected_answers.append(-2)
            else:
                detected_answers.append(-1)

    while len(detected_answers) < expected_questions:
        detected_answers.append(-1)

    return detected_answers, None

if __name__ == "__main__":
    dataset_path = r"D:\OMRScannerApp\ml_dataset"
    image_dir = os.path.join(dataset_path, "OMR_Image_Resized")
    csv_path = os.path.join(dataset_path, "OMR_Ans.csv")

    df = pd.read_csv(csv_path)
    df['Image_ID'] = df['Image_ID'].astype(str).str.replace(r'\.0$', '', regex=True)

    mapping = {'A': 0, 'B': 1, 'C': 2, 'D': 3}

    total_questions = 0
    correct_predictions = 0

    images = glob.glob(os.path.join(image_dir, "*.jpg"))
    for img_path in tqdm(images):
        img_id = os.path.splitext(os.path.basename(img_path))[0]
        row = df[df['Image_ID'] == img_id]
        if row.empty:
            continue

        expected_ans = row.iloc[0, 1:31].tolist()
        expected_indices = [mapping.get(ans, -1) for ans in expected_ans]

        detected, err = process_omr_sheet(img_path, expected_questions=30, options_per_question=4)
        if not detected:
            total_questions += 30
            continue

        for d, e in zip(detected, expected_indices):
            total_questions += 1
            if d == e:
                correct_predictions += 1

    print(f"\nOpenCV Baseline Question Accuracy: {correct_predictions / total_questions * 100:.2f}%")
