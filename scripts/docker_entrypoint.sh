ls -la ./
ls -la app/
whoami
chown -R `whoami`:`whoami` ./
mv "app/google-services-debug.json" "app/google-services.json"
./gradlew lintDebug
./gradlew assembleDebug --stacktrace
./gradlew assembleAndroidTest
./gradlew clean test --info
