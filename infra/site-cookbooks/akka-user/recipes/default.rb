#
# Cookbook Name:: gce-logging-agent
# Recipe:: default
#
# Copyright 2017, Lightbend
#
# All rights reserved - Do Not Redistribute
#

user "akka" do 
  home '/home/akka'
  shell '/bin/bash'
  password 'akka'
  action :create
  manage_home true
end

template '/home/akka/root-application.conf' do
  source 'root-application.conf.rb'
end

template '/home/akka/run-multinode-benchmark.sh' do 
  source 'run-multinode-benchmark.sh.rb'
end

template '/home/akka/multi-node-test.hosts' do 
  source 'multi-node-test.hosts.rb'
end

bash "clone akka" do
  code <<-EOH
    cd /home/akka
    git clone https://github.com/akka/akka

    chown -R akka.akka akka
  EOH
end
