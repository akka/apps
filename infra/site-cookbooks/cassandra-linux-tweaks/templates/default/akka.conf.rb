<source>
  type tail
  format none
  path <%= @log_path %>
  pos_file /var/lib/google-fluentd/pos/akka.pos
  read_from_head true
  tag akka
</source>
