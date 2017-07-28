#
# Cookbook Name:: update-hosts
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

template '/home/akka/run-max-throughput-benchmark.sh' do 
  source 'run-max-throughput-benchmark.sh.rb'
end

template '/home/akka/run-max-throughput-benchmark.sh' do 
  source 'run-latency-benchmark.sh.rb'
end

cookbook_file '/home/akka/multi-node-test.hosts' do 
  source 'multi-node-test.hosts'
end

bash "chown generated files for akka user" do
  code <<-EOH
    chown -R akka.akka /home/akka
    chmod +x *.sh
  EOH
end
