from pydantic import BaseModel, Field
from typing import List, Optional

class QuestionEvaluation(BaseModel):
    question_number: int
    question_text: str
    max_marks: float
    marks_awarded: float
    student_answer_transcription: str = Field(description="Transcription of what the student wrote")
    feedback: str = Field(description="Explanation of why marks were awarded or deducted")
    correct_concepts: str = Field(description="Concepts the student got right")
    missing_concepts: str = Field(description="Concepts the student missed or got wrong")
    suggested_improvements: str = Field(description="How the student can improve this answer")
    model_answer: str = Field(description="A model answer or key points for a stronger answer")
    needs_manual_review: bool = Field(description="True if the handwriting is illegible or the answer is ambiguous")

class EvaluationReport(BaseModel):
    total_marks_obtained: float
    max_possible_marks: float
    percentage: float
    strengths: str
    weaknesses: str
    common_mistakes: str
    recommendations: str
    requires_manual_review: bool
    question_evaluations: List[QuestionEvaluation]

class ExtractedQuestion(BaseModel):
    question_number: int
    question_text: str
    max_marks: float

class ExtractedQuestionsResponse(BaseModel):
    questions: List[ExtractedQuestion]
