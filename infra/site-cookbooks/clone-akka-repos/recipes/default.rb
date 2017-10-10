#
# Cookbook Name:: gce-logging-agent
# Recipe:: default
#
# Copyright 2017, Lightbend
#
# All rights reserved - Do Not Redistribute
#

package "git"

# bash "clone akka" do
#   code <<-EOH
#     cd /home/akka
#     rm -rf akka
#     git clone https://github.com/akka/akka
#
#     chown -R akka.akka akka
#   EOH
# end
#
# bash "clone akka/apps" do
#   code <<-EOH
#     cd /home/akka
#     rm -rf apps
#     git clone https://github.com/akka/apps.git
#
#     chown -R akka.akka apps
#   EOH
# end

#bash "clone akka/apps/multidc" do
#  code <<-EOH
#    cd /home/akka/multidc
#    rm -rf apps
#    git clone https://github.com/chbatey/apps.git
#
#    chown -R akka.akka apps
#  EOH
# end
#


bash "clone akka/apps/management" do
  code <<-EOH
    cd /home/akka/multidc
    rm -rf apps
    git clone https://github.com/chbatey/akka-management.git 

    chown -R akka.akka akka-management
  EOH
 end

