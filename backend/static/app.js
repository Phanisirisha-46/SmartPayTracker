// SPA Router & API Client for SmartPay Tracker

const API_BASE = ""; // Relative URL since it's hosted on the same server

// Local Session Cache
let token = localStorage.getItem("token") || null;
let userName = localStorage.getItem("userName") || "User";
let userEmail = localStorage.getItem("userEmail") || "";
let activeBudget = null;
let categories = [];
let activeCategoryId = null;

// Screen elements
const screens = {
    splash: document.getElementById("screen-splash"),
    login: document.getElementById("screen-login"),
    signup: document.getElementById("screen-signup"),
    dashboard: document.getElementById("screen-dashboard"),
    addCategory: document.getElementById("screen-add-category"),
    categoryDetails: document.getElementById("screen-category-details"),
    payment: document.getElementById("screen-payment"),
    reports: document.getElementById("screen-reports")
};

// Navigate Helper
function navigateTo(screenKey) {
    Object.keys(screens).forEach(key => {
        if (key === screenKey) {
            screens[key].classList.add("active");
        } else {
            screens[key].classList.remove("active");
        }
    });

    // Screen load initializers
    if (screenKey === "dashboard") {
        document.getElementById("user-greeting").innerText = `Hello, ${userName}`;
        loadDashboard();
    } else if (screenKey === "payment") {
        setupPaymentScreen();
    } else if (screenKey === "reports") {
        loadReports();
    }
}

// Check session on start
document.addEventListener("DOMContentLoaded", () => {
    setTimeout(() => {
        if (token) {
            navigateTo("dashboard");
        } else {
            navigateTo("login");
        }
    }, 1500); // 1.5s splash screen
});

// Setup fetch requests helper
async function apiCall(endpoint, method = "GET", body = null) {
    const headers = {};
    if (token) {
        headers["Authorization"] = token;
    }
    if (body) {
        headers["Content-Type"] = "application/json";
    }

    const config = {
        method: method,
        headers: headers
    };

    if (body) {
        config.body = JSON.stringify(body);
    }

    const response = await fetch(`${API_BASE}/${endpoint}`, config);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const error = new Error(errorData.detail || "An error occurred");
        error.status = response.status;
        throw error;
    }
    return response.json();
}

// ==================== AUTHENTICATION FLOW ====================

// Go to signup/login
document.getElementById("go-to-signup").addEventListener("click", () => navigateTo("signup"));
document.getElementById("go-to-login").addEventListener("click", () => navigateTo("login"));

// Login Action
document.getElementById("btn-login").addEventListener("click", async () => {
    const email = document.getElementById("login-email").value;
    const password = document.getElementById("login-password").value;
    const errorEl = document.getElementById("login-error");
    const spinner = document.querySelector("#btn-login .spinner");
    const btnText = document.querySelector("#btn-login .btn-text");

    if (!email || !password) {
        errorEl.innerText = "Please fill in all fields.";
        errorEl.classList.remove("hidden");
        return;
    }

    errorEl.classList.add("hidden");
    spinner.classList.remove("hidden");
    btnText.classList.add("hidden");

    try {
        const res = await apiCall("login", "POST", { email, password });
        token = `Bearer ${res.access_token}`;
        localStorage.setItem("token", token);

        // Fetch user profile
        const user = await apiCall("user/me");
        userName = user.name;
        userEmail = user.email;
        localStorage.setItem("userName", userName);
        localStorage.setItem("userEmail", userEmail);

        navigateTo("dashboard");
    } catch (e) {
        errorEl.innerText = e.message || "Invalid credentials.";
        errorEl.classList.remove("hidden");
    } finally {
        spinner.classList.add("hidden");
        btnText.classList.remove("hidden");
    }
});

