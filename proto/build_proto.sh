#!/usr/bin/env bash
################################################################################
# build_proto.sh - builds the proto files in this directory and outputs them to
# the project source tree.
################################################################################

cd "$(cd "$(dirname "$0")" && pwd)"
protoc ./*.proto --java_out ../src/main/java

