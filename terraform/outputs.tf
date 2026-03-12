output "api_url" {
  description = "Invoke URL for the deployed HTTP API"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "lambda_function_name" {
  description = "Lambda function name"
  value       = aws_lambda_function.backend.function_name
}

output "ecr_repository_url" {
  description = "ECR repository URL for Lambda image deployments"
  value       = data.aws_ecr_repository.lambda.repository_url
}
