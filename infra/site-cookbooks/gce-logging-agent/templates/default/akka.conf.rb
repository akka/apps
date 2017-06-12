<source>
  type tail
  format none
  path /home/akka/logs/*
  pos_file /home/akka/gce-fluentd-akka.pos
  read_from_head true
  tag akka
</source>
