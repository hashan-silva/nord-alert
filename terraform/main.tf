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
  tenancy_ocid = var.tenancy_ocid
  user_ocid    = var.user_ocid
  fingerprint  = var.fingerprint
  private_key  = var.private_key
  region       = var.region
}

############################################
# Data: ADs, existing VCNs, image lookup
############################################
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

# Look for any existing VCNs in the compartment
data "oci_core_vcns" "existing" {
  compartment_id = var.compartment_ocid
}

# Choose Ubuntu 22.04 for the selected shape
data "oci_core_images" "ubuntu" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "22.04"
  shape                    = var.compute_shape
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

############################################
# VCN: reuse if found; otherwise create one
############################################
resource "oci_core_virtual_network" "vcn" {
  count          = length(data.oci_core_vcns.existing.virtual_networks) == 0 ? 1 : 0
  compartment_id = var.compartment_ocid
  cidr_block     = var.vcn_cidr
  display_name   = "nord-alert-vcn"
  dns_label      = "nordalert"
}

# Pick the VCN id (existing or newly created)
locals {
  vcn_id = length(data.oci_core_vcns.existing.virtual_networks) > 0 ? data.oci_core_vcns.existing.virtual_networks[0].id : oci_core_virtual_network.vcn[0].id
}

############################################
# Internet Gateway: reuse or create
############################################
data "oci_core_internet_gateways" "existing" {
  compartment_id = var.compartment_ocid
  vcn_id         = local.vcn_id
}

locals {
  existing_igws = try(data.oci_core_internet_gateways.existing.internet_gateways, [])
  igw_id        = length(local.existing_igws) > 0 ? local.existing_igws[0].id : oci_core_internet_gateway.igw[0].id
}

resource "oci_core_internet_gateway" "igw" {
  count          = length(local.existing_igws) == 0 ? 1 : 0
  compartment_id = var.compartment_ocid
  vcn_id         = local.vcn_id
  display_name   = "nord-alert-igw"
  enabled        = true
}

############################################
# Our own route table (points to IGW)
############################################
resource "oci_core_route_table" "rt" {
  compartment_id = var.compartment_ocid
  vcn_id         = local.vcn_id
  display_name   = "nord-alert-rt"

  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = local.igw_id
  }
}

############################################
# Public subnet (ours; avoids changing existing)
############################################
resource "oci_core_subnet" "public" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = local.vcn_id
  display_name               = "nord-alert-public-subnet"
  cidr_block                 = var.public_subnet_cidr
  route_table_id             = oci_core_route_table.rt.id
  prohibit_public_ip_on_vnic = false
  # Use the VCN's default DHCP opts & security list automatically
  dns_label = "pub"
}

############################################
# NSG for the instance (ingress 22/80/443)
############################################
resource "oci_core_network_security_group" "web" {
  compartment_id = var.compartment_ocid
  vcn_id         = local.vcn_id
  display_name   = "nord-alert-web-nsg"
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

# Allow all egress
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
  display_name        = var.instance_display
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
    # Avoid recreation when the latest image ID changes or when
    # OCI normalizes user_data on the instance. This keeps the
    # instance in place on repeated terraform apply runs.
    ignore_changes = [
      source_details[0].source_id,
      metadata["user_data"],
    ]
  }
}
