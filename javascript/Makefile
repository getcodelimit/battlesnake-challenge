.PHONY: build run tunnel

run: build
	docker run --init -p 3000:3000 -e PORT=3000 battlesnake

build:
	docker build -q -t battlesnake .

tunnel:
	ssh -R battlesnake-challenge:80:localhost:3000 serveo.net
