.PHONY: build run
name = battlesnake

run: build
	docker run --init -p 3000:3000 -e PORT=3000 $(name)

build:
	docker build -q -t $(name) .
