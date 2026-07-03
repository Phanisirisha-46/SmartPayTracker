import re
from typing import List, Dict, Tuple

# AI Classification patterns
MERCHANT_PATTERNS: List[Tuple[str, str]] = [
    (r"(?i)swiggy|zomato|eat|restaurant|food|cafe|starbucks|pizza|burger|kfc|mcdonald|instamart|blinkit|zepto|grocery", "Food"),
    (r"(?i)apollo|clinic|medplus|hospital|pharmacy|pharma|doctor|medicine|health|dentist|lab|diagnostic", "Hospital"),
    (r"(?i)rent|landlord|pg|flat|lease|room|maintenance|broker", "Rent"),
    (r"(?i)amazon|flipkart|shopping|myntra|ajio|zara|h&m|clothing|apparel|shoe|electronics|gadget|mall", "Shopping"),
    (r"(?i)indian\s*oil|hp\s*petrol|shell|fuel|cng|gas|petrol|station|bharat\s*petroleum|speed", "Fuel"),
    (r"(?i)school|tuition|milk|grocery|supermarket|kids|parent|gift|transfer|cash", "Family")
]

def classify_merchant(merchant: str) -> Tuple[str, float]:
    """
    Returns suggested category name and a confidence score.
    """
    merchant = merchant.strip()
    if not merchant:
        return "Others", 0.1

    for pattern, category in MERCHANT_PATTERNS:
        if re.search(pattern, merchant):
            return category, 0.95
            
    # Default fallback
    return "Others", 0.3

def generate_spending_insights(total_budget: float, total_spent: float, categories: list) -> List[str]:
    """
    Generates personalized insights based on current spending metrics.
    `categories` is a list of objects or dictionaries containing:
    category_name, allocated_amount, remaining_amount
    """
    insights = []
    
    if total_budget == 0:
        return ["Please set up your budget to receive spending insights."]
        
    overall_spent_pct = (total_spent / total_budget) * 100
    insights.append(f"Overall, you have spent {overall_spent_pct:.1f}% of your monthly budget (₹{total_spent:.0f} of ₹{total_budget:.0f}).")
    
    exhausted_cats = []
    warning_cats = []
    healthy_cats = []
    
    for cat in categories:
        # Support both objects and dicts
        name = getattr(cat, "category_name", cat.get("category_name") if isinstance(cat, dict) else "")
        allocated = getattr(cat, "allocated_amount", cat.get("allocated_amount") if isinstance(cat, dict) else 0)
        remaining = getattr(cat, "remaining_amount", cat.get("remaining_amount") if isinstance(cat, dict) else 0)
        
        spent = allocated - remaining
        spent_pct = (spent / allocated) * 100 if allocated > 0 else 0
        
        if spent_pct >= 100:
            exhausted_cats.append((name, spent_pct, remaining))
        elif spent_pct >= 80:
            warning_cats.append((name, spent_pct, remaining))
        elif spent_pct < 40 and remaining > 2000:
            healthy_cats.append((name, spent_pct, remaining))

    # Generate warnings
    for name, pct, rem in exhausted_cats:
        insights.append(f"⚠️ Your '{name}' budget has been 100% completed and is now exceeded! Any further payment will be blocked.")
        
    for name, pct, rem in warning_cats:
        insights.append(f"⚠️ You've spent {pct:.0f}% of your '{name}' budget this month. You only have ₹{rem:.0f} left.")

    # Suggest fund transfers!
    # If we have warning/exhausted categories and some healthy categories with excess funds
    if (exhausted_cats or warning_cats) and healthy_cats:
        needy = exhausted_cats + warning_cats
        # Sort by urgency
        needy.sort(key=lambda x: x[1], reverse=True)
        # Sort healthy by excess funds
        healthy_cats.sort(key=lambda x: x[2], reverse=True)
        
        target_name = needy[0][0]
        source_name = healthy_cats[0][0]
        source_remaining = healthy_cats[0][2]
        
        # Suggest moving a reasonable amount (e.g. 20% of source remaining, or up to 2000)
        transfer_suggestion = min(source_remaining * 0.3, 2000.0)
        # Round to nearest 100
        transfer_suggestion = round(transfer_suggestion / 100) * 100
        
        if transfer_suggestion >= 500:
            insights.append(
                f"💡 Tip: Consider moving ₹{transfer_suggestion:,.0f} from '{source_name}' to '{target_name}' to cover pending expenses."
            )
            
    # Positive feedback if doing well
    if overall_spent_pct < 50 and len(exhausted_cats) == 0 and len(warning_cats) == 0:
        insights.append("🎉 You are managing your budget exceptionally well this month! Keep it up.")
        
    return insights
