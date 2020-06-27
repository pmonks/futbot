[![Build Status](https://github.com/pmonks/futbot/workflows/build/badge.svg)](https://github.com/pmonks/futbot/actions?query=workflow%3Abuild)
[![Open Issues](https://img.shields.io/github/issues/pmonks/futbot.svg)](https://github.com/pmonks/futbot/issues)
[![License](https://img.shields.io/github/license/pmonks/futbot.svg)](https://github.com/pmonks/futbot/blob/main/LICENSE)

<img alt="futbot logo" align="right" src="https://github.com/pmonks/futbot/blob/main/futbot.png?raw=true"/>

# futbot

A small [Discord](https://discord.com/) bot that provides football (soccer) competition information sourced from [football-data.org](https://www.football-data.org/).  This bot was specifically designed and implemented for the [Referee Discord Server](https://discord.gg/FgUPVe).

## Trying it Out

### Obtaining API Keys

Configure a Discord bot using the [Discord developer portal](https://discord.com/developers), obtaining an API key.  Detailed instructions on this process are provided in the [`discljord` project](https://github.com/IGJoshua/discljord).

Obtain an API key for [football-data.org](https://football-data.org/).

### Running the Bot

Currently the bot is only distributed in source form, so regardless of how you intend to deploy it, you'll need to clone this repository locally.

#### Direct Execution

1. Either set environment variables as described in the default [`config.edn` file](https://github.com/pmonks/futbot/blob/main/resources/config.edn), or copy that file somewhere else and hardcode the values in the file directly.
2. If you set the environment variables in the previous step run `clj -m futbot.main`, otherwise run `clj -m futbot.main -c /path/to/your/config.edn`

#### Dockerised Execution

Copy the default [`config.edn` file](https://github.com/pmonks/futbot/blob/main/resources/config.edn) to `./docker-config.edn`, and edit the entries in the file for your needs.

Build the container:
```
$ docker build -t futbot .
```

Run the container:

```
$ # Interactively:
$ docker run futbot
$ # In the background:
$ docker run -d futbot
```

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/futbot/blob/main/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/futbot/issues)

[Code of Conduct](https://github.com/pmonks/futbot/blob/main/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

The `futbot` source repository has two permanent branches: `main` and `dev`.  **All development must occur either in branch `dev`, or (preferably) in branches off of `dev`.**  All PRs must also be submitted against `dev`; the `main` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `main` will be rejected.

This model allows otherwise unrelated changes to be batched up in the `dev` branch, integration tested there, and then released en masse to the `main` branch.  The `main` branch is configured to auto-deploy to heroku, and therefore this branch must only contain tested, functioning code, and should not be updated too frequently.

## License

Copyright © 2020 Peter Monks

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
