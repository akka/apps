#!/bin/sh

VERSION=0.1-SNAPSHOT

#sbt clean dist
mv target/dist target/akka-testapp-$VERSION
cp bin/* target/akka-testapp-$VERSION/bin/
cp gce-scripts1/* target/akka-testapp-$VERSION/bin/
tar -cz -C target -f target/akka-testapp-$VERSION.tgz akka-testapp-$VERSION


