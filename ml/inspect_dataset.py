import os
import pandas as pd
import glob

dataset_path = r"C:\Users\Admin\Downloads\archive"
image_dir = os.path.join(dataset_path, "OMR_Image_Resized")
csv_path = os.path.join(dataset_path, "OMR_Ans.csv")

images = glob.glob(os.path.join(image_dir, "*"))
print(f"Number of images in dir: {len(images)}")
if images:
    print(f"Sample image names: {[os.path.basename(p) for p in images[:5]]}")

df = pd.read_csv(csv_path)
print(f"CSV shape: {df.shape}")
print(f"CSV columns: {df.columns.tolist()}")
print(f"Sample CSV rows:")
print(df.head())

# Check how many images match CSV
# Convert float string '1.0' to '1' where necessary
csv_ids = df['Image_ID'].astype(str).str.replace(r'\.0$', '', regex=True).tolist()
img_ids = [os.path.splitext(os.path.basename(p))[0] for p in images]

matches = set(csv_ids).intersection(set(img_ids))
print(f"Number of matching records: {len(matches)}")

missing_in_csv = set(img_ids) - set(csv_ids)
missing_in_imgs = set(csv_ids) - set(img_ids)
print(f"Images without CSV row: {len(missing_in_csv)}")
print(f"CSV rows without Image: {len(missing_in_imgs)}")
