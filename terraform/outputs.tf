output "public_ip" {
  description = "Public IP of the VM"
  value       = oci_core_instance.vm.public_ip
}

output "ssh_example" {
  description = "SSH command"
  value       = "ssh ubuntu@${oci_core_instance.vm.public_ip}"
}

output "web_url" {
  description = "HTTP URL (container exposed on port 80)"
  value       = "http://${oci_core_instance.vm.public_ip}"
}
