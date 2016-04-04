#!/bin/sh
# Heroku exec script
export BRUTUSIN_RPC_LINK_SOURCE=true
if [ "$TARGET" = "secure" ]; then
  cd rpc-demos/rpc-demo-security-jar
elif [ "$TARGET" = "jar" ]; then
  cd rpc-demos/rpc-demo-jar
elif [ "$TARGET" = "chat" ]; then
  cd rpc-demos/rpc-chat
fi;
mvn exec:java -Dexec.mainClass=org.brutusin.rpc.Main