// Signup Action
document.getElementById("btn-signup").addEventListener("click", async () => {
    const name = document.getElementById("signup-name").value;
    const email = document.getElementById("signup-email").value;
    const password = document.getElementById("signup-password").value;
    const errorEl = document.getElementById("signup-error");
    const spinner = document.querySelector("#btn-signup .spinner");
    const btnText = document.querySelector("#btn-signup .btn-text");

    if (!name || !email || !password) {
        errorEl.innerText = "Please fill in all fields.";
        errorEl.classList.remove("hidden");
        return;
    }
    if (password.length < 6) {
        errorEl.innerText = "Password must be at least 6 characters.";
        errorEl.classList.remove("hidden");
        return;
    }

    errorEl.classList.add("hidden");
    spinner.classList.remove("hidden");
    btnText.classList.add("hidden");

    try {
        // Register user
        await apiCall("signup", "POST", { name, email, password });
        
        // Auto Login
        const res = await apiCall("login", "POST", { email, password });
        token = `Bearer ${res.access_token}`;
        localStorage.setItem("token", token);
        
        userName = name;
        userEmail = email;
        localStorage.setItem("userName", userName);
        localStorage.setItem("userEmail", userEmail);

        navigateTo("dashboard");
    } catch (e) {
        errorEl.innerText = e.message || "Registration failed.";
        errorEl.classList.remove("hidden");
    } finally {
        spinner.classList.add("hidden");
        btnText.classList.remove("hidden");
    }
});

// Logout Action
document.getElementById("btn-logout").addEventListener("click", () => {
    token = null;
    userName = "User";
    userEmail = "";
    localStorage.clear();
    navigateTo("login");
});

// ==================== DASHBOARD FLOW ====================

async function loadDashboard() {
    try {
        const today = new Date();
        const month = today.getMonth() + 1;
        const year = today.getFullYear();
        
        // Get budget
        isBudgetSet = false;
        try {
            activeBudget = await apiCall(`budget?month=${month}&year=${year}`);
            isBudgetSet = true;
        } catch (e) {
            if (e.status === 404) {
                isBudgetSet = false;
            } else {
                throw e;
            }
        }

        if (!isBudgetSet) {
            document.getElementById("prompt-set-budget").classList.remove("hidden");
            document.getElementById("dashboard-active").classList.add("hidden");
        } else {
            document.getElementById("prompt-set-budget").classList.add("hidden");
            document.getElementById("dashboard-active").classList.remove("hidden");

            // Fetch categories
            categories = await apiCall(`categories?budget_id=${activeBudget.id}`);
            
            // Fetch AI insights
            const insightsRes = await apiCall(`ai/insights?budget_id=${activeBudget.id}`).catch(() => ({ insights: [] }));
            renderInsights(insightsRes.insights);

            // Calculate overall budget statistics
            const totalSpent = categories.reduce((sum, c) => sum + (c.allocated_amount - c.remaining_amount), 0);
            const totalRemaining = activeBudget.total_amount - totalSpent;

            document.getElementById("total-budget").innerText = `₹${activeBudget.total_amount.toLocaleString('en-IN')}`;
            document.getElementById("total-spent").innerText = `₹${totalSpent.toLocaleString('en-IN')}`;
            document.getElementById("total-remaining").innerText = `₹${totalRemaining.toLocaleString('en-IN')}`;

            renderBudgetAlerts(categories);
            renderCategories(categories);
        }
    } catch (e) {
        console.error(e);
        alert("Failed to load dashboard data: " + e.message);
    }
}

// Set Budget Action
document.getElementById("btn-create-budget").addEventListener("click", async () => {
    const amt = parseFloat(document.getElementById("input-budget-income").value);
    if (!amt || amt <= 0) {
        alert("Please enter a valid budget amount.");
        return;
    }
    try {
        const today = new Date();
        const month = today.getMonth() + 1;
        const year = today.getFullYear();

        await apiCall("create-budget", "POST", {
            total_amount: amt,
            month: month,
            year: year
        });
        loadDashboard();
    } catch (e) {
        alert("Failed to create budget: " + e.message);
    }
});

