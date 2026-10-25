import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from dotenv import load_dotenv
from google import genai
from google.genai import types

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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
