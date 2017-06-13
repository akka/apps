#
# Cookbook Name:: gce-logging-agent
# Recipe:: default
#
# Copyright 2017, Lightbend
#
# All rights reserved - Do Not Redistribute
#

cookbook_file "/home/akka/startup-script-akka-tweaks.sh" do
  source "startup-script-akka-tweaks.sh"
end

bash "chown akka.akka startup-script-akka-tweaks.sh" do
  code <<-EOH
    chown akka.akka /home/akka/startup-script-akka-tweaks.sh
  EOH
end

bash 'execute startup-script-akka-linux-tweaks.sh' do
  code <<-EOH
    cd /home/akka
    chmod +x ./startup-script-akka-tweaks.sh
    ./startup-script-akka-tweaks.sh
  EOH
end
