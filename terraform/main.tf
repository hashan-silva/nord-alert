terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "remote" {
    hostname     = "app.terraform.io"
    organization = "hashan-silva"

    workspaces {
      prefix = "nord-alert-"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

locals {
  workspace                   = var.workspace_name
  name_prefix                 = "${var.project_name}-${local.workspace}"
  subscription_table_name     = lower("${local.name_prefix}-subscriptions")
  web_bucket_name             = lower("${local.name_prefix}-web")
  web_logs_bucket_name        = lower("${local.name_prefix}-web-logs")
  web_access_logs_bucket_name = lower("${local.name_prefix}-web-access-logs")
}

data "aws_caller_identity" "current" {}

data "aws_ecr_repository" "lambda" {
  name = var.ecr_repository_name
}

resource "aws_ecr_lifecycle_policy" "lambda" {
  repository = data.aws_ecr_repository.lambda.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Retain only the three most recent Lambda images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 3
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

resource "aws_iam_role" "lambda" {
  name = "${local.name_prefix}-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "lambda_app" {
  name = "${local.name_prefix}-lambda-app-policy"
  role = aws_iam_role.lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:Scan",
          "dynamodb:UpdateItem"
        ]
        Resource = aws_dynamodb_table.subscriptions.arn
      },
      {
        Effect = "Allow"
        Action = [
          "ses:CreateEmailIdentity",
          "ses:GetEmailIdentity",
          "ses:SendEmail",
          "ses:SendRawEmail"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role" "apigateway_logs" {
  name = "${local.name_prefix}-apigw-logs-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "apigateway.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "apigateway_logs" {
  role       = aws_iam_role.apigateway_logs.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonAPIGatewayPushToCloudWatchLogs"
}

resource "aws_api_gateway_account" "main" {
  cloudwatch_role_arn = aws_iam_role.apigateway_logs.arn

  depends_on = [aws_iam_role_policy_attachment.apigateway_logs]
}

resource "aws_cloudwatch_log_group" "apigateway" {
  name              = "/aws/apigateway/${local.name_prefix}-http-api"
  retention_in_days = 14
}

resource "aws_lambda_function" "backend" {
  function_name = "${local.name_prefix}-backend"
  package_type  = "Image"
  role          = aws_iam_role.lambda.arn
  image_uri     = var.lambda_image_uri
  timeout       = var.lambda_timeout_seconds
  memory_size   = var.lambda_memory_size
  architectures = [var.lambda_architecture]

  environment {
    variables = {
      SUBSCRIPTION_TABLE_NAME   = aws_dynamodb_table.subscriptions.name
      SUBSCRIPTION_SENDER_EMAIL = var.ses_sender_email
    }
  }

  depends_on = [aws_iam_role_policy_attachment.lambda_basic]
}

resource "aws_lambda_function" "subscription_dispatcher" {
  function_name = "${local.name_prefix}-subscription-dispatcher"
  package_type  = "Image"
  role          = aws_iam_role.lambda.arn
  image_uri     = var.lambda_image_uri
  timeout       = 60
  memory_size   = 1024
  architectures = [var.lambda_architecture]

  image_config {
    command = ["com.hashan0314.nordalert.backend.SubscriptionDispatchLambdaHandler::handleRequest"]
  }

  environment {
    variables = {
      SUBSCRIPTION_TABLE_NAME   = aws_dynamodb_table.subscriptions.name
      SUBSCRIPTION_SENDER_EMAIL = var.ses_sender_email
    }
  }

  depends_on = [aws_iam_role_policy_attachment.lambda_basic]
}

resource "aws_apigatewayv2_api" "backend" {
  name          = "${local.name_prefix}-http-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_headers = ["content-type", "authorization", "x-requested-with"]
    allow_methods = ["GET", "POST", "OPTIONS"]
    allow_origins = [
      "http://localhost:3000",
      "https://${aws_cloudfront_distribution.web.domain_name}"
    ]
    expose_headers    = ["content-type"]
    max_age           = 3600
    allow_credentials = false
  }
}

resource "aws_apigatewayv2_integration" "backend" {
  api_id                 = aws_apigatewayv2_api.backend.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.backend.invoke_arn
  integration_method     = "POST"
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "default" {
  api_id    = aws_apigatewayv2_api.backend.id
  route_key = "$default"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.backend.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.apigateway.arn
    format = jsonencode({
      requestId         = "$context.requestId"
      ip                = "$context.identity.sourceIp"
      requestTime       = "$context.requestTime"
      httpMethod        = "$context.httpMethod"
      routeKey          = "$context.routeKey"
      status            = "$context.status"
      protocol          = "$context.protocol"
      responseLength    = "$context.responseLength"
      integrationError  = "$context.integrationErrorMessage"
      integrationStatus = "$context.integration.status"
    })
  }

  depends_on = [aws_api_gateway_account.main]
}

resource "aws_lambda_permission" "apigw" {
  statement_id  = "AllowExecutionFromApiGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.backend.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.backend.execution_arn}/*/*"
}

resource "aws_dynamodb_table" "subscriptions" {
  name         = local.subscription_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }
}

resource "aws_ses_email_identity" "subscription_sender" {
  email = var.ses_sender_email
}

resource "aws_cloudwatch_event_rule" "subscription_dispatch" {
  name                = "${local.name_prefix}-subscription-dispatch"
  description         = "Scheduled dispatch of NordAlert email subscriptions"
  schedule_expression = var.subscription_dispatch_schedule_expression
}

resource "aws_cloudwatch_event_target" "subscription_dispatch" {
  rule      = aws_cloudwatch_event_rule.subscription_dispatch.name
  target_id = "subscription-dispatcher"
  arn       = aws_lambda_function.subscription_dispatcher.arn
}

resource "aws_lambda_permission" "eventbridge_subscription_dispatch" {
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.subscription_dispatcher.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.subscription_dispatch.arn
}

resource "aws_s3_bucket" "web" {
  bucket = local.web_bucket_name
}

resource "aws_s3_bucket_versioning" "web" {
  bucket = aws_s3_bucket.web.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "web" {
  bucket = aws_s3_bucket.web.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "web" {
  bucket = aws_s3_bucket.web.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket" "web_access_logs" {
  bucket = local.web_access_logs_bucket_name
}

resource "aws_s3_bucket_ownership_controls" "web_access_logs" {
  bucket = aws_s3_bucket.web_access_logs.id

  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "web_access_logs" {
  bucket = aws_s3_bucket.web_access_logs.id
  acl    = "log-delivery-write"

  depends_on = [aws_s3_bucket_ownership_controls.web_access_logs]
}

resource "aws_s3_bucket_server_side_encryption_configuration" "web_access_logs" {
  bucket = aws_s3_bucket.web_access_logs.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "web_access_logs" {
  bucket = aws_s3_bucket.web_access_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_logging" "web" {
  bucket        = aws_s3_bucket.web.id
  target_bucket = aws_s3_bucket.web_access_logs.id
  target_prefix = "web/"
}

resource "aws_s3_bucket" "web_logs" {
  bucket = local.web_logs_bucket_name
}

resource "aws_s3_bucket_ownership_controls" "web_logs" {
  bucket = aws_s3_bucket.web_logs.id

  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "web_logs" {
  bucket = aws_s3_bucket.web_logs.id
  acl    = "log-delivery-write"

  depends_on = [aws_s3_bucket_ownership_controls.web_logs]
}

resource "aws_s3_bucket_server_side_encryption_configuration" "web_logs" {
  bucket = aws_s3_bucket.web_logs.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "web_logs" {
  bucket = aws_s3_bucket.web_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_logging" "web_logs" {
  bucket        = aws_s3_bucket.web_logs.id
  target_bucket = aws_s3_bucket.web_access_logs.id
  target_prefix = "web-logs/"
}

resource "aws_cloudfront_origin_access_control" "web" {
  name                              = "${local.name_prefix}-web-oac"
  description                       = "Origin access control for NordAlert web assets"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "web" {
  enabled             = true
  default_root_object = "index.html"
  price_class         = "PriceClass_100"

  logging_config {
    bucket          = aws_s3_bucket.web_logs.bucket_domain_name
    include_cookies = false
    prefix          = "cloudfront/"
  }

  origin {
    domain_name              = aws_s3_bucket.web.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.web.id
    origin_id                = "web-s3-origin"
  }

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "web-s3-origin"

    cache_policy_id            = "658327ea-f89d-4fab-a63d-7e88639e58f6"
    compress                   = true
    viewer_protocol_policy     = "redirect-to-https"
    response_headers_policy_id = "67f7725c-6f97-4210-82d7-5512b31e9d03"
  }

  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 0
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 0
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

data "aws_iam_policy_document" "web_bucket_policy" {
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.web.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.web.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "web" {
  bucket = aws_s3_bucket.web.id
  policy = data.aws_iam_policy_document.web_bucket_policy.json

  depends_on = [aws_s3_bucket_public_access_block.web]
}
