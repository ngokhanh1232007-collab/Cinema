#!/bin/bash
# =====================================================================
# Script khởi chạy Giao diện Web (WebServer) - Cinema Booking System
# =====================================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR" || exit 1
BIN_DIR="$PROJECT_DIR/bin"
MYSQL_JAR="$HOME/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"

if [ ! -f "$MYSQL_JAR" ]; then
    MYSQL_JAR=$(find ~/.m2 /usr/share/java -name "mysql-connector-j-*.jar" 2>/dev/null | head -n 1)
fi

echo "======================================================================"
echo " 🌐 KHỞI ĐỘNG CINEMA BOOKING WEB SERVER (CỔNG 8080)"
echo "======================================================================"

mkdir -p "$BIN_DIR"

echo "⏳ Đang biên dịch toàn bộ mã nguồn Java..."
javac -encoding UTF-8 \
      -cp "$MYSQL_JAR:src/main/java" \
      -d "$BIN_DIR" \
      src/main/java/com/cinema/utils/*.java \
      src/main/java/com/cinema/model/*.java \
      src/main/java/com/cinema/repository/*.java \
      src/main/java/com/cinema/service/*.java \
      src/main/java/com/cinema/main/*.java

if [ $? -ne 0 ]; then
    echo "❌ Lỗi biên dịch Java! Vui lòng kiểm tra lại mã nguồn."
    exit 1
fi

echo "✅ Biên dịch thành công 100%!"
echo "🚀 Đang khởi chạy WebServer tại: http://localhost:8080/"
echo "💡 Nhấn [Ctrl + C] bất cứ lúc nào để dừng Web Server."
echo "----------------------------------------------------------------------"

# Tự động mở trình duyệt web sau 1.2 giây
(sleep 1.2 && (xdg-open "http://localhost:8080" 2>/dev/null || open "http://localhost:8080" 2>/dev/null || sensible-browser "http://localhost:8080" 2>/dev/null || echo "👉 Mở trình duyệt và truy cập: http://localhost:8080")) &

java -Dfile.encoding=UTF-8 \
     -cp "$MYSQL_JAR:$BIN_DIR:src/main/resources" \
     com.cinema.main.WebServer
