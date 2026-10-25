from fastapi.testclient import TestClient
from main import app
import os
import pytest

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert "status" in response.json()

def test_ask_theory_no_key(monkeypatch):
    # Ensure API key is missing
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)

    response = client.post("/api/theory/ask", json={
        "question": "What is gravity?",
        "subject": "Physics",
        "detail_level": "short"
    })

    assert response.status_code == 500
    assert "not configured" in response.json()["detail"]

def test_ask_theory_invalid_input():
    # detail_level not allowed
    response = client.post("/api/theory/ask", json={
        "question": "What is gravity?",
        "subject": "Physics",
        "detail_level": "super_long"
    })
    assert response.status_code == 422 # Validation error

def test_ask_theory_empty_question():
    response = client.post("/api/theory/ask", json={
        "question": "",
        "subject": "Physics",
        "detail_level": "short"
    })
    assert response.status_code == 422

# Testing a mocked successful response
def test_ask_theory_mocked_success(monkeypatch):
    monkeypatch.setenv("GEMINI_API_KEY", "fake_key")

    class MockResponse:
        text = "Mocked answer"

    class MockModels:
        def generate_content(self, *args, **kwargs):
            return MockResponse()

    class MockClient:
        def __init__(self, *args, **kwargs):
            self.models = MockModels()

    # We need to mock the genai.Client
    import main
    monkeypatch.setattr(main.genai, "Client", MockClient)

    response = client.post("/api/theory/ask", json={
        "question": "What is a mock?",
        "subject": "Software",
        "detail_level": "short"
    })

    assert response.status_code == 200
    data = response.json()
    assert data["answer"] == "Mocked answer"
    assert "gemini-2.5-flash" in data["model"]
