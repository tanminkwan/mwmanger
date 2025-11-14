#!/bin/bash
# Linux/Mac용 테스트 실행 스크립트

echo "========================================"
echo "MwManger Agent Test Runner"
echo "========================================"
echo ""

# Java 버전 확인
echo "[1/3] Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found! Please install JDK 1.8 or higher."
    exit 1
fi
java -version
echo ""

# Maven 확인
echo "[2/3] Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Please install Maven to run tests."
    echo "  - Ubuntu/Debian: sudo apt-get install maven"
    echo "  - Mac: brew install maven"
    echo "  - Or download from: https://maven.apache.org/download.cgi"
    echo ""
    echo "Alternative: Use an IDE like IntelliJ IDEA or VS Code"
    exit 1
fi
echo "Maven found!"
echo ""

# 테스트 실행
echo "[3/3] Running tests..."
echo "========================================"
mvn test
TEST_RESULT=$?
echo "========================================"
echo ""

if [ $TEST_RESULT -ne 0 ]; then
    echo "TESTS FAILED!"
    echo "Check target/surefire-reports/ for details"
    exit 1
else
    echo "ALL TESTS PASSED!"
    echo "Test reports: target/surefire-reports/"
fi
