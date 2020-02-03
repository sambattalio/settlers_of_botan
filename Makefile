FILES=src/bot/NDBot.java

COOKIE=foobar

build:	JSettlers.jar $(FILES)
	javac -cp $< $(FILES)

run:	build
	java -cp "JSettlers.jar:src/" bot.NDBot -c $(COOKIE)

serve:
	java -cp "JSettlers.jar" soc.server.SOCServer -o N7=t7 -Djsettlers.startrobots=3 -Djsettlers.allow.debug=Y -Djsettlers.bots.cookie=$(COOKIE)

JSettlers.jar:
	wget http://nand.net/jsettlers/JSettlers.jar

clean:
	rm JSettlers.jar
	find src/ -name "*.class" | xargs rm