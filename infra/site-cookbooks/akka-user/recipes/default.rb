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
bash "trust akka's public key" do
  code <<-EOH
  echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDg0jkMkullztlpthHQ8LB4fYvD9Fu2f0EdE6+slC9FjfqIdgkvC2OfOFNrjVniP9ghe7iVcIU+ZscbxiQO92VT6tj67XxgqMosYwmeonn43IR77JsgUKqlfHgGlrCwjg9b1IL2bhAY0Ib7Ot+1iqpxHIKk6uWyZZm6Nu3hMLIFBNA0C50DmuzC3y6sbUCVg/2d6seLeG6hFrhdtSWW/RkkxXEh5x798VgY0+WoU+SyYBEIcrc9ObNMzYKavg3jNJH+tEBUetVhzg2t9whmFWwZvFoX89pYF0vzK9lwh72AxqsjWa3tor4QHH39SBBPKsKT22vfX94Mzt0D2+panyzB akka@akka-nodes" >> /home/akka/.ssh/authorized_keys
  EOH
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
    rm -rf akka
    git clone https://github.com/akka/akka

    chown -R akka.akka akka
  EOH
end
