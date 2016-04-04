#!/bin/sh
# Heroku exec script
export BRUTUSIN_RPC_LINK_SOURCE=true
if [ "$TARGET" = "secure" ]; then
  cd rpc-demos/rpc-demo-security-jar/target
elif [ "$TARGET" = "jar" ]; then
  cd rpc-demos/rpc-demo-jar/target
elif [ "$TARGET" = "chat" ]; then
  cd rpc-demos/rpc-chat/target
fi;
java -jar executable.jar