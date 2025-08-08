variable "tenancy_ocid" {}
variable "user_ocid" {}
variable "fingerprint" {}
variable "private_key" {}
variable "region" {}
variable "compartment_ocid" {}
variable "image" {}
variable "availability_domain" {}
variable "subnet_ocid" {}
variable "shape" {
  default = "CI.Standard.A1.Flex"
}
