# Smart Billing & Inventory Management System

A comprehensive web-based application for managing billing, inventory, and business analytics. This system features a robust backend, a modern frontend, and an analytics engine to provide actionable insights.

## Tech Stack

*   **Frontend**:
    *   React.js
    *   Vite
    *   TailwindCSS
    *   Lucide React (Icons)
*   **Backend**:
    *   Java Spring Boot
    *   Maven
*   **Analytics**:
    *   Python (Flask/FastAPI)
    *   Pandas / SciPy (for data processing)

## Prerequisites

Ensure you have the following installed on your system:

*   **Java JDK** (version 17 or higher recommended)
*   **Node.js** (version 16 or higher) & npm
*   **Python** (version 3.8 or higher) & pip

## Installation & Setup

### 1. Backend Setup
The backend is a Spring Boot application.
```bash
cd backend
./mvnw clean install
```
*Note: Make sure `mvnw` is executable (`chmod +x mvnw` on Mac/Linux).*

### 2. Frontend Setup
The frontend is built with React and Vite.
```bash
cd frontend
npm install
```

### 3. Analytics Setup
The analytics service runs on Python.
```bash
cd analytics
# It is recommended to create a virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Running the Application

A convenience script is provided to start all services simultaneously.

```bash
# From the root directory
./start-all-services.sh
```

### Manual Start
If you prefer to start services individually:

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

**Analytics:**
```bash
cd analytics
source venv/bin/activate
python app.py
```

**Frontend:**
```bash
cd frontend
npm run dev
```

## Accessing the Application

Once all services are running, you can access them at:

*   **Frontend (User Interface)**: [http://localhost:5173](http://localhost:5173)
*   **Backend API**: [http://localhost:8080](http://localhost:8080)
*   **Analytics Service**: [http://localhost:5001](http://localhost:5001)

## Project Structure

*   `backend/`: Source code for the Java Spring Boot application.
*   `frontend/`: Source code for the React frontend application.
*   `analytics/`: Python scripts and API for data analytics.
*   `start-all-services.sh`: Helper script to launch the full stack.

---
*Generated for the Smart Billing Inventory Management System*
