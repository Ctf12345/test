#! /bin/sh
OUT_DIR=../java/com/ctf/autotest/Xml
TARGET_DIR=../java
PKG_NAME=com.ctf.autotest.Xml
XSD_FILE=../resources/TestPlan.xsd

rm -rf $OUT_DIR
xjc -Xlocator -d $TARGET_DIR -p $PKG_NAME -encoding UTF-8 -readOnly $XSD_FILE
