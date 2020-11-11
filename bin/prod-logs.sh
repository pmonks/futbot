#!/usr/bin/env bash
heroku logs --app=referees-futbot --dyno=bot.1 -n 2000
