from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
import pickle
import numpy as np
from model import EmailPreprocessor


class ModelComponents:
    def __init__(self):
        self.model = None
        self.tfidf = None
        self.preprocessor = None


components = ModelComponents()


def load_model(model_path='phishing_model.pkl'):
    try:
        with open(model_path, 'rb') as f:
            saved_data = pickle.load(f)
            components.model = saved_data['model']
            components.tfidf = saved_data['tfidf']
            components.preprocessor = EmailPreprocessor()
        return True
    except Exception as e:
        print(f"Error loading model: {str(e)}")
        return False


@asynccontextmanager
async def lifespan(_: FastAPI):
    if not load_model():
        raise Exception("Failed to load model during startup")
    yield
    components.model = None
    components.tfidf = None
    components.preprocessor = None


app = FastAPI(
    title="Email Phishing Detection API",
    description="API for detecting phishing attempts in emails",
    version="1.0.0",
    lifespan=lifespan
)


class EmailRequest(BaseModel):
    email_text: str


class EmailAnalysis(BaseModel):
    probability: float
    is_phishing: bool
    features: dict


@app.post("/analyze-email", response_model=EmailAnalysis)
async def analyze_email(request: EmailRequest):
    if not all([components.preprocessor, components.model, components.tfidf]):
        raise HTTPException(status_code=500, detail="Model components not initialized")

    try:
        cleaned_text = components.preprocessor.preprocess_text(request.email_text)
        features = components.preprocessor.extract_features(request.email_text)

        text_features = components.tfidf.transform([cleaned_text])
        X = np.hstack((text_features.toarray(), np.array([list(features.values())])))

        probability = float(components.model.predict_proba(X)[0, 1])
        is_phishing = probability >= 0.5

        return EmailAnalysis(
            probability=probability,
            is_phishing=is_phishing,
            features=features
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
