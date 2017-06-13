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

bash "clone akka" do
  code <<-EOH
    cd /home/akka
    rm -rf akka
    git clone https://github.com/akka/akka

    chown -R akka.akka akka
  EOH
end
