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

bash "all akka users should trust replicated-entity.pem" do
  code <<-EOH
  sudo su - akka
  echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCXlAG/NOl6RMXCSvWvwZuqOMed3PFIkZvxfgJaSOhf2t50uSG1gJ6UEafDHuPf2wgkDr8Og8EtO5DV7X4s9oDbXII81U/4Mr0m7ar6n7KRR2e3YKrrJ1TpYMCivzrrABSqQYyWqpEh63YASNlyh8YQzGSpBbkALKkA4b4mR5F2/+6c/bXAthLMlElVs9Nvf3REXIZHmilLHpQzSsIi5DkqBXZmSl1GD4bjNuKUzVbjNNDXS0dRSztonOD4JWJRaOC4dagNL+OXVyycS3HKj0BnB3it4b03rFpr2U/oqp1mZEayTqb5wGlRBz9jUh6a5F94K3eJVR3Fxv1A54iKVAsd replicated-entity" >> $HOME/.ssh/authorized_keys
  EOH
end
