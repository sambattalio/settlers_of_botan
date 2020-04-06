FILES=src/main/java/bot/*.java
LIBJARFILE=lib/jsettlers2/build/libs/JSettlers-2.3.00.jar
BOTCLASS=bot.NDRobotClient
SERVERCLASS=soc.server.SOCServer
CLIENTCLASS=soc.client.SOCPlayerClient


COOKIE=foobar

build: $(FILES)
	gradle build jar

update:
	git submodule init
	git submodule update
	git submodule foreach git pull origin master

install:	update build

run:
	java -cp "$(LIBJARFILE):build/classes/java/main" $(BOTCLASS) -c $(COOKIE)

serve:
	java -cp "$(LIBJARFILE)" $(SERVERCLASS) -o N7=t7 -Djsettlers.startrobots=3 -Djsettlers.allow.debug=Y -Djsettlers.bots.cookie=$(COOKIE) > server.log 2>&1

simulate:
	java -cp "$(LIBJARFILE)" $(SERVERCLASS) -Djsettlers.allow.debug=Y -Djsettlers.startrobots=4 -Djsettlers.bots.percent3p=100 -Djsettlers.bots.botgames.parallel=1 -Djsettlers.bots.botgames.wait_sec=5 -Djsettlers.bots.botgames.total=10 -Djsettlers.bots.botgames.shutdown=Y -Djsettlers.bots.cookie=$(COOKIE) > server.log 2>&1

client:
	java -cp "$(LIBJARFILE)" $(CLIENTCLASS) localhost 8880

test:
	bash demo.sh

clean:
	gradle clean

.PHONY:	build update install run serve simulate client test clean