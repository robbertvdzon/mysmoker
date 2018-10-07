#!/bin/bash
set -eu

source env.sh

echo "Build jar file"
mvn package -Dskiptests

echo "Update Lambda function"
aws lambda update-function-code --function-name "$LAMBDA_NAME" --zip-file fileb://target/mysmoker-1.0-SNAPSHOT.jar
