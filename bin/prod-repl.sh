#!/usr/bin/env bash
echo "Once the heroku port forwarder has started, in a separate shell run:"
echo ""
echo "        nc 127.0.0.1 5555"
echo ""
heroku ps:forward 5555 --app=referees-futbot --dyno=bot.1