// Edit Budget Actions
const editBudgetModal = document.getElementById("edit-budget-modal");
document.getElementById("btn-edit-budget").addEventListener("click", () => {
    document.getElementById("edit-budget-input").value = activeBudget.total_amount;
    editBudgetModal.classList.remove("hidden");
});

document.getElementById("btn-close-edit").addEventListener("click", () => {
    editBudgetModal.classList.add("hidden");
});

document.getElementById("btn-confirm-edit").addEventListener("click", async () => {
    const amt = parseFloat(document.getElementById("edit-budget-input").value);
    if (!amt || amt <= 0) {
        alert("Please enter a valid amount.");
        return;
    }
    try {
        await apiCall(`budget?budget_id=${activeBudget.id}&total_amount=${amt}`, "PUT");
        editBudgetModal.classList.add("hidden");
        loadDashboard();
    } catch (e) {
        alert(e.message);
    }
});

// Render dynamic notifications (Warning Alerts)
function renderBudgetAlerts(cats) {
    const alertsBox = document.getElementById("budget-alerts");
    alertsBox.innerHTML = "<h5><i class='fa-solid fa-bell'></i> Budget Alerts</h5>";
    let hasAlerts = false;

    cats.forEach(c => {
        const spent = c.allocated_amount - c.remaining_amount;
        const spentPct = (spent / c.allocated_amount) * 100;
        
        if (spentPct >= 100) {
            hasAlerts = true;
            alertsBox.innerHTML += `<p class="alert-item"><i class="fa-solid fa-circle-exclamation text-danger"></i> <strong>${c.category_name}</strong> budget completed (100% spent).</p>`;
        } else if (spentPct >= 80) {
            hasAlerts = true;
            alertsBox.innerHTML += `<p class="alert-item"><i class="fa-solid fa-triangle-exclamation var(--warning)"></i> <strong>${c.category_name}</strong> budget almost exhausted (${Math.round(spentPct)}% spent).</p>`;
        }
    });

    if (hasAlerts) {
        alertsBox.classList.remove("hidden");
    } else {
        alertsBox.classList.add("hidden");
    }
}

// Render dynamic categories progress list
function renderCategories(cats) {
    const catList = document.getElementById("categories-list");
    catList.innerHTML = "";

    cats.forEach(c => {
        const spent = c.allocated_amount - c.remaining_amount;
        const progress = Math.min((spent / c.allocated_amount) * 100, 100);
        
        // Icon mapping helper
        let iconClass = "fa-tag";
        if (c.icon === "food") iconClass = "fa-utensils";
        else if (c.icon === "home") iconClass = "fa-house";
        else if (c.icon === "hospital") iconClass = "fa-house-medical";
        else if (c.icon === "family") iconClass = "fa-users";
        else if (c.icon === "shopping") iconClass = "fa-bag-shopping";
        else if (c.icon === "fuel") iconClass = "fa-gas-pump";

        // Progress color logic
        let barColor = c.color;
        if (progress >= 100) {
            barColor = "var(--danger)";
        } else if (progress >= 80) {
            barColor = "var(--warning)";
        }

        const div = document.createElement("div");
        div.className = "category-item";
        div.innerHTML = `
            <div class="cat-icon-container" style="background-color: ${c.color}22; color: ${c.color}">
                <i class="fa-solid ${iconClass}"></i>
            </div>
            <div class="cat-info">
                <div class="cat-info-header">
                    <span>${c.category_name}</span>
                    <span class="remaining" style="color: ${c.remaining_amount <= 0 ? 'var(--danger)' : 'inherit'}">₹${Math.round(c.remaining_amount)} left</span>
                </div>
                <div class="progress-track">
                    <div class="progress-fill" style="width: ${progress}%; background-color: ${barColor}"></div>
                </div>
                <span class="cat-spent-text">Spent: ₹${Math.round(spent)} of ₹${Math.round(c.allocated_amount)}</span>
            </div>
        `;
        
        div.addEventListener("click", () => {
            activeCategoryId = c.id;
            document.getElementById("details-cat-title").innerText = c.category_name;
            document.getElementById("detail-allocated").innerText = `₹${Math.round(c.allocated_amount)}`;
            navigateTo("categoryDetails");
            loadCategoryDetails(c.id, c.allocated_amount);
        });

        catList.appendChild(div);
    });
}

