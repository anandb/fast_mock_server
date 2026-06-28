#!/bin/sh
#
# Hybrid executable JAR launcher.
# This script is prepended to the JAR file at build time.
# Java ignores leading garbage in JARs (ZIP EOCD is at end of file).
#
exec java $JAVA_OPTS -jar "$0" "$@"
