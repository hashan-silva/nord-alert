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
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
  backend "remote" {}
}

provider "oci" {
  tenancy_ocid = var.tenancy_ocid
  user_ocid    = var.user_ocid
  fingerprint  = var.fingerprint
  private_key  = var.private_key
  region       = var.region
}

############################################
# Data: ADs and image lookup
############################################
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

data "oci_core_images" "ubuntu" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "22.04"
  shape                    = var.compute_shape
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

############################################
# VCN (per workspace via remote state)
############################################
resource "random_string" "vcn_suffix" {
  length  = 4
  upper   = false
  lower   = true
  numeric = true
  special = false
}

resource "oci_core_virtual_network" "vcn" {
  compartment_id = var.compartment_ocid
  cidr_block     = var.vcn_cidr
  display_name   = "nord-alert-vcn-${local.ws}"
  dns_label      = "nord${random_string.vcn_suffix.result}"
}

locals {
  ws     = var.workspace_name
  vcn_id = oci_core_virtual_network.vcn.id
}

############################################
# Internet Gateway
############################################
resource "oci_core_internet_gateway" "igw" {
  compartment_id = var.compartment_ocid
  vcn_id         = local.vcn_id
  display_name   = "nord-alert-igw-${local.ws}"
  enabled        = true
}

############################################
# Our own route table (points to IGW)
############################################
resource "oci_core_route_table" "rt" {
  compartment_id = var.compartment_ocid
  vcn_id         = local.vcn_id
  display_name   = "nord-alert-rt-${local.ws}"

  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.igw.id
  }
}

############################################
# Public subnet (ours; avoids changing existing)
############################################
resource "oci_core_subnet" "public" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = local.vcn_id
  display_name               = "nord-alert-public-subnet-${local.ws}"
  cidr_block                 = var.public_subnet_cidr
  route_table_id             = oci_core_route_table.rt.id
  prohibit_public_ip_on_vnic = false
  dns_label                  = "pub"
}

############################################
# NSG for the instance (ingress 22/80/443)
############################################
resource "oci_core_network_security_group" "web" {
  compartment_id = var.compartment_ocid
  vcn_id         = local.vcn_id
  display_name   = "nord-alert-web-nsg-${local.ws}"
}

resource "oci_core_network_security_group_security_rule" "ingress_ssh" {
  network_security_group_id = oci_core_network_security_group.web.id
  direction                 = "INGRESS"
  protocol                  = "6"
  source                    = var.ssh_ingress_cidr
  tcp_options {
    destination_port_range {
      min = 22
      max = 22
    }
  }
}

resource "oci_core_network_security_group_security_rule" "ingress_http" {
  network_security_group_id = oci_core_network_security_group.web.id
  direction                 = "INGRESS"
  protocol                  = "6"
  source                    = var.http_ingress_cidr
  tcp_options {
    destination_port_range {
      min = 80
      max = 80
    }
  }
}

resource "oci_core_network_security_group_security_rule" "ingress_https" {
  network_security_group_id = oci_core_network_security_group.web.id
  direction                 = "INGRESS"
  protocol                  = "6"
  source                    = var.https_ingress_cidr
  tcp_options {
    destination_port_range {
      min = 443
      max = 443
    }
  }
}

resource "oci_core_network_security_group_security_rule" "egress_all" {
  network_security_group_id = oci_core_network_security_group.web.id
  direction                 = "EGRESS"
  protocol                  = "all"
  destination               = "0.0.0.0/0"
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
  display_name        = "${var.instance_display}-${local.ws}"
  shape               = var.compute_shape

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.ubuntu.images[0].id
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.public.id
    assign_public_ip = true
    hostname_label   = "nordalert"
    nsg_ids          = [oci_core_network_security_group.web.id]
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data           = base64encode(local.cloud_init)
  }

  preserve_boot_volume = false

  lifecycle {
    ignore_changes = [
      source_details[0].source_id,
      metadata["user_data"],
    ]
  }
}
