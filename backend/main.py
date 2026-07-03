from fastapi import FastAPI, Depends, HTTPException, status, Query
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
import datetime
from typing import List, Optional

import database, models, schemas, crud, auth, ai

# Initialize FastAPI App
app = FastAPI(title="Smart Budget Assistant API", version="1.0.0")

# Enable CORS (Critical for Android connections over Local Wi-Fi)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Database tables with a retry loop
import time
for i in range(6):
    try:
        models.Base.metadata.create_all(bind=database.engine)
        print("Database tables initialized successfully!")
        break
    except Exception as e:
        if i == 5:
            raise e
        print(f"Database connection attempt {i+1} failed: {e}. Retrying in 5 seconds...")
        time.sleep(5)

# Static file serving & Web Preview
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
import os

os.makedirs("static", exist_ok=True)
app.mount("/static", StaticFiles(directory="static"), name="static")

@app.get("/", response_class=HTMLResponse)
def read_root():
    with open("static/index.html", "r", encoding="utf-8") as f:
        return f.read()

@app.get("/db", response_class=HTMLResponse)
def read_db_preview():
    with open("static/db.html", "r", encoding="utf-8") as f:
        return f.read()

@app.get("/api/admin/db")
def get_db_dump(db: Session = Depends(database.get_db)):
    users = db.query(models.User).all()
    budgets = db.query(models.Budget).all()
    categories = db.query(models.Category).all()
    expenses = db.query(models.Expense).all()
    return {
        "users": [{"id": u.id, "name": u.name, "email": u.email, "created_at": u.created_at} for u in users],
        "budgets": [{"id": b.id, "user_id": b.user_id, "total_amount": b.total_amount, "month": b.month, "year": b.year, "created_at": b.created_at} for b in budgets],
        "categories": [{"id": c.id, "budget_id": c.budget_id, "category_name": c.category_name, "allocated_amount": c.allocated_amount, "remaining_amount": c.remaining_amount, "color": c.color, "icon": c.icon} for c in categories],
        "expenses": [{"id": e.id, "category_id": e.category_id, "amount": e.amount, "merchant": e.merchant, "date": e.date, "remarks": e.remarks} for e in expenses]
    }

@app.post("/api/admin/db/reset")
def reset_db(db: Session = Depends(database.get_db)):
    try:
        models.Base.metadata.drop_all(bind=database.engine)
        models.Base.metadata.create_all(bind=database.engine)
        return {"message": "Database reset successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/api/admin/user/{id}")
def delete_user(id: int, db: Session = Depends(database.get_db)):
    try:
        user = db.query(models.User).filter(models.User.id == id).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        db.delete(user)
        db.commit()
        return {"message": "User deleted successfully"}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

# Helper to get current month/year
def get_current_month_year() -> tuple:
    now = datetime.datetime.now()
    return now.month, now.year

# ----------------- AUTHENTICATION ROUTES -----------------

@app.post("/signup", response_model=schemas.UserOut, status_code=status.HTTP_201_CREATED)
def signup(user: schemas.UserCreate, db: Session = Depends(database.get_db)):
    db_user = crud.get_user_by_email(db, user.email)
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    return crud.create_user(db, user)

