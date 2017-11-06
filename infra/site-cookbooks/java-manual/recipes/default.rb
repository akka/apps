#
# Cookbook Name:: cassandra-manual
# Recipe:: default
#
# Copyright 2017, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute

bash 'install oracle apt repo' do
  code <<-EOH
  add-apt-repository ppa:webupd8team/java

  apt-get update
  EOH
end

package 'debconf-utils' do
  action :install
end

bash 'accept oracle license' do
  code <<-EOH
  echo debconf shared/accepted-oracle-license-v1-1 select true | \
    sudo debconf-set-selections

  echo debconf shared/accepted-oracle-license-v1-1 seen true | \
    sudo debconf-set-selections
  EOH
end

package 'oracle-java8-installer' do
  action :install
end

package 'oracle-java8-set-default' do
  action :install
end

bash 'download open jdk9' do
  code <<-EOH
  cd /usr/lib/jvm
  rm -rf jdk-9
  wget 'http://download.oracle.com/otn-pub/java/jdk/9.0.1+11/jdk-9.0.1_linux-x64_bin.tar.gz'
  tar xzvf jdk-9.0.1_linux-x64_bin.tar.gz
  rm jdk-9.0.1_linux-x64_bin.tar.gz
  EOH
end
