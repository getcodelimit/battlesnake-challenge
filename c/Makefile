.PHONY: build run

run: build
	docker run -t --init -p 3000:3000 -e PORT=3000 battlesnake

build:
	docker build -q -t battlesnake .
