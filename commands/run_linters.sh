#!/bin/bash

# The only change was to rename pmd and checkstyle output file to main.xml, because we can't change it in build.gradle

source /commands/test_helper.sh
source /commands/utils.sh

APP_NAME="fury_"$APPLICATION
TASKS=$(./gradlew tasks --all)
file=""


if [[ $TASKS == *"pmdMain"* && $TASKS == *"checkstyleMain"* ]]; then
    ./gradlew clean pmdMain
    mv build/reports/pmd/main.xml build/reports/pmd/${APP_NAME}-pmd-main.xml
    file="build/reports/pmd/${APP_NAME}-pmd-main.xml"
    post_to_report_uploader "${file}"
elif [[ $TASKS == *"checkstyleMain"* ]]; then
    ./gradlew clean checkstyleMain
    mv build/reports/checkstyle/main.xml build/reports/checkstyle/${APP_NAME}-checkstyle-main.xml
    file="build/reports/checkstyle/${APP_NAME}-checkstyle-main.xml"
    post_to_report_uploader "${file}"
elif [[ $TASKS  == *"pmdMain"* ]]; then
    ./gradlew clean pmdMain
    mv build/reports/pmd/main.xml build/reports/pmd/${APP_NAME}-pmd-main.xml
    file="build/reports/pmd/${APP_NAME}-pmd-main.xml"
    post_to_report_uploader "${file}"
else
    prefix_logger "LINTER" echo "Linter not configured"
fi
