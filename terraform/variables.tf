# Provider auth & context
variable "tenancy_ocid"     { type = string, description = "OCID of your tenancy" }
variable "user_ocid"        { type = string, description = "OCID of the user owning the API key" }
variable "fingerprint"      { type = string, description = "API key fingerprint" }
variable "private_key"      { type = string, description = "PEM contents of your API private key", sensitive = true }
variable "region"           { type = string, description = "Region identifier, e.g. eu-stockholm-1" }
variable "compartment_ocid" { type = string, description = "Target compartment OCID" }

# Workload/container
variable "image"            { type = string, description = "Container image, e.g. ghcr.io/you/app:latest" }
variable "container_port"   { type = number, default = 3000, description = "Port your container listens on" }

# Networking
variable "vcn_cidr"           { type = string, default = "10.20.0.0/16" }
variable "public_subnet_cidr" { type = string, default = "10.20.10.0/24" }
variable "ssh_ingress_cidr"   { type = string, default = "0.0.0.0/0" }  # tighten for prod
variable "http_ingress_cidr"  { type = string, default = "0.0.0.0/0" }
variable "https_ingress_cidr" { type = string, default = "0.0.0.0/0" }

# Compute (Always Free-friendly)
variable "compute_shape"    { type = string, default = "VM.Standard.E2.1.Micro" }
variable "instance_display" { type = string, default = "nord-alert-free" }

# Access
variable "ssh_public_key"   { type = string, description = "Your SSH public key (single line)" }
