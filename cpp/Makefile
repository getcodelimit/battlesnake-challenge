.PHONY: run shell build tunnel

run: build
	docker run --platform=linux/amd64 -t --init -p 3000:3000 -e PORT=3000 battlesnake

shell: build
	docker run --platform=linux/amd64 -it --init -p 3000:3000 -e PORT=3000 battlesnake /bin/sh

build:
	docker build -q -t battlesnake .

tunnel:
	ssh -R battlesnake-challenge:80:localhost:3000 serveo.net
