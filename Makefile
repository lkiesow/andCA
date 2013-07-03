it:
	ant debug

install:
	adb uninstall de.larskiesow.andca
	adb install bin/AndCA-debug.apk

start: install
	adb shell am start de.larskiesow.andca/.AndCA

debug: start
	adb logcat
