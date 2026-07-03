from pydantic import BaseModel, EmailStr, Field
from typing import List, Optional
import datetime

# Authentication Schemas
class UserCreate(BaseModel):
    name: str = Field(..., min_length=2)
    email: EmailStr
    password: str = Field(..., min_length=6)

class UserLogin(BaseModel):
    email: EmailStr
    password: str

class UserOut(BaseModel):
    id: int
    name: str
    email: EmailStr
    created_at: datetime.datetime

    class Config:
        from_attributes = True

class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    user_id: Optional[int] = None

# Budget Schemas
class BudgetCreate(BaseModel):
    total_amount: float = Field(..., gt=0)
    month: int = Field(..., ge=1, le=12)
    year: int = Field(..., ge=2000)

class BudgetUpdate(BaseModel):
    total_amount: float = Field(..., gt=0)

class BudgetOut(BaseModel):
    id: int
    user_id: int
    total_amount: float
    month: int
    year: int
    created_at: datetime.datetime

    class Config:
        from_attributes = True

# Category Schemas
class CategoryCreate(BaseModel):
    budget_id: int
    category_name: str = Field(..., min_length=1)
    allocated_amount: float = Field(..., gt=0)
    color: str = Field(..., pattern="^#[0-9a-fA-F]{6}$") # E.g. #FF5733
    icon: str = Field(..., min_length=1) # E.g. "food", "home"

class CategoryUpdate(BaseModel):
    category_name: Optional[str] = None
    allocated_amount: Optional[float] = None
    color: Optional[str] = None
    icon: Optional[str] = None

class CategoryOut(BaseModel):
    id: int
    budget_id: int
    category_name: str
    allocated_amount: float
    remaining_amount: float
    color: str
    icon: str

    class Config:
        from_attributes = True

# Expense Schemas
class ExpenseCreate(BaseModel):
    category_id: int
    amount: float = Field(..., gt=0)
    merchant: str = Field(..., min_length=1)
    date: datetime.date
    remarks: Optional[str] = None

class ExpenseOut(BaseModel):
    id: int
    category_id: int
    amount: float
    merchant: str
    date: datetime.date
    remarks: Optional[str]

    class Config:
        from_attributes = True

# AI Schemas
class AICategorizationRequest(BaseModel):
    merchant: str

class AICategorizationResponse(BaseModel):
    category_name: str
    confidence: float

class AIInsightsResponse(BaseModel):
    insights: List[str]

# Report Schemas
class CategoryReport(BaseModel):
    category_id: int
    category_name: str
    allocated_amount: float
    spent_amount: float
    remaining_amount: float
    color: str
    percentage: float

class MonthlyReportOut(BaseModel):
    total_budget: float
    total_spent: float
    total_remaining: float
    categories: List[CategoryReport]
