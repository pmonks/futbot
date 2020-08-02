#!/usr/bin/env bash

source ./bin/build

# Use a low memory ceiling to emulate Heroku dyno environment
java -Xmx300m -Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -jar ./target/futbot-standalone.jar "$@"
