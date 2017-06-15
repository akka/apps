# ------------------------ WARNING -------------------------------- 
#      This file is generated automatically via Chef scripts. 
#      Do not edit manually - all your changes WILL be lost.
# ------------------------ WARNING -------------------------------- 
akka {
  
  system-name = "<%= node['akka']['system_name'] %>"

  # total number of nodes this benchmark will run on (same as number of nodes in multi-node-test.hosts)
  total-nodes = <%= node['akka']['total_nodes'] %>
  
  remote {
    netty.tcp {
      hostname = "<%= node['ipaddress-internal'] %>"
      port = <%= node['akka']['port'] %>
    }
    
    artery {
      # enabled = on this is up to the app to decide
      canonical.hostname = "<%= node['ipaddress-internal'] %>"
      canonical.port = <%= node['akka']['port'] %>
    }
  } 

  cluster {
    seed-nodes = [
      <%= (node['akka']['seed_nodes'].map do |seed|
         "\"akka://#{node['akka']['system_name']}@#{seed}:#{node['akka']['port']}\""
      end).join(",\n      ") %>
    ]

    http.management {
      hostname = "0.0.0.0"
      port = 19999
    }
  }

}
