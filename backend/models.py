from sqlalchemy import Column, Integer, String, Float, ForeignKey, Date, DateTime, UniqueConstraint
from sqlalchemy.orm import relationship
import datetime
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    password_hash = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    budgets = relationship("Budget", back_populates="user", cascade="all, delete-orphan")

class Budget(Base):
    __tablename__ = "budgets"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    total_amount = Column(Float, nullable=False)
    month = Column(Integer, nullable=False)
    year = Column(Integer, nullable=False)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    user = relationship("User", back_populates="budgets")
    categories = relationship("Category", back_populates="budget", cascade="all, delete-orphan")

    __table_args__ = (
        UniqueConstraint("user_id", "month", "year", name="unique_user_month_year"),
    )

class Category(Base):
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, index=True)
    budget_id = Column(Integer, ForeignKey("budgets.id", ondelete="CASCADE"), nullable=False)
    category_name = Column(String, nullable=False)
    allocated_amount = Column(Float, nullable=False)
    remaining_amount = Column(Float, nullable=False)
    color = Column(String(7), nullable=False) # e.g. #FF5733
    icon = Column(String(50), nullable=False) # Icon name

    budget = relationship("Budget", back_populates="categories")
    expenses = relationship("Expense", back_populates="category", cascade="all, delete-orphan")

class Expense(Base):
    __tablename__ = "expenses"

    id = Column(Integer, primary_key=True, index=True)
    category_id = Column(Integer, ForeignKey("categories.id", ondelete="CASCADE"), nullable=False)
    amount = Column(Float, nullable=False)
    merchant = Column(String, nullable=False)
    date = Column(Date, nullable=False)
    remarks = Column(String, nullable=True)

    category = relationship("Category", back_populates="expenses")
