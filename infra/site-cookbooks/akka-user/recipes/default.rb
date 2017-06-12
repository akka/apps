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

# set up mutual (naive, same key) trust between benchmark nodes
cookbook_file '/home/akka/.ssh/authorized_keys' do
  source 'akka-user_authorized_keys'
end
cookbook_file '/home/akka/.ssh/id_rsa' do
  source 'akka-user_id_rsa'
end
cookbook_file '/home/akka/.ssh/id_rsa.pub' do
  source 'akka-user_id_rsa.pub'
end

bash "clone akka" do
  code <<-EOH
    cd /home/akka
    git clone https://github.com/akka/akka

    chown -R akka.akka akka
  EOH
end
