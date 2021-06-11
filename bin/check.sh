#!/usr/bin/env bash
clj -M:check
clj -M:kondo
clj -M:eastwood
clj -M:outdated
