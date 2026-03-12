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

output "web_bucket_name" {
  description = "S3 bucket that stores the React dashboard build"
  value       = aws_s3_bucket.web.bucket
}

output "web_distribution_id" {
  description = "CloudFront distribution ID for the React dashboard"
  value       = aws_cloudfront_distribution.web.id
}

output "web_distribution_domain_name" {
  description = "CloudFront domain name for the React dashboard"
  value       = aws_cloudfront_distribution.web.domain_name
}
