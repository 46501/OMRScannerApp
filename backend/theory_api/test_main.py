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

def test_extract_questions_no_key(monkeypatch):
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    # mock a file upload
    files = {"file": ("test.jpg", b"fake image bytes", "image/jpeg")}
    response = client.post("/api/theory/extract_questions", files=files)
    assert response.status_code == 500

def test_evaluate_answers_no_key(monkeypatch):
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    files = [("files", ("test.jpg", b"fake image bytes", "image/jpeg"))]
    data = {"questions_json": '[{"question_number": 1, "question_text": "Q", "max_marks": 1}]'}
    response = client.post("/api/theory/evaluate", data=data, files=files)
    assert response.status_code == 500

def test_evaluate_answers_invalid_json(monkeypatch):
    monkeypatch.setenv("GEMINI_API_KEY", "fake")
    files = [("files", ("test.jpg", b"fake image bytes", "image/jpeg"))]
    data = {"questions_json": 'invalid'}
    response = client.post("/api/theory/evaluate", data=data, files=files)
    assert response.status_code == 400

def test_extract_questions_mocked(monkeypatch):
    monkeypatch.setenv("GEMINI_API_KEY", "fake")
    class MockResponse:
        text = '{"questions": [{"question_number": 1, "question_text": "Test?", "max_marks": 5.0}]}'

    class MockModels:
        def generate_content(self, *args, **kwargs):
            return MockResponse()

    class MockClient:
        def __init__(self, *args, **kwargs):
            self.models = MockModels()

    import main
    monkeypatch.setattr(main.genai, "Client", MockClient)

    files = {"file": ("test.jpg", b"fake", "image/jpeg")}
    response = client.post("/api/theory/extract_questions", files=files)

    assert response.status_code == 200
    assert response.json()["questions"][0]["question_text"] == "Test?"

def test_evaluate_answers_mocked(monkeypatch):
    monkeypatch.setenv("GEMINI_API_KEY", "fake")
    class MockResponse:
        text = '{"total_marks_obtained": 0.0, "max_possible_marks": 0.0, "percentage": 0.0, "strengths": "S", "weaknesses": "W", "common_mistakes": "C", "recommendations": "R", "requires_manual_review": false, "question_evaluations": [{"question_number": 1, "question_text": "Q1", "max_marks": 5.0, "marks_awarded": 4.0, "student_answer_transcription": "T", "feedback": "F", "correct_concepts": "C", "missing_concepts": "M", "suggested_improvements": "S", "model_answer": "M", "needs_manual_review": false}]}'

    class MockModels:
        def generate_content(self, *args, **kwargs):
            return MockResponse()

    class MockClient:
        def __init__(self, *args, **kwargs):
            self.models = MockModels()

    import main
    monkeypatch.setattr(main.genai, "Client", MockClient)

    files = [("files", ("test.jpg", b"fake image bytes", "image/jpeg"))]
    data = {"questions_json": '[{"question_number": 1, "question_text": "Q", "max_marks": 1}]'}
    response = client.post("/api/theory/evaluate", data=data, files=files)

    assert response.status_code == 200
    assert response.json()["question_evaluations"][0]["marks_awarded"] == 4.0
    # ensure total calculation is correct
    assert response.json()["total_marks_obtained"] == 4.0

