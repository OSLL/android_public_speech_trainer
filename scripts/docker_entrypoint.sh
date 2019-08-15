ls -la ./
ls -la app/
mv "app/google-services-debug.json" "app/google-services.json"
./gradlew assembleDebug --stacktrace
# ./gradlew assembleAndroidTest
# ./gradlew lintDebug
