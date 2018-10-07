#!/bin/bash
set -eu

source env.sh

echo "Build jar file"
mvn package -Dskiptests

# TODO - if doesn't exist, create it
echo "Find role, account and region"
LAMBDA_ROLE_ARN=$(aws iam get-role --role-name lambda_basic_execution --query Role.Arn --output text)
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=$(aws configure get region)
STAGE_NAME="api"

echo "Creating Lambda function"
aws lambda create-function \
	--function-name "$LAMBDA_NAME" \
	--runtime java8 \
	--role "$LAMBDA_ROLE_ARN" \
	--timeout 15 \
	--handler "$HANDLER_FUNCTION" \
	--zip-file fileb://target/"$JAR_FILENAME" --memory-size 512

echo "Creating API $LAMBDA_NAME..."
REST_API_ID=$(aws apigateway create-rest-api \
	--name "$LAMBDA_NAME" \
	--query id --output text)
echo "API created with ID $REST_API_ID"

ROOT_RESOURCE_ID=$(aws apigateway get-resources \
	--rest-api-id "$REST_API_ID" \
	--query items[0].id --output text)
echo "Root resource ID $ROOT_RESOURCE_ID"

echo "Creating proxy resouce..."
PROXY_RESOURCE_ID=$(aws apigateway create-resource \
	--rest-api-id "$REST_API_ID" \
	--parent-id "$ROOT_RESOURCE_ID" \
	--path-part "{proxy+}" \
	--query id --output text)
echo "Proxy resource created with ID $PROXY_RESOURCE_ID"

setupApiMethod () {
	local RESOURCE_ID=$1
	local RESOURCE_DESCRIPTION=$2

	echo "Creating ANY method on $RESOURCE_DESCRIPTION resource..."
	aws apigateway put-method \
		--rest-api-id "$REST_API_ID" \
		--resource-id "$RESOURCE_ID" \
		--http-method ANY \
		--authorization-type NONE \
		--request-parameters method.request.path.proxy=true

	echo "Integrating $RESOURCE_DESCRIPTION resource with Lambda function '$LAMBDA_NAME'..."
	aws apigateway put-integration \
		--rest-api-id "$REST_API_ID" \
		--resource-id "$RESOURCE_ID" \
		--http-method ANY \
		--integration-http-method POST \
		--type AWS_PROXY \
		--content-handling CONVERT_TO_TEXT \
		--cache-key-parameters "method.request.path.proxy" \
		--uri "arn:aws:apigateway:$AWS_REGION:lambda:path/2015-03-31/functions/arn:aws:lambda:$AWS_REGION:$AWS_ACCOUNT:function:$LAMBDA_NAME/invocations"

	echo "Completing $RESOURCE_DESCRIPTION resource integration by configuring method response..."
	aws apigateway put-method-response \
		--rest-api-id "$REST_API_ID" \
		--resource-id "$RESOURCE_ID" \
		--http-method ANY \
		--status-code 200 \
		--response-models "{\"application/json\": \"Empty\"}"
}

setupApiMethod "$ROOT_RESOURCE_ID" "root"
setupApiMethod "$PROXY_RESOURCE_ID" "proxy"

echo "Deploying API to stage '$STAGE_NAME'..."
aws apigateway create-deployment \
	--rest-api-id "$REST_API_ID" \
	--stage-name "$STAGE_NAME"

echo "Updating Lambda to be executable by API..."
aws lambda add-permission \
	--function-name "$LAMBDA_NAME" \
	--statement-id "api-$REST_API_ID-$ROOT_RESOURCE_ID" \
	--action lambda:InvokeFunction \
	--principal apigateway.amazonaws.com \
	--source-arn "arn:aws:execute-api:$AWS_REGION:$AWS_ACCOUNT:$REST_API_ID/*/*/*"

echo
echo "*** API Setup Complete!"
echo "API $LAMBDA_NAME (ID $REST_API_ID) is available at https://$REST_API_ID.execute-api.$AWS_REGION.amazonaws.com/$STAGE_NAME"
echo "Try 'curl'ing it!"
echo