// Render dynamic AI Insights
function renderInsights(ins) {
    const insightsContainer = document.getElementById("ai-insights-container");
    const insightsList = document.getElementById("ai-insights-list");
    insightsList.innerHTML = "";

    if (ins && ins.length > 0) {
        ins.forEach(insight => {
            const li = document.createElement("li");
            li.innerHTML = insight;
            insightsList.appendChild(li);
        });
        insightsContainer.classList.remove("hidden");
    } else {
        insightsContainer.classList.add("hidden");
    }
}

// Nav buttons click
document.getElementById("action-add-category").addEventListener("click", () => navigateTo("addCategory"));
document.getElementById("action-pay").addEventListener("click", () => navigateTo("payment"));
document.getElementById("action-reports").addEventListener("click", () => navigateTo("reports"));

// Back buttons click
document.getElementById("back-from-add-cat").addEventListener("click", () => navigateTo("dashboard"));
document.getElementById("back-from-cat-details").addEventListener("click", () => navigateTo("dashboard"));
document.getElementById("back-from-payment").addEventListener("click", () => navigateTo("dashboard"));
document.getElementById("back-from-reports").addEventListener("click", () => navigateTo("dashboard"));

// ==================== ADD CATEGORY FLOW ====================

// Color selectors
const colorDots = document.querySelectorAll(".color-dot");
colorDots.forEach(dot => {
    dot.addEventListener("click", () => {
        colorDots.forEach(d => d.classList.remove("active"));
        dot.classList.add("active");
    });
});

// Icon selectors
const iconDots = document.querySelectorAll(".icon-dot");
iconDots.forEach(dot => {
    dot.addEventListener("click", () => {
        iconDots.forEach(d => d.classList.remove("active"));
        dot.classList.add("active");
    });
});

// Add Category Save Button
document.getElementById("btn-save-category").addEventListener("click", async () => {
    const name = document.getElementById("cat-name").value;
    const allocated = parseFloat(document.getElementById("cat-allocated").value);
    const selectedColorDot = document.querySelector(".color-dot.active");
    const selectedIconDot = document.querySelector(".icon-dot.active");
    const errorEl = document.getElementById("add-cat-error");

    if (!name || !allocated || allocated <= 0) {
        errorEl.innerText = "Please fill in all valid fields.";
        errorEl.classList.remove("hidden");
        return;
    }

    errorEl.classList.add("hidden");

    try {
        await apiCall("category", "POST", {
            budget_id: activeBudget.id,
            category_name: name.trim(),
            allocated_amount: allocated,
            color: selectedColorDot.dataset.color,
            icon: selectedIconDot.dataset.icon
        });
        
        // Reset fields
        document.getElementById("cat-name").value = "";
        document.getElementById("cat-allocated").value = "";
        
        navigateTo("dashboard");
    } catch (e) {
        errorEl.innerText = e.message || "Failed to create category.";
        errorEl.classList.remove("hidden");
    }
});

// ==================== CATEGORY DETAILS FLOW ====================

