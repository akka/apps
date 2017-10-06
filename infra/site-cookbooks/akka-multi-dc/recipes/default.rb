#
# Cookbook Name:: akka-multi-dc
# Recipe:: default
#
# Copyright 2017, Lightbend
#
# All rights reserved - Do Not Redistribute
#

directory '/home/akka/multidc' do
  owner 'akka'
  group 'akka'
  action :create
end


template "/home/akka/multidc/application.conf" do
  source "application.conf.erb"
end
