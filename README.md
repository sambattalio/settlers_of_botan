# settler-ai
Settlers of Catan bot for CSE 40971 @ Notre Dame. Built using JSettlers2

## Dependencies

Our bot depends on Gradle for building with dependencies. [Install Gradle](https://gradle.org/install/)

As well, we depend upon the [JSettlers library](https://github.com/jdmonin/JSettlers2) as our Catan implementation. We use git submodules to include this, and it can be setup with `git pull $REPO --recursive` or `make update`.

## Getting Set Up

Make scripts are provided:

To run the server `make serve`

To build the bot `make build`

To run the bot `make run`

To install the bot `make install`

To run the client on the currently running (local) server run `make client`. java appears to be picky in regards to the path separator so it may be necessary to update it to use a semicolon instead of the colon.

To run a simulation run `make test`. This will run a server and connect the bot to it and begin simulations. THe bot will print its output to the log if it is in the simulation (sometimes does not make it in due to race conditions, which requires a restart). To view its progress, run the client program, select a name, and join the current game, where it will be called 'nd'.

To clean the project `make clean`

## Playing the Game

After `make serve` and `make run`, open JSettlers.jar

Choose `Connect to Server`

Name the Server `localhost`, make the port `8880`, and select connect

Name your player `debug`

Set the game parameters and start the game


## Behavioral Tree Setup

The bot chooses from three separate high level strategies. Based on path-finding algorithm, the bot first decides if it would like to win by obtaining the longest road. To achieve this goal, it will build roads in the direction most likely to gain it longest road and towards open spots that will provide more wood and brick to greater expand the road. If the bot determines that it is no longer possible or likely to win with longest road, it will switch to a Largest Army strategy. In this strategy, the bot will start buying development cards to gain knights. It will also prioritize road building toward open spots that would provide ore, sheep, and wheat. If the bot's army size falls too far behind another player's, the bot will switch to an expansion strategy. In this defaults strategy, the bot attempts to build as many settlements and cities as possible with little care about the spots they reside on. It will also prioritize buying development cards in order to gain vicotry points. 
