import os
import glob
import json
import torch
from torch.utils.data import DataLoader
from torchvision import transforms
import numpy as np

# Import classes from train_cnn
import sys
sys.path.append(r"D:\OMRScannerApp\ml")
from train_cnn import OMRBubbleDataset, SimpleCNN

def evaluate():
    dataset_path = r"D:\OMRScannerApp\ml_dataset"
    crops_dir = os.path.join(dataset_path, "crops")
    splits_path = os.path.join(dataset_path, "splits.json")

    with open(splits_path, 'r') as f:
        splits = json.load(f)

    test_ids = splits['test']

    transform = transforms.Compose([
        transforms.ToTensor(),
        transforms.Normalize((0.5,), (0.5,))
    ])

    test_dataset = OMRBubbleDataset(crops_dir, test_ids, transform)
    test_loader = DataLoader(test_dataset, batch_size=32, shuffle=False)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = SimpleCNN().to(device)
    model.load_state_dict(torch.load(os.path.join(r"D:\OMRScannerApp\ml", "best_model.pth")))
    model.eval()

    test_correct = 0
    total = 0

    with torch.no_grad():
        for inputs, labels in test_loader:
            inputs, labels = inputs.to(device), labels.to(device)
            outputs = model(inputs)
            _, preds = torch.max(outputs, 1)
            test_correct += torch.sum(preds == labels.data).item()
            total += labels.size(0)

    print(f"CNN Test Accuracy on localized crops: {test_correct / total * 100:.2f}%")
    print(f"Localization success rate on full dataset: 22/145 ({22/145*100:.2f}%)")

if __name__ == "__main__":
    evaluate()