@app.post("/login", response_model=schemas.Token)
def login(user: schemas.UserLogin, db: Session = Depends(database.get_db)):
    db_user = crud.get_user_by_email(db, user.email)
    if not db_user or not auth.verify_password(user.password, db_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    access_token = auth.create_access_token(data={"sub": str(db_user.id)})
    return {"access_token": access_token, "token_type": "bearer"}

@app.get("/user/me", response_model=schemas.UserOut)
def get_user_me(current_user: models.User = Depends(auth.get_current_user)):
    return current_user

# ----------------- BUDGET ROUTES -----------------

@app.post("/create-budget", response_model=schemas.BudgetOut)
def create_budget(
    budget: schemas.BudgetCreate,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    return crud.create_budget(db, current_user.id, budget)

@app.get("/budget", response_model=schemas.BudgetOut)
def get_budget(
    month: Optional[int] = Query(None, ge=1, le=12),
    year: Optional[int] = Query(None, ge=2000),
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    curr_month, curr_year = get_current_month_year()
    m = month if month is not None else curr_month
    y = year if year is not None else curr_year
    
    budget = crud.get_budget_by_date(db, current_user.id, m, y)
    if not budget:
        # Auto-create a default empty budget of ₹0 so the UI doesn't crash, or raise 404.
        # Let's raise 404, prompting the user to set up a budget.
        raise HTTPException(status_code=404, detail="No budget allocated for this month.")
    return budget

@app.put("/budget", response_model=schemas.BudgetOut)
def update_budget(
    budget_id: int,
    total_amount: float,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    # Verify ownership
    budget = db.query(models.Budget).filter(models.Budget.id == budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to edit this budget")
    return crud.update_budget(db, budget_id, total_amount)

# ----------------- CATEGORY ROUTES -----------------

@app.post("/category", response_model=schemas.CategoryOut)
def create_category(
    category: schemas.CategoryCreate,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    # Verify budget ownership
    budget = db.query(models.Budget).filter(models.Budget.id == category.budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to add categories to this budget")
    return crud.create_category(db, category)

@app.get("/categories", response_model=List[schemas.CategoryOut])
def get_categories(
    budget_id: int,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    # Verify budget ownership
    budget = db.query(models.Budget).filter(models.Budget.id == budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to access this budget")
    return crud.get_categories_by_budget(db, budget_id)

@app.put("/category/{id}", response_model=schemas.CategoryOut)
def update_category(
    id: int,
    category_data: schemas.CategoryUpdate,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    category = db.query(models.Category).filter(models.Category.id == id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
        
    budget = db.query(models.Budget).filter(models.Budget.id == category.budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to edit this category")
        
    return crud.update_category(db, id, category_data)

@app.delete("/category/{id}")
def delete_category(
    id: int,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    category = db.query(models.Category).filter(models.Category.id == id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
        
    budget = db.query(models.Budget).filter(models.Budget.id == category.budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to delete this category")
        
    crud.delete_category(db, id)
    return {"message": "Category deleted successfully"}

# ----------------- EXPENSE ROUTES -----------------

@app.post("/expense", response_model=schemas.ExpenseOut)
def create_expense(
    expense: schemas.ExpenseCreate,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    # Verify category and budget ownership
    category = db.query(models.Category).filter(models.Category.id == expense.category_id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
        
    budget = db.query(models.Budget).filter(models.Budget.id == category.budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to add expenses to this category")
        
    return crud.create_expense(db, expense)

@app.get("/expenses", response_model=List[schemas.ExpenseOut])
def get_expenses(
    budget_id: int,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    # Verify budget ownership
    budget = db.query(models.Budget).filter(models.Budget.id == budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized")
    return crud.get_expenses_by_budget(db, budget_id)

@app.delete("/expense/{id}")
def delete_expense(
    id: int,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    expense = db.query(models.Expense).filter(models.Expense.id == id).first()
    if not expense:
        raise HTTPException(status_code=404, detail="Expense not found")
        
    category = db.query(models.Category).filter(models.Category.id == expense.category_id).first()
    budget = db.query(models.Budget).filter(models.Budget.id == category.budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized")
        
    crud.delete_expense(db, id)
    return {"message": "Expense deleted and budget restored successfully"}

# ----------------- REPORTS & AI ROUTES -----------------

@app.get("/reports/monthly", response_model=schemas.MonthlyReportOut)
def get_monthly_report(
    month: Optional[int] = Query(None, ge=1, le=12),
    year: Optional[int] = Query(None, ge=2000),
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    curr_month, curr_year = get_current_month_year()
    m = month if month is not None else curr_month
    y = year if year is not None else curr_year
    
    budget = crud.get_budget_by_date(db, current_user.id, m, y)
    if not budget:
        raise HTTPException(status_code=404, detail="No budget allocated for this month")
        
    categories = crud.get_categories_by_budget(db, budget.id)
    total_spent = sum([cat.allocated_amount - cat.remaining_amount for cat in categories])
    total_remaining = budget.total_amount - total_spent
    
    category_reports = []
    for cat in categories:
        spent = cat.allocated_amount - cat.remaining_amount
        percentage = (spent / total_spent * 100) if total_spent > 0 else 0.0
        category_reports.append(
            schemas.CategoryReport(
                category_id=cat.id,
                category_name=cat.category_name,
                allocated_amount=cat.allocated_amount,
                spent_amount=spent,
                remaining_amount=cat.remaining_amount,
                color=cat.color,
                percentage=round(percentage, 2)
            )
        )
        
    return schemas.MonthlyReportOut(
        total_budget=budget.total_amount,
        total_spent=total_spent,
        total_remaining=total_remaining,
        categories=category_reports
    )

@app.post("/ai/classify", response_model=schemas.AICategorizationResponse)
def get_ai_classification(
    req: schemas.AICategorizationRequest,
    current_user: models.User = Depends(auth.get_current_user)
):
    category_name, confidence = ai.classify_merchant(req.merchant)
    return schemas.AICategorizationResponse(category_name=category_name, confidence=confidence)

@app.get("/ai/insights", response_model=schemas.AIInsightsResponse)
def get_ai_insights(
    budget_id: int,
    db: Session = Depends(database.get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    budget = db.query(models.Budget).filter(models.Budget.id == budget_id).first()
    if not budget or budget.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Unauthorized")
        
    categories = crud.get_categories_by_budget(db, budget.id)
    total_spent = sum([cat.allocated_amount - cat.remaining_amount for cat in categories])
    
    insights = ai.generate_spending_insights(budget.total_amount, total_spent, categories)
    return schemas.AIInsightsResponse(insights=insights)