async function loadCategoryDetails(catId, allocatedAmt) {
    const transactionsList = document.getElementById("transactions-list");
    transactionsList.innerHTML = "<div class='text-muted text-center'>Loading transactions...</div>";

    try {
        // Fetch categories to calculate remaining
        const catsRes = await apiCall(`categories?budget_id=${activeBudget.id}`);
        const matchingCat = catsRes.find(c => c.id === catId);
        
        if (matchingCat) {
            const spent = matchingCat.allocated_amount - matchingCat.remaining_amount;
            document.getElementById("detail-allocated").innerText = `₹${Math.round(matchingCat.allocated_amount)}`;
            document.getElementById("detail-spent").innerText = `₹${Math.round(spent)}`;
            document.getElementById("detail-remaining").innerText = `₹${Math.round(matchingCat.remaining_amount)}`;
        }

        // Fetch expenses
        const expenses = await apiCall(`expenses?budget_id=${activeBudget.id}`);
        const catExpenses = expenses.filter(e => e.category_id === catId);

        transactionsList.innerHTML = "";
        if (catExpenses.length === 0) {
            transactionsList.innerHTML = "<div class='card text-muted text-center'>No transactions yet.</div>";
            return;
        }

        catExpenses.forEach(tx => {
            const div = document.createElement("div");
            div.className = "tx-card";
            div.innerHTML = `
                <div class="tx-main">
                    <h5>${tx.merchant}</h5>
                    ${tx.remarks ? `<span class="remarks">${tx.remarks}</span>` : ""}
                    <span class="date">${tx.date}</span>
                </div>
                <div class="tx-side">
                    <span class="amt">₹${Math.round(tx.amount)}</span>
                    <button class="delete-tx-btn" data-id="${tx.id}"><i class="fa-solid fa-circle-xmark"></i></button>
                </div>
            `;

            // Delete transaction listener
            div.querySelector(".delete-tx-btn").addEventListener("click", async () => {
                if (confirm("Are you sure you want to delete this expense and restore category budget?")) {
                    try {
                        await apiCall(`expense/${tx.id}`, "DELETE");
                        loadCategoryDetails(catId, allocatedAmt);
                    } catch (e) {
                        alert(e.message);
                    }
                }
            });

            transactionsList.appendChild(div);
        });
    } catch (e) {
        transactionsList.innerHTML = `<div class='error-text'>Failed to load transactions: ${e.message}</div>`;
    }
}

// Delete Category Action
document.getElementById("btn-delete-category").addEventListener("click", async () => {
    if (confirm("Are you sure you want to delete this category? All its transactions will be deleted.")) {
        try {
            await apiCall(`category/${activeCategoryId}`, "DELETE");
            navigateTo("dashboard");
        } catch (e) {
            alert(e.message);
        }
    }
});

// ==================== PAYMENT FLOW ====================

function setupPaymentScreen() {
    const catSelect = document.getElementById("pay-category");
    catSelect.innerHTML = "";
    
    document.getElementById("pay-amount").value = "";
    document.getElementById("pay-merchant").value = "";
    document.getElementById("pay-remarks").value = "";
    document.getElementById("ai-suggestion").classList.add("hidden");
    document.getElementById("payment-error").classList.add("hidden");

    if (categories.length === 0) {
        catSelect.innerHTML = "<option value=''>Please add a category first</option>";
        return;
    }

    categories.forEach(c => {
        catSelect.innerHTML += `<option value="${c.id}">${c.category_name} (₹${Math.round(c.remaining_amount)} left)</option>`;
    });
}

