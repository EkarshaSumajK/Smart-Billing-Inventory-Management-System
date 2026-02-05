#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Starting Smart Billing Management System${NC}"
echo -e "${BLUE}========================================${NC}"

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Start Backend (Spring Boot)
echo -e "\n${GREEN}[1/3] Starting Backend Service...${NC}"
cd "$SCRIPT_DIR/backend"
mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo -e "Backend started with PID: $BACKEND_PID"

# Start Analytics (Python)
echo -e "\n${GREEN}[2/3] Starting Analytics Service...${NC}"
cd "$SCRIPT_DIR/analytics"
python app.py > analytics.log 2>&1 &
ANALYTICS_PID=$!
echo -e "Analytics started with PID: $ANALYTICS_PID"

# Start Frontend (React/Vite)
echo -e "\n${GREEN}[3/3] Starting Frontend Service...${NC}"
cd "$SCRIPT_DIR/frontend"
node node_modules/vite/bin/vite.js > frontend.log 2>&1 &
FRONTEND_PID=$!
echo -e "Frontend started with PID: $FRONTEND_PID"

# Store PIDs in a file for easy cleanup
echo "$BACKEND_PID" > "$SCRIPT_DIR/.service_pids"
echo "$ANALYTICS_PID" >> "$SCRIPT_DIR/.service_pids"
echo "$FRONTEND_PID" >> "$SCRIPT_DIR/.service_pids"

echo -e "\n${BLUE}========================================${NC}"
echo -e "${GREEN}All services started successfully!${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "\nService PIDs:"
echo -e "  Backend:   $BACKEND_PID"
echo -e "  Analytics: $ANALYTICS_PID"
echo -e "  Frontend:  $FRONTEND_PID"
echo -e "\nLogs are being written to:"
echo -e "  Backend:   $SCRIPT_DIR/backend/backend.log"
echo -e "  Analytics: $SCRIPT_DIR/analytics/analytics.log"
echo -e "  Frontend:  $SCRIPT_DIR/frontend/frontend.log"
echo -e "\n${GREEN}Access your services at:${NC}"
echo -e "  Frontend:  ${BLUE}http://localhost:5173${NC}"
echo -e "  Backend:   ${BLUE}http://localhost:8080${NC}"
echo -e "  Analytics: ${BLUE}http://localhost:5001${NC}"
echo -e "\n${RED}To stop all services, run:${NC}"
echo -e "  kill $BACKEND_PID $ANALYTICS_PID $FRONTEND_PID"
echo -e "\n  Or use: pkill -P $$ (to kill all child processes)"
echo -e "\n${BLUE}========================================${NC}"

# Keep script running and wait for Ctrl+C
echo -e "\nPress ${RED}Ctrl+C${NC} to stop all services..."

# Trap Ctrl+C and cleanup
cleanup() {
    echo -e "\n${RED}Stopping all services...${NC}"
    kill $BACKEND_PID $ANALYTICS_PID $FRONTEND_PID 2>/dev/null
    rm -f "$SCRIPT_DIR/.service_pids"
    echo -e "${GREEN}All services stopped.${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# Wait indefinitely
wait
