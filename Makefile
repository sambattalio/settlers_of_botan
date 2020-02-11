FILES=src/main/java/bot/*.java

COOKIE=foobar

build: $(FILES)
	gradle build jar

update:
	git submodule foreach git pull origin master

install:	update build

run:	build
	java -cp "lib/jsettlers2/build/libs/JSettlers-2.2.00.jar:build/classes/java/main" bot.NDRobotClient -c $(COOKIE)

serve:	build
	java -cp "lib/jsettlers2/build/libs/JSettlers-2.2.00.jar" soc.server.SOCServer -o N7=t7 -Djsettlers.startrobots=3 -Djsettlers.allow.debug=Y -Djsettlers.bots.cookie=$(COOKIE) > /dev/null 2>&1

simulate:	build
	java -cp "lib/jsettlers2/build/libs/JSettlers-2.2.00.jar" soc.server.SOCServer -Djsettlers.allow.debug=Y -Djsettlers.startrobots=4 -Djsettlers.bots.percent3p=25 -Djsettlers.bots.botgames.parallel=1 -Djsettlers.bots.botgames.wait_sec=5 -Djsettlers.bots.botgames.total=10 -Djsettlers.bots.botgames.shutdown=Y -Djsettlers.bots.cookie=$(COOKIE) > /dev/null 2>&1

client:	build
	java -cp "lib/jsettlers2/build/libs/JSettlers-2.2.00.jar" soc.client.SOCPlayerClient localhost 8880

test:
	bash demo.sh

clean:
	gradle clean
