FILES=src/main/java/bot/*.java

COOKIE=foobar

build: $(FILES)
	gradle jar

update:
	git submodule foreach git pull origin master

run:	build/
	java -cp "lib/jsettlers2/build/libs/JSettlers-2.2.00.jar:build/classes/java/main" -Djsettlers.debug.traffic=Y bot.NDRobotClient -c $(COOKIE)

serve:
	java -cp "lib/jsettlers2/build/libs/JSettlers-2.2.00.jar" soc.server.SOCServer -o N7=t7 -Djsettlers.startrobots=3 -Djsettlers.allow.debug=Y -Djsettlers.bots.cookie=$(COOKIE)

simulate:
	java -cp "lib/jsettlers2/build/libs/JSettlers-2.2.00.jar" soc.server.SOCServer -Djsettlers.allow.debug=Y -Djsettlers.startrobots=4 -Djsettlers.bots.botgames.total=7 -Djsettlers.bots.botgames.shutdown=Y -Djsettlers.bots.cookie=$(COOKIE)

clean:
	@-rm -f JSettlers.jar
	@-rm -rf build
