#
# Cookbook Name:: cassandra-linux-tweaks
# Recipe:: default
#
# Copyright 2017, Lightbend
#
# All rights reserved - Do Not Redistribute
#


bash 'update net.core.rmem_max' do
  code <<-EOH
    sysctl net.core.rmem_max=16777216
  EOH
end

bash 'update net.core.wmem_max' do
  code <<-EOH
    sysctl net.core.wmem_max=16777216
  EOH
end

bash 'update net.core.rmem_default' do
  code <<-EOH
    sysctl net.core.rmem_default=16777216
  EOH
end

bash 'update net.core.wmem_default' do
  code <<-EOH
    sysctl net.core.wmem_default=16777216
  EOH
end

bash 'update net.core.optmem_max' do
  code <<-EOH
    sysctl net.core.optmem_max=40960    
  EOH
end

bash 'update net.ipv4.tcp_rmem' do
  code <<-EOH
    sysctl net.ipv4.tcp_rmem="4096 87380 16777216"
  EOH
end

bash 'update net.ipv4.tcp_wmem' do
  code <<-EOH
    sysctl net.ipv4.tcp_wmem="4096 65536 16777216"
  EOH
end
