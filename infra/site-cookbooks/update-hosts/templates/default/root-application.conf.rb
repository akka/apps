akka {

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
      <%= (node['akka']['seed_nodes'].each do |seed|
         "akka://#{node['akka']['system_name']}/@#{seed}:#{node['akka']['port']}"
      end).join(",") %>
    ]
  }
}
