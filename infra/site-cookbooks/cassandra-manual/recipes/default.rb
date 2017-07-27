#
# Cookbook Name:: cassandra-manual
# Recipe:: default
#
# Copyright 2017, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute


user "cassandra" do 
  home '/home/cassandra'
  shell '/bin/bash'
  password 'cassandra'
  action :create
  manage_home true
end


bash 'install cassandra apt repo' do
  code <<-EOH
  echo "deb http://www.apache.org/dist/cassandra/debian 310x main" | sudo tee -a /etc/apt/sources.list.d/cassandra.sources.list

  curl https://www.apache.org/dist/cassandra/KEYS | sudo apt-key add -

  sudo apt-get update

  sudo apt-key adv --keyserver pool.sks-keyservers.net --recv-key A278B781FE4B2BDA

  EOH
end

package 'ca-certificates-java' do
  action :install
end

package 'openjdk-8-jre-headless' do
  action :install
end

package 'cassandra' do
  action :install
end

template "/etc/cassandra/cassandra.yaml" do 
  source "cassandra.yaml.rb"
end

bash "restart cassandra" do 
  code "service cassandra restart"
end
