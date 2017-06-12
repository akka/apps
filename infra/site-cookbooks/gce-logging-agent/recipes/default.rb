#
# Cookbook Name:: gce-logging-agent
# Recipe:: default
#
# Copyright 2017, Lightbend
#
# All rights reserved - Do Not Redistribute
#

bash 'install logging agent (ubuntu)' do
  code <<-EOH
    curl -sSO https://dl.google.com/cloudagents/install-logging-agent.sh
    # sha256sum install-logging-agent.sh

    bash install-logging-agent.sh
  EOH
end

# create the config file so akka logs are collected ()
template "/etc/google-fluentd/config.d/akka.conf" do 
  source "akka.conf.rb" 
  variables :log_path => "/var/log/akka.log" # configure here where we log things
end

bash "force restart of google-logging agent" do 
  code "service google-fluentd restart"
end