// Local AI categorization patterns
function localClassify(merchant) {
    const m = merchant.toLowerCase();
    if (m.includes("swiggy") || m.includes("zomato") || m.includes("eat") || m.includes("restaurant") || m.includes("food") || m.includes("cafe") || m.includes("starbucks") || m.includes("pizza") || m.includes("burger") || m.includes("kfc") || m.includes("mcdonald") || m.includes("instamart") || m.includes("blinkit") || m.includes("zepto") || m.includes("grocery")) return "Food";
    if (m.includes("apollo") || m.includes("clinic") || m.includes("medplus") || m.includes("hospital") || m.includes("pharmacy") || m.includes("pharma") || m.includes("doctor") || m.includes("medicine") || m.includes("health") || m.includes("diagnostic") || m.includes("lab")) return "Hospital";
    if (m.includes("rent") || m.includes("landlord") || m.includes("pg") || m.includes("flat") || m.includes("lease") || m.includes("house")) return "Rent";
    if (m.includes("amazon") || m.includes("flipkart") || m.includes("shopping") || m.includes("myntra") || m.includes("ajio") || m.includes("zara") || m.includes("h&m") || m.includes("clothing")) return "Shopping";
    if (m.includes("indian oil") || m.includes("hp petrol") || m.includes("shell") || m.includes("fuel") || m.includes("cng") || m.includes("gas") || m.includes("petrol") || m.includes("station") || m.includes("bharat petroleum")) return "Fuel";
    if (m.includes("school") || m.includes("tuition") || m.includes("milk") || m.includes("grocery") || m.includes("supermarket") || m.includes("kids") || m.includes("parent") || m.includes("gift") || m.includes("transfer") || m.includes("cash")) return "Family";
    return null;
}

// Auto classify on typing merchant
document.getElementById("pay-merchant").addEventListener("input", async (e) => {
    const merchantVal = e.target.value;
    const aiText = document.getElementById("ai-suggestion-text");
    const aiBadge = document.getElementById("ai-suggestion");
    const catSelect = document.getElementById("pay-category");

    if (!merchantVal.trim()) {
        aiBadge.classList.add("hidden");
        return;
    }

    // 1. Local check
    const localMatch = localClassify(merchantVal);
    if (localMatch) {
        aiText.innerText = `AI Suggestion: ${localMatch}`;
        aiBadge.classList.remove("hidden");
        selectCategoryByName(localMatch, catSelect);
    }

    // 2. Debounced server check
    clearTimeout(this.aiTimeout);
    this.aiTimeout = setTimeout(async () => {
        try {
            const res = await apiCall("ai/classify", "POST", { merchant: merchantVal });
            if (res.confidence >= 0.8) {
                aiText.innerText = `AI Suggestion: ${res.category_name}`;
                aiBadge.classList.remove("hidden");
                selectCategoryByName(res.category_name, catSelect);
            }
        } catch (err) {
            // Silence classification errors
        }
    }, 500);
});

function selectCategoryByName(name, selectEl) {
    for (let i = 0; i < selectEl.options.length; i++) {
        const optionText = selectEl.options[i].text.toLowerCase();
        if (optionText.startsWith(name.toLowerCase())) {
            selectEl.selectedIndex = i;
            break;
        }
    }
}

// Modal control variables
const successModal = document.getElementById("payment-success-modal");
let paymentToProcess = null;

// Pay / Submit Expense Check
document.getElementById("btn-pay").addEventListener("click", async () => {
    const amt = parseFloat(document.getElementById("pay-amount").value);
    const merchantVal = document.getElementById("pay-merchant").value;
    const catId = parseInt(document.getElementById("pay-category").value);
    const remarksVal = document.getElementById("pay-remarks").value;
    const errorEl = document.getElementById("payment-error");
    const errorText = document.getElementById("payment-error-text");

    if (!amt || amt <= 0 || !merchantVal || !catId) {
        alert("Please fill in all valid details.");
        return;
    }

    errorEl.classList.add("hidden");

    try {
        const today = new Date();
        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');
        const dateStr = `${yyyy}-${mm}-${dd}`;

        const reqBody = {
            category_id: catId,
            amount: amt,
            merchant: merchantVal.trim(),
            date: dateStr,
            remarks: remarksVal.trim() || null
        };

        // Create transaction call
        const res = await apiCall("expense", "POST", reqBody);
        
        // Show success modal
        const selectedCat = categories.find(c => c.id === catId);
        const remAfter = selectedCat.remaining_amount - amt;
        
        document.getElementById("success-modal-desc").innerText = `The transaction of ₹${amt.toLocaleString('en-IN')} to '${merchantVal}' matches your budget.`;
        document.getElementById("success-modal-remaining").innerText = `Remaining in category after payment: ₹${Math.round(remAfter)}`;
        
        paymentToProcess = amt;
        successModal.classList.remove("hidden");
    } catch (e) {
        // Display budget exceeded or server error
        errorText.innerText = e.message || "Budget Exceeded. You do not have enough funds.";
        errorEl.classList.remove("hidden");
    }
});

