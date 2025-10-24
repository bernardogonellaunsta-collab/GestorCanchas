
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/src"
javac com/gestor/*.java
java com.gestor.GestorDeportivoApp
