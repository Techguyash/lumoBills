# LumoBills - Business Management & Billing Platform

LumoBills is a comprehensive, enterprise-level business management solution designed for retail and stock-based service businesses. It features a modern, Verizon-inspired high-contrast theme, intelligent billing flows, and robust inventory tracking.

---

## 🎨 Design & Aesthetics
LumoBills uses a premium **Verizon-Inspired Design System**:
*   **Vibrant Color Palette**: High-contrast Red (`#d52b1e`), White, and Deep Black.
*   **Dynamic Theme Toggling**: Sticky Light/Dark mode support.
*   **AESTHETICS ARE PRORITY**: Smooth transitions, glassmorphism elements, and modern typography (Outfit/Inter).

---

## 🚀 Core Features

### 1. **Intelligent Billing Dashboard**
*   **Dynamic Workspace**: Modern billing form with a resizable, collapsible "Recent Invoices" sidebar.
*   **One-Click Toggle**: Switch between a focused full-width billing form and a dual-view split layout for history tracking.
*   **Real-time Calculations**: Automatic calculation of Subtotal, Tax (GST), and Discounts.

### 2. **Inventory & Stock Management**
*   **Product Tracking**: Manage products, selling prices, and categories.
*   **Low Stock Alerts**: Visual badges and dashboard notifications for products hitting reorder levels.
*   **Stock History**: Detailed logs for every stock movement (Sales, Purchases, Adjustments).

### 3. **Purchasing & Financials**
*   **Purchase Tracking**: Record raw material or stock-refill purchases from sellers.
*   **Ledger View**: Comprehensive financial statement tracking all income and expenses.
*   **Business Reports**: Sales performance, stock history, and product profitability reports.

### 4. **Modern Communication**
*   **PDF Generation**: Generate professional invoices with company branding.
*   **Email Integration**: Send invoices directly to customers via email.

---

## 🛠 Technical Documentation

### **Technological Stack**
*   **Backend**: Java 17, Spring Boot 3.x
*   **Frontend**: Vaadin Flow 24 (Web Components, TypeScript/Vite)
*   **Security**: Spring Security (Role-based access: ADMIN / USER)
*   **Database**: 
    *   **Local**: H2 In-memory Database (Console at `/h2-console`)
    *   **Production**: MySQL Compatible
*   **Reporting**: Apache POI (Excel) & OpenPDF (PDF Generation)
*   **Styling**: Pure Vanilla CSS following the Verizon Design System.

### **Getting Started**
1.  **Clone the Repository**:
    ```bash
    git clone <repository-url>
    cd lumoBills
    ```
2.  **Prerequisites**:
    *   JDK 17 or higher
    *   Maven 3.8+
    *   Node.js (for Vaadin frontend compilation)
3.  **Run in Development Mode**:
    ```bash
    mvn spring-boot:run
    ```
    Access the app at `http://localhost:8080`.
4.  **Build for Production**:
    ```bash
    mvn clean package -Pproduction
    ```
    This will compile the optimized frontend bundle and generate a JAR in the `target/` directory.

---

## 📖 User Usage Guide

### **1. Navigation**
Use the sidebar on the left to navigate between modules.
*   **Dashboard**: Overview of business health and alerts.
*   **Stock**: Manage your products and inventory levels.
*   **Billing**: Create new sales invoices.
*   **Invoices**: Search, view details, print, or email past invoices.
*   **Purchase**: Record stock refills from suppliers.
*   **Reports**: Export data to Excel for analysis.
*   **Ledger**: Track your net balance and cash flow.
*   **Admin**: Configure company details, GST, and user accounts.

### **2. Creating a Bill**
1.  Navigate to **Billing**.
2.  Select a **Customer** (or add a new one using the "+" icon).
3.  Add products to the list; quantities and totals update in real-time.
4.  Click **Save Invoice** to finalize the sale and generate a PDF.
5.  Need to see previous sales? Click **Recent Invoices** to open the resizable history sidebar.

### **3. Managing Stock**
1.  Go to **Stock Management**.
2.  Use the "Add Product" button to enter new inventory.
3.  Define **Reorder Levels**; the system will warn you on the Dashboard when stocks are low.
4.  Track every change in the **Reports > Stock History**.

### **4. System Configuration (ADMIN)**
1.  Navigate to **Admin Settings**.
2.  Update **Business Configuration** with your GST number, address, and billing terms.
3.  Configure **Currency** (defaults to INR, but supports USD, EUR, etc.).
4.  Manage **User Accounts** and their access permissions for different modules.

---

## 🔧 Maintenance
*   **Clean and Build**: After major CSS or frontend changes, run `mvn clean` before starting to clear the Vite cache.
*   **Version Control**: This project follows Git standard practices for versioning.

Created with 🚀 using **Antigravity AI**.
