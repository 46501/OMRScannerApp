import cv2
import numpy as np

def debug_omr(img_path):
    img = cv2.imread(img_path)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edged = cv2.Canny(blurred, 75, 200)

    contours, hierarchy = cv2.findContours(edged, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)
    paper_contour = None
    for c in contours:
        peri = cv2.arcLength(c, True)
        approx = cv2.approxPolyDP(c, 0.02 * peri, True)
        if len(approx) == 4:
            paper_contour = approx
            break

    cv2.drawContours(img, [paper_contour] if paper_contour is not None else [], -1, (0, 255, 0), 2)

    cv2.imwrite("debug_paper.jpg", img)
    print("Saved debug_paper.jpg")

debug_omr(r'D:\OMRScannerApp\ml_dataset\OMR_Image_Resized\1.jpg')
