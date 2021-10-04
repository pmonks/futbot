#!/usr/bin/env bash
clojure -Srepro -J-Dclojure.main.report=stderr -T:release release
