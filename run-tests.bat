@echo off
REM Windows용 간단한 테스트 실행 스크립트

echo ========================================
echo MwManger Agent Test Runner
echo ========================================
echo.

REM Java 버전 확인
echo [1/3] Checking Java version...
java -version
if errorlevel 1 (
    echo ERROR: Java not found! Please install JDK 1.8 or higher.
    exit /b 1
)
echo.

REM Maven 확인
echo [2/3] Checking Maven...
where mvn >nul 2>&1
if errorlevel 1 (
    echo Maven not found. Please install Maven to run tests.
    echo Download from: https://maven.apache.org/download.cgi
    echo.
    echo Alternative: Use an IDE like IntelliJ IDEA or Eclipse
    exit /b 1
)
echo Maven found!
echo.

REM 테스트 실행
echo [3/3] Running tests...
echo ========================================
mvn test
echo ========================================
echo.

if errorlevel 1 (
    echo TESTS FAILED!
    echo Check target/surefire-reports/ for details
) else (
    echo ALL TESTS PASSED!
    echo Test reports: target/surefire-reports/
)

pause
