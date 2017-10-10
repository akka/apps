echo 'Running $CLAZZ on $IP2'

cd /home/akka/apps

/home/akka/sbt -J-XX:+PrintGCDetails -J-XX:+PrintGCTimeStamps -J-Xms4G -J-Xmx4G -Dsbt.log.noformat=true '; project multidc; run'