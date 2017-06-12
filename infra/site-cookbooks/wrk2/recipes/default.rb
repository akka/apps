#
# Cookbook Name:: wrk2
# Recipe:: default
#
# Copyright (C) 2016 Konrad Malawski
#
# All rights reserved - Do Not Redistribute
#

package 'make' do
  action :install
  end

package 'gcc' do
  action :install
end

execute 'cleanup wrk2' do
  command 'rm -rf wrk2'
end

execute 'clone wrk2' do
  command 'git clone https://github.com/giltene/wrk2'
end

package 'libssl-dev' do
  action :install
end

execute 'make wrk2' do
  command 'make'
  cwd 'wrk2'
end

execute 'copy to /usr/bin/wrk2' do
  command 'cp wrk /usr/bin/wrk'
  cwd 'wrk2'
end
