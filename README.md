# settler-ai
Settlers of Catan bot for CSE 40971 @ Notre Dame. Built using JSettlers2

## Getting Set Up

Make scripts are provided:

To run the server `make serve`

To build the bot `make build`

To run the bot `make run`

To install the bot `make install`

To run the client on the currently running (local) server run `make client`. java appears to be picky in regards to the path separator so it may be necessary to update it to use a semicolon instead of the colon.

To run a simulation run `make test`. This will run a server and connect the bot to it and begin simulations. THe bot will print its output to the log if it is in the simulation (sometimes does not make it in due to race conditions, which requires a restart). To view its progress, run the client program, select a name, and join the current game, where it will be called 'nd'.

To clean the project `make clean`