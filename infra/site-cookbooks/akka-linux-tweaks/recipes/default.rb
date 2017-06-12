#
# Cookbook Name:: gce-logging-agent
# Recipe:: default
#
# Copyright 2017, Lightbend
#
# All rights reserved - Do Not Redistribute
#

bash 'update net.core.rmem_max' do
  code <<-EOH
    sysctl net.core.rmem_max=2097152
  EOH
end

bash 'update net.core.wmem_max' do
  code <<-EOH
    sysctl net.core.wmem_max=2097152
  EOH
end
