#!/bin/sh
# Heroku start script
if [ "$TARGET" = "secure" ]; then
  sh rpc-demos/rpc-demo-secure-jar/target/bin/exec
else
 sh rpc-demos/rpc-demo-jar/target/bin/exec
fi;