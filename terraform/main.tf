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

data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

data "oci_core_vcns" "vcns" {
  compartment_id = var.compartment_ocid
}

data "oci_core_subnets" "selected" {
  compartment_id = var.compartment_ocid
  vcn_id         = data.oci_core_vcns.vcns.virtual_networks[0].id
}

resource "oci_container_instances_container_instance" "app" {
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  compartment_id       = var.compartment_ocid
  display_name         = "nord-alert"
  shape                = var.shape

  shape_config {
    memory_in_gbs = 1
    ocpus         = 1
  }

  vnics {
    assign_public_ip = true
    subnet_id        = data.oci_core_subnets.selected.subnets[0].id
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
