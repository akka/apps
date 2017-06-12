#
# Cookbook Name:: docker_simple
# Recipe:: default
#
# Copyright (C) 2015 Konrad Malawski
#
# All rights reserved - Do Not Redistribute
#

execute 'wget install docker' do
  command 'wget -qO- https://get.docker.com/ | sh'
end

group 'docker' do
  action :modify
  members ['jenkinsakka', 'local']
  append true
end