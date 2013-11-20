#! /bin/bash

VERSION=0.1-SNAPSHOT

gsutil cp gs://jvm/jdk-7u40-linux-x64.tar.gz - | tar -C /opt -xzf -

cd /tmp
gsutil cp gs://akka-testapp/akka-testapp-$VERSION.tgz - | tar xzf -
gsutil cp -R gs://akka-testapp/patches .
cd akka-testapp-$VERSION

#patch it
cp -a ../patches/* .

exec bash bin/startTestappSeed.sh


