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

# set up mutual (naive, same key) trust between benchmark nodes
bash "trust akka's public key" do
  code <<-EOH
  echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDg0jkMkullztlpthHQ8LB4fYvD9Fu2f0EdE6+slC9FjfqIdgkvC2OfOFNrjVniP9ghe7iVcIU+ZscbxiQO92VT6tj67XxgqMosYwmeonn43IR77JsgUKqlfHgGlrCwjg9b1IL2bhAY0Ib7Ot+1iqpxHIKk6uWyZZm6Nu3hMLIFBNA0C50DmuzC3y6sbUCVg/2d6seLeG6hFrhdtSWW/RkkxXEh5x798VgY0+WoU+SyYBEIcrc9ObNMzYKavg3jNJH+tEBUetVhzg2t9whmFWwZvFoX89pYF0vzK9lwh72AxqsjWa3tor4QHH39SBBPKsKT22vfX94Mzt0D2+panyzB akka@akka-nodes" >> /home/akka/.ssh/authorized_keys
  EOH
end


# these are secrets, any key will work though, just generate one
cookbook_file '/home/akka/.ssh/id_rsa' do
  source 'akka-user_id_rsa'
end
cookbook_file '/home/akka/.ssh/id_rsa.pub' do
  source 'akka-user_id_rsa.pub'
end

# this is a special file to get commercial tools onto the nodes
# use your key and put it in there
cookbook_file '/home/akka/.lightbend/commercial.credentials' do
  source 'lightbend-commercial.credentials'
end

