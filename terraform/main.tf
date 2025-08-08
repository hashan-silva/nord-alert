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
# Data: availability domains & image lookup
############################################
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
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
# Networking (VCN, IGW, Route Table, Subnet, Security List)
############################################
resource "oci_core_virtual_network" "vcn" {
  compartment_id = var.compartment_ocid
  cidr_block     = var.vcn_cidr
  display_name   = "nord-alert-vcn"
  dns_label      = "nordalert"
}

resource "oci_core_internet_gateway" "igw" {
  compartment_id = var.compartment_ocid
  display_name   = "nord-alert-igw"
  vcn_id         = oci_core_virtual_network.vcn.id
  enabled        = true
}

resource "oci_core_route_table" "rt" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_virtual_network.vcn.id
  display_name   = "nord-alert-rt"

  route_rules {
    network_entity_id = oci_core_internet_gateway.igw.id
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
  }
}

resource "oci_core_security_list" "sl" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_virtual_network.vcn.id
  display_name   = "nord-alert-sl"

  # Egress: allow all
  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
  }

  # Ingress: SSH (22)
  ingress_security_rules {
    protocol = "6" # TCP
    source   = var.ssh_ingress_cidr
    tcp_options { min = 22, max = 22 }
  }

  # Ingress: HTTP (80)
  ingress_security_rules {
    protocol = "6"
    source   = var.http_ingress_cidr
    tcp_options { min = 80, max = 80 }
  }

  # Ingress: HTTPS (443)
  ingress_security_rules {
    protocol = "6"
    source   = var.https_ingress_cidr
    tcp_options { min = 443, max = 443 }
  }
}

resource "oci_core_subnet" "public" {
  compartment_id              = var.compartment_ocid
  vcn_id                      = oci_core_virtual_network.vcn.id
  display_name                = "nord-alert-public-subnet"
  cidr_block                  = var.public_subnet_cidr
  route_table_id              = oci_core_route_table.rt.id
  security_list_ids           = [oci_core_security_list.sl.id]
  prohibit_public_ip_on_vnic  = false
  dhcp_options_id             = oci_core_virtual_network.vcn.default_dhcp_options_id
  dns_label                   = "pub"
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
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data           = base64encode(local.cloud_init)
  }

  preserve_boot_volume = false
}
