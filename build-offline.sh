#!/bin/bash
# 오프라인 빌드 스크립트
# Maven/Gradle 없이 javac만으로 빌드합니다.

set -e

PROJECT_NAME="mwmanger"
VERSION="0000.0008.0005"
MAIN_CLASS="mwmanger.MwAgent"

echo "========================================="
echo "  MwManger Offline Build"
echo "========================================="
echo "Project: $PROJECT_NAME"
echo "Version: $VERSION"
echo "Main Class: $MAIN_CLASS"
echo ""

# 1. Clean
echo "[1/5] Cleaning build directory..."
rm -rf build/classes build/jar
mkdir -p build/classes build/jar

# 2. Classpath 설정
echo "[2/5] Setting up classpath..."
CLASSPATH=""
for jar in lib/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done
echo "Classpath: $CLASSPATH"

# 3. 컴파일
echo "[3/5] Compiling Java sources..."
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 \
      -source 1.8 \
      -target 1.8 \
      -d build/classes \
      -cp ".$CLASSPATH" \
      @sources.txt

if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed!"
    exit 1
fi

rm sources.txt
echo "Compilation successful!"

# 4. Manifest 생성
echo "[4/5] Creating manifest..."
cat > build/jar/MANIFEST.MF << EOF
Manifest-Version: 1.0
Main-Class: $MAIN_CLASS
Class-Path: $(cd lib && ls *.jar | tr '\n' ' ')
EOF

# 5. JAR 패키징
echo "[5/5] Creating JAR package..."
cd build/classes
jar cvfm "../jar/$PROJECT_NAME-$VERSION.jar" ../jar/MANIFEST.MF .
cd ../..

echo ""
echo "========================================="
echo "  Build Complete!"
echo "========================================="
echo ""
echo "Generated files:"
echo "  - build/jar/$PROJECT_NAME-$VERSION.jar"
echo ""
echo "To run:"
echo "  java -jar build/jar/$PROJECT_NAME-$VERSION.jar"
echo ""
echo "Note: lib/*.jar files must be in the same directory"
