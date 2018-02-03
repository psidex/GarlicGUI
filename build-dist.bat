:: Create jar, get clean settings, create settings dir & bundle into zip
@echo off
echo Cleaning /dist
if exist dist rd /s /q dist
mkdir dist
cd dist
mkdir settings
cd ..
echo Copying jar from /out
cp out/artifacts/GarlicGUI_jar/GarlicGUI.jar dist/GarlicGUI.jar
echo Copying clean settings /settings
cp settings/settings.clean.ser dist/settings
cd dist/settings
cp settings.clean.ser settings.ser
cd ..
echo Zipping:
7z a GarlicGUI GarlicGUI.jar settings/*
echo build-dist Done
cd ..
