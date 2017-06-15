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
    curl -Ls https://git.io/sbt > /home/akka/sbt 
    chmod 0755 /home/akka/sbt
    chown akka.akka sbt

    echo '' >> /home/akka/.bashrc
    echo 'PATH=$PATH:/home/akka' >> /home/akka/.bashrc
  EOH
end
