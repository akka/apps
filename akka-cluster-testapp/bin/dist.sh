#!/bin/sh

#sbt clean dist
mv target/dist target/akka-testapp
cp bin/* target/akka-testapp/bin/
tar -cz -C target -f target/akka-testapp.tgz akka-testapp


