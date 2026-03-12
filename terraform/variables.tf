variable "aws_region" {
  type        = string
  description = "AWS region for the Lambda deployment"
}

variable "workspace_name" {
  type        = string
  default     = "main"
  description = "Logical workspace/environment name used for resource names"
}

variable "project_name" {
  type        = string
  default     = "nord-alert"
  description = "Project name prefix used in AWS resource naming"
}

variable "ecr_repository_name" {
  type        = string
  default     = "nord-alert-backend"
  description = "ECR repository name for the Lambda container image"
}

variable "lambda_image_uri" {
  type        = string
  description = "Fully qualified ECR image URI for the Lambda function"
}

variable "lambda_timeout_seconds" {
  type        = number
  default     = 30
  description = "Lambda timeout in seconds"
}

variable "lambda_memory_size" {
  type        = number
  default     = 1024
  description = "Lambda memory size in MB"
}

variable "lambda_architecture" {
  type        = string
  default     = "x86_64"
  description = "Lambda architecture to deploy"
}
