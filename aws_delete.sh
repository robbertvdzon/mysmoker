#!/bin/bash
set -eu

source env.sh

echo "Remove api gateway"
APIID=$(aws apigateway get-rest-apis --query "items[?name=='$LAMBDA_NAME'].id"  --output text)
aws apigateway delete-rest-api --rest-api-id $APIID

echo "Remove function"
aws lambda delete-function --function-name $LAMBDA_NAME