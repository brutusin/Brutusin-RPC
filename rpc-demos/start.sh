#!/bin/sh
# Heroku start script
export BRUTUSIN_RPC_LINK_SOURCE=true
if [ "$TARGET" = "secure" ]; then
  cd rpc-demos/rpc-demo-security-jar
  mvn exec:java -Dexec.mainClass=org.brutusin.rpc.Main
elif [ "$TARGET" = "jar" ]; then
  cd rpc-demos/rpc-demo-jar
  mvn exec:java -Dexec.mainClass=org.brutusin.rpc.Main
elif [ "$TARGET" = "chat" ]; then
  cd rpc-demos/rpc-chat
  mvn exec:java -Dexec.mainClass=org.brutusin.rpc.Main
fi;
