from sqlalchemy.orm import Session
from fastapi import HTTPException, status
import models, schemas, auth
import datetime

# User Operations
def get_user_by_email(db: Session, email: str):
    return db.query(models.User).filter(models.User.email == email).first()

def create_user(db: Session, user: schemas.UserCreate):
    hashed_password = auth.get_password_hash(user.password)
    db_user = models.User(
        name=user.name,
        email=user.email,
        password_hash=hashed_password
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

# Budget Operations
def get_budget_by_date(db: Session, user_id: int, month: int, year: int):
    return db.query(models.Budget).filter(
        models.Budget.user_id == user_id,
        models.Budget.month == month,
        models.Budget.year == year
    ).first()

def create_budget(db: Session, user_id: int, budget: schemas.BudgetCreate):
    existing = get_budget_by_date(db, user_id, budget.month, budget.year)
    if existing:
        existing.total_amount = budget.total_amount
        db.commit()
        db.refresh(existing)
        return existing

    db_budget = models.Budget(
        user_id=user_id,
        total_amount=budget.total_amount,
        month=budget.month,
        year=budget.year
    )
    db.add(db_budget)
    db.commit()
    db.refresh(db_budget)
    return db_budget

def update_budget(db: Session, budget_id: int, total_amount: float):
    db_budget = db.query(models.Budget).filter(models.Budget.id == budget_id).first()
    if not db_budget:
        raise HTTPException(status_code=404, detail="Budget not found")
    db_budget.total_amount = total_amount
    db.commit()
    db.refresh(db_budget)
    return db_budget

# Category Operations
def create_category(db: Session, category: schemas.CategoryCreate):
    # Verify budget exists
    budget = db.query(models.Budget).filter(models.Budget.id == category.budget_id).first()
    if not budget:
        raise HTTPException(status_code=404, detail="Budget not found")
    
    # Calculate sum of other category allocations
    allocated_sum = db.query(models.Category).filter(
        models.Category.budget_id == category.budget_id
    ).with_entities(models.Category.allocated_amount).all()
    
    total_allocated = sum([a[0] for a in allocated_sum])
    if total_allocated + category.allocated_amount > budget.total_amount:
        raise HTTPException(
            status_code=400,
            detail=f"Cannot allocate ₹{category.allocated_amount}. Maximum available budget allocation remaining is ₹{budget.total_amount - total_allocated:.2f}."
        )

    db_category = models.Category(
        budget_id=category.budget_id,
        category_name=category.category_name,
        allocated_amount=category.allocated_amount,
        remaining_amount=category.allocated_amount, # Initially, remaining is equal to allocated
        color=category.color,
        icon=category.icon
    )
    db.add(db_category)
    db.commit()
    db.refresh(db_category)
    return db_category

def get_categories_by_budget(db: Session, budget_id: int):
    return db.query(models.Category).filter(models.Category.budget_id == budget_id).all()

def update_category(db: Session, category_id: int, update_data: schemas.CategoryUpdate):
    db_category = db.query(models.Category).filter(models.Category.id == category_id).first()
    if not db_category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    # If editing the allocation amount
    if update_data.allocated_amount is not None:
        budget = db.query(models.Budget).filter(models.Budget.id == db_category.budget_id).first()
        allocated_sum = db.query(models.Category).filter(
            models.Category.budget_id == db_category.budget_id,
            models.Category.id != category_id
        ).with_entities(models.Category.allocated_amount).all()
        
        total_other_allocated = sum([a[0] for a in allocated_sum])
        if total_other_allocated + update_data.allocated_amount > budget.total_amount:
            raise HTTPException(
                status_code=400,
                detail=f"Cannot update allocation to ₹{update_data.allocated_amount}. Total budget would be exceeded."
            )
            
        spent = db_category.allocated_amount - db_category.remaining_amount
        if update_data.allocated_amount < spent:
            raise HTTPException(
                status_code=400,
                detail=f"Cannot reduce allocation below already spent amount (₹{spent:.2f})."
            )
        
        db_category.remaining_amount = update_data.allocated_amount - spent
        db_category.allocated_amount = update_data.allocated_amount

    if update_data.category_name is not None:
        db_category.category_name = update_data.category_name
    if update_data.color is not None:
        db_category.color = update_data.color
    if update_data.icon is not None:
        db_category.icon = update_data.icon
        
    db.commit()
    db.refresh(db_category)
    return db_category

def delete_category(db: Session, category_id: int):
    db_category = db.query(models.Category).filter(models.Category.id == category_id).first()
    if not db_category:
        raise HTTPException(status_code=404, detail="Category not found")
    db.delete(db_category)
    db.commit()
    return True

# Expense Operations
def create_expense(db: Session, expense: schemas.ExpenseCreate):
    category = db.query(models.Category).filter(models.Category.id == expense.category_id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")
    
    # CRITICAL BUDGET EXCEEDED CHECK
    if category.remaining_amount < expense.amount:
        raise HTTPException(
            status_code=400,
            detail=f"Budget Exceeded. You only have ₹{category.remaining_amount:.0f} left. Please reduce the amount or transfer funds from another category."
        )
    
    db_expense = models.Expense(
        category_id=expense.category_id,
        amount=expense.amount,
        merchant=expense.merchant,
        date=expense.date,
        remarks=expense.remarks
    )
    
    # Deduct from category budget
    category.remaining_amount -= expense.amount
    
    db.add(db_expense)
    db.commit()
    db.refresh(db_expense)
    return db_expense

def get_expenses_by_budget(db: Session, budget_id: int):
    return db.query(models.Expense).join(models.Category).filter(
        models.Category.budget_id == budget_id
    ).all()

def delete_expense(db: Session, expense_id: int):
    db_expense = db.query(models.Expense).filter(models.Expense.id == expense_id).first()
    if not db_expense:
        raise HTTPException(status_code=404, detail="Expense not found")
    
    # Restore the remaining amount in the category
    category = db.query(models.Category).filter(models.Category.id == db_expense.category_id).first()
    if category:
        category.remaining_amount += db_expense.amount
        
    db.delete(db_expense)
    db.commit()
    return True
