#!/bin/sh
# Heroku start script
if [ "$TARGET" = "secure" ]; then
  sh rpc-demos/rpc-demo-security-jar/target/bin/exec
elif [ "$TARGET" = "jar" ]; then
  sh rpc-demos/rpc-demo-jar/target/bin/exec
elif [ "$TARGET" = "chat" ]; then
  sh rpc-demos/rpc-chat/target/bin/exec
fi;