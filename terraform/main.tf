terraform {
  required_version = ">= 1.0"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 5.0"
    }
  }
}

provider "oci" {
  tenancy_ocid  = var.tenancy_ocid
  user_ocid     = var.user_ocid
  fingerprint   = var.fingerprint
  private_key   = var.private_key
  region        = var.region
}

resource "oci_container_instances_container_instance" "app" {
  availability_domain = var.availability_domain
  compartment_id       = var.compartment_ocid
  display_name         = "nord-alert"
  shape                = var.shape

  shape_config {
    memory_in_gbs = 1
    ocpus         = 1
  }

  vnics {
    is_public_ip_enabled = true
    subnet_id            = var.subnet_ocid
  }

  containers {
    display_name = "nord-alert-backend"
    image_url    = var.image
    resource_config {
      memory_limit_in_gbs = 1
      vcpus_limit         = 1
    }
  }
}
