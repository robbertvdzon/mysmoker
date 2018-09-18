# serverless sample

PREREQUISITES
You must have a basic role 'lambda_basic_execution' defined - this is what is created
the first time you create a Lambda function in the web console

Scripts and code based on:
https://aws.amazon.com/blogs/opensource/java-apis-aws-lambda/
and
https://gist.github.com/mspeer383/fbb5f2e283d98be3920bd97607666bfd

# Create the AWS Lambda and API Gateway
./aws_create
The URL to be used will be printed by this command.
Check the API Gateway - resource - stages - api to find this URL afterwards

# Redeploy the code 
./aws_redeploy

# Remove the Lambda and API Gateway 
./aws_delete