// Launch PhonePe / UPI Intent URL
document.getElementById("btn-phonepe").addEventListener("click", () => {
    successModal.classList.add("hidden");
    const upiUri = `upi://pay?pa=veera@ybl&pn=SmartPayTracker&am=${paymentToProcess}&cu=INR`;
    window.open(upiUri, "_blank");
    navigateTo("dashboard");
});

document.getElementById("btn-modal-close").addEventListener("click", () => {
    successModal.classList.add("hidden");
    navigateTo("dashboard");
});

// ==================== REPORTS FLOW (CANVAS DRAWING) ====================

async function loadReports() {
    try {
        const today = new Date();
        const month = today.getMonth() + 1;
        const year = today.getFullYear();
        
        const report = await apiCall(`reports/monthly?month=${month}&year=${year}`);
        
        // Set Center statistics values
        document.getElementById("chart-total-spent").innerText = `₹${Math.round(report.total_spent)}`;
        document.getElementById("chart-total-budget").innerText = `of ₹${Math.round(report.total_budget)}`;

        drawDonutChart(report.categories);
        renderBreakdownList(report.categories);
    } catch (e) {
        alert("Failed to load reports: " + e.message);
    }
}

// Drawing Donut Chart dynamically using Canvas API
function drawDonutChart(cats) {
    const canvas = document.getElementById("donut-chart");
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const activeCats = cats.filter(c => c.spent_amount > 0);
    const totalSpent = activeCats.reduce((sum, c) => sum + c.spent_amount, 0);

    if (totalSpent === 0) {
        // Draw empty gray ring
        ctx.beginPath();
        ctx.arc(100, 100, 75, 0, 2 * Math.PI);
        ctx.strokeStyle = "#2A2B36";
        ctx.lineWidth = 16;
        ctx.stroke();
        return;
    }

    let startAngle = -0.5 * Math.PI; // Start from top 12 o'clock

    activeCats.forEach(c => {
        const sweepAngle = (c.spent_amount / totalSpent) * 2 * Math.PI;
        
        ctx.beginPath();
        ctx.arc(100, 100, 75, startAngle, startAngle + sweepAngle);
        ctx.strokeStyle = c.color;
        ctx.lineWidth = 16;
        ctx.lineCap = "round";
        ctx.stroke();
        
        startAngle += sweepAngle;
    });
}

function renderBreakdownList(cats) {
    const breakdownList = document.getElementById("reports-breakdown");
    breakdownList.innerHTML = "";

    const activeCats = cats.filter(c => c.spent_amount > 0);

    if (activeCats.length === 0) {
        breakdownList.innerHTML = "<div class='card text-muted text-center'>No expenses recorded yet.</div>";
        return;
    }

    // Sort by largest spending
    activeCats.sort((a, b) => b.spent_amount - a.spent_amount);

    activeCats.forEach(c => {
        const div = document.createElement("div");
        div.className = "breakdown-row";
        div.innerHTML = `
            <div class="breakdown-left">
                <span class="color-bullet" style="background-color: ${c.color}"></span>
                <div class="breakdown-title">
                    <h5>${c.category_name}</h5>
                    <span>Spent ₹${Math.round(c.spent_amount)} of ₹${Math.round(c.allocated_amount)}</span>
                </div>
            </div>
            <span class="breakdown-pct" style="color: ${c.color}">${Math.round(c.percentage)}%</span>
        `;
        breakdownList.appendChild(div);
    });
}
