import os
import glob
import json
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from torchvision import transforms
from PIL import Image
import numpy as np

class OMRBubbleDataset(Dataset):
    def __init__(self, crops_dir, img_ids, transform=None):
        self.crops_dir = crops_dir
        self.img_ids = set(img_ids)
        self.transform = transform

        self.samples = []
        all_files = glob.glob(os.path.join(crops_dir, "*.jpg"))
        for f in all_files:
            basename = os.path.basename(f)
            # Format: img_id_q1_opt0_m1.jpg
            parts = basename.split('_')
            img_id = parts[0]
            if img_id in self.img_ids:
                label_str = parts[-1].replace('.jpg', '').replace('m', '')
                label = int(label_str)
                self.samples.append((f, label))

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        path, label = self.samples[idx]
        img = Image.open(path).convert('L') # grayscale
        if self.transform:
            img = self.transform(img)
        return img, label

class SimpleCNN(nn.Module):
    def __init__(self):
        super(SimpleCNN, self).__init__()
        self.features = nn.Sequential(
            nn.Conv2d(1, 16, kernel_size=3, padding=1),
            nn.ReLU(),
            nn.MaxPool2d(2),

            nn.Conv2d(16, 32, kernel_size=3, padding=1),
            nn.ReLU(),
            nn.MaxPool2d(2),
        )
        self.classifier = nn.Sequential(
            nn.Linear(32 * 8 * 8, 128),
            nn.ReLU(),
            nn.Dropout(0.5),
            nn.Linear(128, 2)
        )

    def forward(self, x):
        x = self.features(x)
        x = torch.flatten(x, 1)
        x = self.classifier(x)
        return x

def train():
    dataset_path = r"D:\OMRScannerApp\ml_dataset"
    crops_dir = os.path.join(dataset_path, "crops")
    splits_path = os.path.join(dataset_path, "splits.json")

    with open(splits_path, 'r') as f:
        splits = json.load(f)

    transform = transforms.Compose([
        transforms.ToTensor(),
        transforms.Normalize((0.5,), (0.5,))
    ])

    train_dataset = OMRBubbleDataset(crops_dir, splits['train'], transform)
    val_dataset = OMRBubbleDataset(crops_dir, splits['val'], transform)

    train_loader = DataLoader(train_dataset, batch_size=32, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=32, shuffle=False)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Using device: {device}")

    model = SimpleCNN().to(device)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    best_val_acc = 0.0

    for epoch in range(5):
        model.train()
        train_loss = 0.0
        train_correct = 0

        for inputs, labels in train_loader:
            inputs, labels = inputs.to(device), labels.to(device)

            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()

            train_loss += loss.item() * inputs.size(0)
            _, preds = torch.max(outputs, 1)
            train_correct += torch.sum(preds == labels.data)

        train_loss = train_loss / len(train_dataset)
        train_acc = train_correct.double() / len(train_dataset)

        model.eval()
        val_loss = 0.0
        val_correct = 0
        with torch.no_grad():
            for inputs, labels in val_loader:
                inputs, labels = inputs.to(device), labels.to(device)
                outputs = model(inputs)
                loss = criterion(outputs, labels)
                val_loss += loss.item() * inputs.size(0)
                _, preds = torch.max(outputs, 1)
                val_correct += torch.sum(preds == labels.data)

        val_loss = val_loss / len(val_dataset)
        val_acc = val_correct.double() / len(val_dataset)

        print(f"Epoch {epoch+1}/5 - Train Loss: {train_loss:.4f} Acc: {train_acc:.4f} | Val Loss: {val_loss:.4f} Acc: {val_acc:.4f}")

        if val_acc > best_val_acc:
            best_val_acc = val_acc
            torch.save(model.state_dict(), os.path.join(r"D:\OMRScannerApp\ml", "best_model.pth"))

if __name__ == "__main__":
    train()
