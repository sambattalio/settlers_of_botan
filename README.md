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

## Initial Settlement / Road Placement

Our implementation choses the first settlement placement as the node that has the maximum total probability of gaining 
resources and the road off of that is placed as the one pointing towards where we think the next settlement will be placed 
even though that can change. The second settlement placement selects the top five probability nodes that are possible to build 
on, then compares them in a similar fashion to our high-level strategy. This means that it first sees how good each option is 
for constructing the longest road, then development points, then overall development. The road off of that is then placed 
again in the direction of the first settlement so that the two may more easily be connected.

## Behavioral Tree Setup

The bot chooses from three separate high level strategies. Based on path-finding algorithm, the bot first decides if it would like to win by obtaining the longest road. To achieve this goal, it will build roads in the direction most likely to gain it longest road and towards open spots that will provide more wood and brick to greater expand the road. If the bot determines that it is no longer possible or likely to win with longest road, it will switch to a Largest Army strategy. In this strategy, the bot will start buying development cards to gain knights. It will also prioritize road building toward open spots that would provide ore, sheep, and wheat. If the bot's army size falls too far behind another player's, the bot will switch to an expansion strategy. In this defaults strategy, the bot attempts to build as many settlements and cities as possible with little care about the spots they reside on. It will also prioritize buying development cards in order to gain vicotry points. The behavioral trees are depicted below. 

Longest Road Strategy
![Longest Road](/img/LR.jpg)
Largest Army Strategy
![Largest Army](/img/LA.jpg)
Default Expansion Strategy
![Default](/img/Default.jpg)

# Trading

The trading strategy initially determines what resources the bot needs in order to build the piece it has designated as its next desired piece through the decision tree. It attempts to trade with any ports the bot currently has settlements on in order to obtain this needed piece. The bot will perform port trades until they are determined to no longer helpful. Once this is complete, the bot attempts to perform trades with other players. As it is rarely beneficial to the bot to trade more than one resource for one resource with other players, it will continuously attempt to perform one for one trades with players until it determines offering to players is no longer beneficial. If the bot still doesn’t have the desired resources for the piece, it will try a four for one resource trade with the bank. If all these actions are completed and the resources still aren’t available, the bot will move on to the next piece it desires and start the process over.

# Features one can extend/implement

If you want to contribute / fork this project and make your own bot, look no further!
The best place to start is to modify / extend the decision tree in the decision directory.
This leverages a lot of helper functions, and can easily be modified with new logic to change
the build / trade flow. The simplest way to change the flow of logic in this bot is to change
the shouldUse functions as well as the respective plan functions for each strategy. When you get
the hand of the decision tree, you can even implement your own additional strategy sub-tree!

One feature that should be trivial to gameplay to add is trash talking. Leveraging the api at
[here](http://nand.net/jsettlers/devel/doc/overview-summary.html), you can send messsages in the
chat to taunt the user! This can be sprinkled anywhere that you see fit, as long as it frustrates
the opponent.

# Magic numbers / configuration to alter bot's behavior

To keep the current decision tree structure, there are a few key magic numbers you can change
that will modify how the bot flows.

Here: https://github.com/sambattalio/settlers_of_botan/blob/64719db67084364caf1090942f91e2db3b17fbe3/src/main/java/bot/NDHelpers.java#L31

There are two magic numbers MAX_ROAD_DIFF and MAX_ARMY_DIFF which determine how far ahead or behind you should be
to continue with this strategy. They are very good in tweaking how aggresive the bot should play.

# Other Possible Unimplemented Features

- Aggressive road building when the bot's longest road is threatened
  - This can be built off of the [best possible long road](https://github.com/sambattalio/settlers_of_botan/blob/c867b500f0f1f542e4c03b85db93931a26e1676b/src/main/java/bot/NDHelpers.java#L258) function
- Longest road cutoff when another player's road has potential to be longer than the bots
  - Can be implemented in our Decision directory as wel as the [best possible long road](https://github.com/sambattalio/settlers_of_botan/blob/c867b500f0f1f542e4c03b85db93931a26e1676b/src/main/java/bot/NDHelpers.java#L258) function.
- Keeping track of resouces other players want to offer them more attractive trades
  - NDRobotBrain.java is a good place to look to work with trading!
- Attempting to build more than one piece per turn when a build fails
  - Perhaps look at working with the buildingPlan stack that Jsettlers2 uses more. 
- More advanced analysis of what future resources the bot needs when accepting trades
  - NDRobotBrain.java

# Bugs

- One piece is played per turn occasionally when we could build more
- Late game-play is still weak compared to early-mid game
