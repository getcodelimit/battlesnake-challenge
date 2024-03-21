#!/usr/bin/env node
const http = require('http');

const requestHandler = (req, res) => {
    if (req.url === '/') {
        handleGetMetaData(req, res);
    } else if (req.url === '/start') {
        handleWithBody(req, res, handleGameStart);
    } else if (req.url === '/move') {
        handleWithBody(req, res, handleMove);
    }
}

const handleGetMetaData = (req, res) => {
    const response = {
        'apiversion': '1',
        'author': 'robvanderleek',
        'color': '#C00000',
        'head': 'caffeine',
        'tail': 'curled',
        'version': '1.0'
    }
    res.end(JSON.stringify(response));
}

const handleWithBody = (req, res, handler) => {
    let body = [];
    req.on('data', (chunk) => {
        body.push(chunk);
    }).on('end', () => {
        body = Buffer.concat(body).toString();
        handler(req, res, JSON.parse(body));
    });
}

const handleGameStart = (req, res, body) => {
    console.log(`Game started: ${body.game.id}`);
    res.writeHead(200);
}

const handleMove = (req, res, body) => {
    const {turn, you: snake, board} = body;
    console.log(`/move called for turn: ${turn}`);
    const dir = getDirection(board, snake);
    res.setHeader('Content-Type', 'application/json');
    res.writeHead(200);
    res.end(`{"move": "${dir}"}`);
}

const getDirection = (board, snake) => {
    const directions = preferredDirections(board, snake.head)
    return selectDirection(board, snake.head, directions)
}

const preferredDirections = (board, head) => {
    const food = nearestFood(head, board.food);
    let result = []
    if (head.y != food.y) {
        result.push(head.y < food.y ? 'up' : 'down');
    }
    if (head.x != food.x) {
        result.push(head.x < food.x ? 'right' : 'left');
    }
    return result.concat(['left', 'up', 'right', 'down']
        .filter(d => !result.includes(d)));
}

const nearestFood = (head, food) => food.sort((f1, f2) => distance(head, f1) - distance(head, f2))[0]

const distance = (p1, p2) => Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));

const selectDirection = (board, head, directions) => {
    for (const d of directions) {
        if (d === 'left' && freeCell(board, {x: head.x - 1, y: head.y})) {
            return 'left'
        } else if (d === 'right' && 
                freeCell(board, {x: head.x + 1, y: head.y})) {
            return 'right'
        } else if (d === 'down' && 
                freeCell(board, {x: head.x, y: head.y - 1})) {
            return 'down'
        } else if (d === 'up' && freeCell(board, {x: head.x, y: head.y + 1})) {
            return 'up'
        }
    }
    console.log('Oops');
}

const freeCell = (board, c) => {
    if (board.snakes.flatMap(s => s.body).some(p => equal(c, p))) {
        return false;
    }
    return c.x >= 0 && c.y >= 0 && c.x < board.width && c.y < board.height;
}

const equal = (p1, p2) => p1.x === p2.x && p1.y === p2.y;

const PORT = process.env.PORT || 3000;
const server = http.createServer(requestHandler);
server.listen(PORT, '0.0.0.0', () => console.log(`Battlesnake server running on port: ${PORT}`));
