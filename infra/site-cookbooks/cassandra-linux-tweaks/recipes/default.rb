#
# Cookbook Name:: cassandra-linux-tweaks
# Recipe:: default
#
# Copyright 2017, Lightbend
#
# All rights reserved - Do Not Redistribute
#

cookbook_file "/home/akka/startup-script-cassandra-tweaks.sh" do
  source "startup-script-cassandra-tweaks.sh"
end

template '/etc/security/limits.d/cassandra.conf' do 
  source 'security-cassandra.conf'
end

bash "chown akka.akka startup-script-cassandra-tweaks.sh" do
  code <<-EOH
    chown akka.akka /home/akka/startup-script-cassandra-tweaks.sh
  EOH
end

bash 'execute startup-script-cassandra-linux-tweaks.sh' do
  code <<-EOH
    cd /home/akka
    chmod +x ./startup-script-cassandra-tweaks.sh
    ./startup-script-cassandra-tweaks.sh
  EOH
end
