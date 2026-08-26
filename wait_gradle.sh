while kill -0 $(ps -ef | awk '/gradle :app:compileDebugKotlin/ && !/awk/ {print $2}') 2>/dev/null; do
    sleep 1
done
echo "Gradle compilation finished"
