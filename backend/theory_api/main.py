import os
import json
from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from typing import List, Optional
from pydantic import BaseModel, Field
from dotenv import load_dotenv
from google import genai
from google.genai import types

from eval_schemas import EvaluationReport, ExtractedQuestionsResponse

load_dotenv()

app = FastAPI(title="Theory Assistant API")

# Setup Gemini Client
# The client automatically picks up GEMINI_API_KEY from environment
API_KEY = os.environ.get("GEMINI_API_KEY")
MODEL_NAME = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")

class TheoryRequest(BaseModel):
    question: str = Field(..., min_length=1, max_length=1000)
    subject: str = Field(default="General", max_length=100)
    detail_level: str = Field(default="medium", pattern="^(short|medium|detailed)$")

class TheoryResponse(BaseModel):
    answer: str
    model: str

@app.get("/health")
def health_check():
    return {"status": "ok", "gemini_configured": bool(os.environ.get("GEMINI_API_KEY"))}

@app.post("/api/theory/ask", response_model=TheoryResponse)
def ask_theory(req: TheoryRequest):
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="Gemini API Key is not configured on the server.")

    model_name = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")

    try:
        client = genai.Client()
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to initialize Gemini Client.")

    system_prompt = (
        f"You are an educational assistant for {req.subject}. "
        "Answer the user's educational question directly and accurately. "
        "Explain technical concepts in clear, understandable language. "
        "State uncertainty when information is unclear. "
        "Do not invent citations. Do not format the response with overly complex markdown that cannot be read as plain text, but simple bold/lists are fine. "
        f"Provide a {req.detail_level} length answer."
    )

    try:
        response = client.models.generate_content(
            model=model_name,
            contents=req.question,
            config=types.GenerateContentConfig(
                system_instruction=system_prompt,
                temperature=0.7,
            )
        )
        return TheoryResponse(answer=response.text, model=model_name)
    except Exception as e:
        # Avoid leaking full error details which might contain the API key or internal structures
        raise HTTPException(status_code=502, detail="Failed to communicate with Gemini API or process request.")

@app.post("/api/theory/extract_questions", response_model=ExtractedQuestionsResponse)
def extract_questions(file: UploadFile = File(...)):
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="Gemini API Key is not configured on the server.")

    model_name = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")

    try:
        client = genai.Client()
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to initialize Gemini Client.")

    try:
        contents = file.file.read()
        mime_type = file.content_type or "image/jpeg"

        system_instruction = "You are an assistant that extracts questions and their maximum marks from an exam question paper."
        prompt = "Extract all the questions from this question paper image. For each question, extract the question number, the question text, and the maximum marks allocated to it. If maximum marks are not explicitly stated, estimate 0.0."

        response = client.models.generate_content(
            model=model_name,
            contents=[
                types.Part.from_bytes(data=contents, mime_type=mime_type),
                prompt
            ],
            config=types.GenerateContentConfig(
                system_instruction=system_instruction,
                temperature=0.1,
                response_mime_type="application/json",
                response_schema=ExtractedQuestionsResponse
            )
        )

        # Parse JSON from response
        try:
            res_json = json.loads(response.text)
            return ExtractedQuestionsResponse(**res_json)
        except Exception:
            raise HTTPException(status_code=502, detail="Invalid structured response from Gemini API.")

    except Exception as e:
        raise HTTPException(status_code=502, detail="Failed to communicate with Gemini API or process request.")

@app.post("/api/theory/evaluate", response_model=EvaluationReport)
def evaluate_answers(
    questions_json: str = Form(...),
    files: List[UploadFile] = File(...)
):
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="Gemini API Key is not configured on the server.")

    model_name = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")

    try:
        client = genai.Client()
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to initialize Gemini Client.")

    try:
        questions_data = json.loads(questions_json)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid questions_json format.")

    try:
        parts = []
        for f in files:
            contents = f.file.read()
            mime_type = f.content_type or "image/jpeg"
            parts.append(types.Part.from_bytes(data=contents, mime_type=mime_type))

        system_instruction = (
            "You are an expert teacher grading a student's handwritten answer sheet. "
            "You will be provided with images of the student's handwritten answer sheet, and a list of questions with their max marks. "
            "Transcribe the student's answer for each question. Evaluate the answer fairly based on correctness and conceptual understanding. "
            "Award marks out of the max_marks for that question. Do not exceed max_marks. "
            "If the handwriting is illegible, flag it for manual review. If a question is not answered, award 0 marks."
        )

        prompt = f"Please evaluate the student's answers for the following questions:\\n{json.dumps(questions_data, indent=2)}\\nHere are the images of the student's answer sheet."
        parts.append(prompt)

        response = client.models.generate_content(
            model=model_name,
            contents=parts,
            config=types.GenerateContentConfig(
                system_instruction=system_instruction,
                temperature=0.2,
                response_mime_type="application/json",
                response_schema=EvaluationReport
            )
        )

        try:
            res_json = json.loads(response.text)

            # Recalculate total to ensure it matches
            total_obtained = sum(q.get("marks_awarded", 0.0) for q in res_json.get("question_evaluations", []))
            max_possible = sum(q.get("max_marks", 0.0) for q in res_json.get("question_evaluations", []))
            res_json["total_marks_obtained"] = total_obtained
            res_json["max_possible_marks"] = max_possible
            res_json["percentage"] = (total_obtained / max_possible * 100.0) if max_possible > 0 else 0.0

            # Check if any question needs manual review
            needs_review = any(q.get("needs_manual_review", False) for q in res_json.get("question_evaluations", []))
            res_json["requires_manual_review"] = needs_review

            return EvaluationReport(**res_json)
        except Exception as ex:
            raise HTTPException(status_code=502, detail="Invalid structured response from Gemini API.")

    except Exception as e:
        raise HTTPException(status_code=502, detail="Failed to communicate with Gemini API or process request.")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
