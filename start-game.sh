#!/bin/bash

set -e

if [ $# -eq 0 ]
then
    echo "usage: $0 <language>..."
    echo ""
    echo "languages:"
    for language in `find . -mindepth 1 -maxdepth 1 -type d | sort`
    do
        echo "  $language" | tr -d '\./'
    done
    exit 1
fi

stop_containers() {
    echo "Stopping Battlesnake docker container..."
    docker kill battlesnake >/dev/null 2>&1 || true
    docker rm battlesnake >/dev/null 2>&1 || true
    for language in "${global_args[@]}"
    do
        echo "Stopping $language docker container..."
        docker kill pl-battlesnake-$language >/dev/null 2>&1
        docker rm pl-battlesnake-$language >/dev/null 2>&1
    done
}

build_battlesnake_image() {
    echo "Building Battlesnake Server docker image..."
    docker build -q -t battlesnake . >/dev/null
}

run_battlesnake_container() {
    options=$@
    echo $options
    echo "Starting Battlesnake Server docker container..."
    docker run --name battlesnake -it battlesnake play -v -W 20 -H 20 $options
}

build_language_image() {
    language=$1
    echo "Building $language docker image..."
    (cd $language && 
        docker build -q -t pl-battlesnake-$language .) >/dev/null
}

run_language_container() {
    language=$1
    port=$2
    docker run --name pl-battlesnake-$language -d --init -p $port:$port \
        -e PORT=$port pl-battlesnake-$language >/dev/null
    sleep 3
}

global_args=("$@")

trap stop_containers EXIT

build_battlesnake_image
options=""
port=3000
for language in "${global_args[@]}"
do
    build_language_image $language
    run_language_container $language $port
    options="$options --name $language --url http://host.docker.internal:$port" 
    port=$((port + 1))
done
run_battlesnake_container $options
