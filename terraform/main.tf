############################################
# Terraform & Provider
############################################
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

############################################
# Data lookups (no creation of networking)
############################################
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

# Existing subnet you supply (must be PUBLIC)
data "oci_core_subnet" "existing" {
  subnet_id = var.existing_subnet_ocid
}

# Find a recent Ubuntu 22.04 image compatible with the selected shape
data "oci_core_images" "ubuntu" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "22.04"
  shape                    = var.compute_shape
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

############################################
# Cloud-init: install Docker & run your image
############################################
locals {
  cloud_init = <<-EOT
    #cloud-config
    package_update: true
    runcmd:
      - curl -fsSL https://get.docker.com | sh
      - systemctl enable --now docker
      - docker pull ${var.image}
      - docker rm -f app || true
      - docker run -d --restart unless-stopped -p 80:${var.container_port} --name app ${var.image}
  EOT
}

############################################
# Compute Instance (Always Free E2 Micro)
############################################
resource "oci_core_instance" "vm" {
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  compartment_id      = var.compartment_ocid
  display_name        = var.instance_display
  shape               = var.compute_shape

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.ubuntu.images[0].id
  }

  create_vnic_details {
    subnet_id        = data.oci_core_subnet.existing.id
    assign_public_ip = true
    hostname_label   = "nordalert"
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data           = base64encode(local.cloud_init)
  }

  preserve_boot_volume = false

  # Safety: ensure the chosen subnet actually allows public IPs
  lifecycle {
    precondition {
      condition     = data.oci_core_subnet.existing.prohibit_public_ip_on_vnic == false
      error_message = "Selected subnet prohibits public IPs. Pick a PUBLIC subnet or enable public IPs for it."
    }
  }
}
