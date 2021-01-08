| **main** | [![Build Status](https://github.com/pmonks/futbot/workflows/build/badge.svg?branch=main)](https://github.com/pmonks/futbot/actions?query=workflow%3Abuild) | [![Dependencies](https://github.com/pmonks/futbot/workflows/dependencies/badge.svg?branch=main)](https://github.com/pmonks/futbot/actions?query=workflow%3Adependencies) |
| **dev**  | [![Build Status](https://github.com/pmonks/futbot/workflows/build/badge.svg?branch=dev)](https://github.com/pmonks/futbot/actions?query=workflow%3Abuild) | [![Dependencies](https://github.com/pmonks/futbot/workflows/dependencies/badge.svg?branch=dev)](https://github.com/pmonks/futbot/actions?query=workflow%3Adependencies) |

[![Open Issues](https://img.shields.io/github/issues/pmonks/futbot.svg)](https://github.com/pmonks/futbot/issues)
[![License](https://img.shields.io/github/license/pmonks/futbot.svg)](https://github.com/pmonks/futbot/blob/main/LICENSE)

<img alt="futbot logo" align="right" src="https://github.com/pmonks/futbot/blob/main/futbot.png?raw=true"/>

# futbot

A small [Discord](https://discord.com/) bot that provides football (soccer) competition information sourced from [football-data.org](https://www.football-data.org/), as well as posting new LotG quizzes posted by the [Dutch Referee Blog](https://www.dutchreferee.com/) and [CNRA](http://www.cnra.net/monthly-video-quizzes/), and videos posted by [a number of referee-focused YouTube channels](https://github.com/pmonks/futbot/blob/main/heroku-config.edn).  This bot has been deployed to the [Referee Discord Server](https://invite.gg/referees), and was specifically designed and implemented for that community, and may therefore have limited utility elsewhere.

The bot also has a small, but growing number of ["responsive functions"](https://github.com/pmonks/futbot/milestone/2?closed=1) - command messages that it will respond to in any channels it has been added to.

Please review the [privacy policy](https://github.com/pmonks/futbot/blob/main/PRIVACY.md) before interacting with the deployed instance of the bot on the [Referee Discord Server](https://invite.gg/referees).

## Running Your Own Copy of the Bot

### Obtaining API Keys

Configure a Discord bot using the [Discord developer portal](https://discord.com/developers), obtaining an API key.  Detailed instructions on this process are provided in the [`discljord` project](https://github.com/IGJoshua/discljord).

Obtain an API key for [football-data.org](https://football-data.org/).

Obtain a [Google API key](https://developers.google.com/youtube/registering_an_application).

### Running the Bot

Currently the bot is only distributed in source form, so regardless of how you intend to deploy it, you'll need to clone this repository locally.

Before running the code, you'll need to generate a git info EDN file, by running `clj -M:git-info-edn` (this only needs to be done once).

#### Direct Execution

1. Either set environment variables as described in the default [`config.edn` file](https://github.com/pmonks/futbot/blob/main/resources/config.edn), or copy that file somewhere else and hardcode the values in the file directly.
2. If you set the environment variables in the previous step run `clj -M:run`, otherwise run `clj -M:run -c /path/to/your/config.edn`

#### Dockerised Execution

Copy the default [`config.edn` file](https://github.com/pmonks/futbot/blob/main/resources/config.edn) to `./docker-config.edn`, and edit the entries in the file for your needs.

Build the container:
```
docker build -t futbot .
```

Run the container:

```
# Interactively:
docker run futbot

# In the background:
docker run -d futbot
```

#### Generating the Daily Schedule

The daily schedule PDF file can be generated by running:
```
clj -M:gen-daily-schedule "YOUR_FOOTBALL_DATA_API_KEY"
```

This command will write today's daily schedule (for today, UTC) to a `daily-schedule-YYYY-MM-DD.pdf` file in the current directory.

#### Generating the IST Markov Chain

The [Markov chain](https://github.com/pmonks/futbot/blob/main/resources/ist-markov-chain.edn) containing the analysed [IST YouTube Channel's](https://www.youtube.com/channel/UCmzFaEBQlLmMTWS0IQ90tgA) video titles can be (re)generated by running:
```
clj -M:gen-ist-markov "YOUR_GOOGLE_API_KEY"
```

Note that this command has the side effect of dropping a `titles-YYYY-MM-DD.edn` file in the current directory the first time it's run each day.  This is a workaround for YouTube's draconian API call quotas.  Deleting or renaming the file will force the code to call the YouTube APIs again (which may then fail, due to those draconian quotas...).

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/futbot/blob/main/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/futbot/issues)

[Code of Conduct](https://github.com/pmonks/futbot/blob/main/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

The `futbot` source repository has two permanent branches: `main` and `dev`.  **All development must occur either in branch `dev`, or (preferably) in feature branches off of `dev`.**  All PRs must also be submitted against `dev`; the `main` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `main` will be rejected.

This model allows otherwise unrelated changes to be batched up in the `dev` branch, integration tested there, and then released en masse to the `main` branch.  The `main` branch is configured to auto-deploy to heroku, and therefore this branch must only contain tested, functioning code, and should not be updated too frequently.

## License

Copyright © 2020 Peter Monks

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)

[Flag images](https://github.com/pmonks/futbot/tree/main/resources/flags) used in PDF generation courtesy of [freeflagicons.com](https://www.freeflagicons.com/).
