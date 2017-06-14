akka {

  remote {
    netty.tcp {
      hostname = "<%= node['ipaddress-internal'] %>"
    }
  }

  cluster {
    seed-nodes = <%= node['akka']['seed_nodes'] %>
  }
}
