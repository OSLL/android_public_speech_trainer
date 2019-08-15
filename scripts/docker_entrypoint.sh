ls -la ./
ls -la app/
echo $USER
chown -R $USER:$USER ./
mv "app/google-services-debug.json" "app/google-services.json"
./gradlew assembleDebug --stacktrace
# ./gradlew assembleAndroidTest
# ./gradlew lintDebug
