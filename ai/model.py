import pandas as pd
import numpy as np
import re
from dataclasses import dataclass, field
from typing import Dict, List, Set
from nltk.corpus import stopwords
import nltk
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
import xgboost as xgb
from imblearn.over_sampling import SMOTE
import pickle

nltk.download(['stopwords'])


@dataclass
class EmailPreprocessor:
    stop_words: List[str] = field(default_factory=lambda: list(stopwords.words('english')))
    url_pattern: str = r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\\(\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+'
    email_pattern: str = r'\S+@\S+'
    special_chars_pattern: str = r'[!@#$%^&*(),.?":{}|<>]'

    shortened_domains: Set[str] = field(default_factory=lambda: {
        'bit.ly', 'tinyurl', 't.co', 'goo.gl', 'ow.ly', 'is.gd', 'buff.ly',
        'rebrand.ly', 'clicky.me', 'shorte.st', 'bl.ink', 'cutt.ly', 'snip.ly',
        'short.io', 'tiny.cc', 'clck.ru', 'shrtco.de', 'v.gd', 'po.st', 'mcaf.ee',
        'qr.ae', 'trib.al', 'lnkd.in', 'db.tt', 'amzn.to', 'fb.me', 'ift.tt',
        'youtu.be', 'wp.me', 'twit.ac', 'chilp.it', 'u.nu', 'tny.im'
    })

    urgent_words: Set[str] = field(default_factory=lambda: {
        'urgent', 'immediate', 'attention', 'important', 'action required', 'act now',
        'expires', 'deadline', 'critical', 'warning', 'limited time', 'account suspended',
        'security alert', 'verify now', 'asap', 'do not ignore', 'response needed',
        'immediate response', 'time-sensitive', 'final notice', 'last chance', 'reply now',
        'respond urgently', 'act fast', 'ending soon', 'action needed', 'verify immediately'
    })

    money_words: Set[str] = field(default_factory=lambda: {
        'money', 'cash', 'bank', 'account', 'credit', 'debit', 'payment', 'transfer',
        'transaction', 'financial', 'balance', 'fund', 'deposit', 'wire', 'bitcoin',
        'crypto', 'investment', 'prize', 'lottery', 'inheritance', 'check', 'invoice',
        'refund', 'fee', 'reward', 'bonus', 'salary', 'loan', 'mortgage', 'profit',
        'earn', 'payout', 'winning', 'rich', 'fortune', 'gold', 'payment overdue',
        'billing', 'crypto wallet', 'insurance claim'
    })

    threat_words: Set[str] = field(default_factory=lambda: {
        'suspend', 'terminate', 'block', 'restrict', 'limit', 'close', 'delete',
        'unauthorized', 'suspicious', 'breach', 'compromised', 'hacked', 'violated',
        'locked', 'security', 'verify', 'confirm', 'validate', 'alert', 'warning',
        'penalty', 'fine', 'violation', 'legal action', 'lawsuit', 'investigation',
        'unauthorized access', 'security breach', 'fraud', 'scam', 'identity theft',
        'illegal', 'locked out', 'account compromise', 'risk', 'threat', 'malicious'
    })

    def extract_features(self, text: str) -> Dict[str, float]:
        if not isinstance(text, str):
            return self._get_default_features()

        text_lower = text.lower()
        words = text.split()

        return {
            'text_length': len(text),
            'word_count': len(words),
            'avg_word_length': np.mean([len(word) for word in words]) if words else 0,
            'url_count': len(re.findall(self.url_pattern, text)),
            'has_shortened_url': int(any(domain in text_lower for domain in self.shortened_domains)),
            'urgent_words_count': sum(1 for word in self.urgent_words if word in text_lower),
            'money_words_count': sum(1 for word in self.money_words if word in text_lower),
            'threat_words_count': sum(1 for word in self.threat_words if word in text_lower),
            'exclamation_count': text.count('!'),
            'question_count': text.count('?'),
            'special_chars_count': len(re.findall(self.special_chars_pattern, text)),
            'uppercase_ratio': sum(1 for c in text if c.isupper()) / max(len(text), 1),
            'digit_ratio': sum(1 for c in text if c.isdigit()) / max(len(text), 1),
            'has_email': int(bool(re.search(self.email_pattern, text))),
            'multiple_emails': int(len(re.findall(self.email_pattern, text)) > 1),
            'has_urgent': int(any(word in text_lower for word in self.urgent_words)),
            'has_money': int(any(word in text_lower for word in self.money_words)),
            'has_threat': int(any(word in text_lower for word in self.threat_words))
        }

    @staticmethod
    def _get_default_features() -> Dict[str, float]:
        return {feature: 0 for feature in [
            'text_length', 'word_count', 'avg_word_length', 'url_count', 'has_shortened_url',
            'urgent_words_count', 'money_words_count', 'threat_words_count', 'exclamation_count',
            'question_count', 'special_chars_count', 'uppercase_ratio', 'digit_ratio',
            'has_email', 'multiple_emails', 'has_urgent', 'has_money', 'has_threat'
        ]}

    def preprocess_text(self, text: str) -> str:
        if not isinstance(text, str):
            return ""

        text = self._replace_patterns(text, self.url_pattern, 'URL')
        text = self._replace_patterns(text, self.email_pattern, 'EMAIL')

        text = text.lower()
        text = re.sub(r'\d+', 'NUM', text)
        text = re.sub(r'[^\w\s]', ' ', text)

        tokens = [token for token in text.split() if token not in self.stop_words]
        return ' '.join(tokens)

    @staticmethod
    def _replace_patterns(text: str, pattern: str, placeholder: str) -> str:
        matches = re.findall(pattern, text)
        for idx, match in enumerate(matches):
            text = text.replace(match, f' {placeholder}{idx} ')
        return text


def train_model(input_df: pd.DataFrame) -> tuple:
    print("Training phishing detection model...")
    email_preprocessor = EmailPreprocessor()

    processed_df = input_df.copy()
    processed_df['cleaned_text'] = processed_df['Email Text'].apply(email_preprocessor.preprocess_text)
    feature_matrix = pd.DataFrame(processed_df['Email Text'].apply(email_preprocessor.extract_features).tolist())

    text_vectorizer = TfidfVectorizer(ngram_range=(1, 3), max_features=1500, min_df=2, max_df=0.95)
    text_features = text_vectorizer.fit_transform(processed_df['cleaned_text'])

    X = np.hstack((text_features.toarray(), feature_matrix.values))
    y = processed_df['Email Type'].apply(lambda x: 1 if x == 'Phishing Email' else 0)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    X_train_balanced, y_train_balanced = SMOTE(random_state=42).fit_resample(X_train, y_train)

    classifier = xgb.XGBClassifier(
        objective='binary:logistic',
        max_depth=6,
        learning_rate=0.1,
        n_estimators=200,
        subsample=0.8,
        colsample_bytree=0.8
    )
    classifier.fit(X_train_balanced, y_train_balanced)

    y_pred = classifier.predict(X_test)
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))

    with open('phishing_model.pkl', 'wb') as f:
        pickle.dump({'model': classifier, 'tfidf': text_vectorizer}, f)

    return classifier, text_vectorizer


if __name__ == "__main__":
    df = pd.read_csv('data/Phishing_Email.csv')
    print(f"Loaded {len(df)} emails")
    model, tfidf = train_model(df)
