#
# Cookbook Name:: sbt_credentials
# Recipe:: default
#
# Copyright 2017, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute

bash 'link credentials for 1.0 to 0.13' do
  code <<-EOH
  cd /home/jenkinsakka/.sbt
  mkdir -p 1.0

  rm 1.0/credentials.sbt 
  ln -s $(pwd)/0.13/credentials.sbt 1.0/credentials.sbt
 
  rm 1.0/global.sbt 
  ln -s $(pwd)/0.13/global.sbt 1.0/

  rm  1.0/repositories
  ln -s $(pwd)/0.13/repositories 1.0/ 
  EOH
end
