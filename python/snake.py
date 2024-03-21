#!/usr/bin/env python3
import os
import json
import math
from http.server import HTTPServer, BaseHTTPRequestHandler

class RequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        handle_get_meta_data(self)

    def do_POST(self):
        if self.path == '/start':
            handle_game_started(self)
        elif self.path == '/move': 
            handle_move(self)

def handle_get_meta_data(request_handler):
    content = json.dumps({
        'apiversion': '1',
        'author': 'robvanderleek',
        'version': '1.0',
        'color': '#3776ab',
        'head': 'safe',
        'tail': 'sharp'
    })
    request_handler.send_response(200)
    request_handler.end_headers()
    request_handler.wfile.write(content.encode('utf-8'))

def handle_game_started(request_handler):
    body = get_body(request_handler)
    game_id = body['game']['id']
    print(f'Game started: {game_id}');
    request_handler.send_response(200)
    request_handler.end_headers()

def get_body(request_handler):
    content_length = int(request_handler.headers['Content-Length'])
    post_data = request_handler.rfile.read(content_length)
    return json.loads(post_data.decode('utf-8'))

def handle_move(request_handler):
    body = get_body(request_handler)
    turn = body['turn']
    print(f'Turn: {turn}')
    board = body['board']
    snake = body['you']
    direction = get_direction(board, snake)
    request_handler.send_response(200)
    request_handler.end_headers()
    request_handler.wfile.write(f'{{"move": "{direction}"}}'.encode('utf-8'))

def get_direction(board, snake):
    head = snake['head']
    directions = preferred_directions(board, head)
    return select_direction(board, head, directions)

def preferred_directions(board, head):
    food = nearest_food(head, board['food'])
    result = []
    if head['x'] != food['x']:
        result.append('right' if head['x'] < food['x'] else 'left')
    if head['y'] != food['y']:
        result.append('up' if head['y'] < food['y'] else 'down')
    for d in ['right', 'down', 'left', 'up']:
        if not d in result:
            result.append(d)
    return result

def nearest_food(head, food):
    food.sort(key=lambda f: distance(head, f))
    return food[0]

def distance(p1, p2):
    return math.sqrt(pow(p2['x'] - p1['x'], 2) + pow(p2['y'] - p1['y'], 2))

def select_direction(board, head, directions):
    for d in directions:
        if d == 'left' and free_cell(board, head['x'] - 1, head['y']):
            return 'left'
        if d == 'right' and free_cell(board, head['x'] + 1, head['y']):
            return 'right'
        if d == 'down' and free_cell(board, head['x'], head['y'] - 1):
            return 'down'
        if d == 'up' and free_cell(board, head['x'], head['y'] + 1):
            return 'up'
    else:
        print('Oops')

def free_cell(board, x, y):
    for s in board['snakes']:
        if {'x': x, 'y': y} in s['body']:
            return False
    return x >= 0 and y >= 0 and x < board['width'] and y < board['height']

PORT=int(os.getenv('PORT', 3000))

print(f'Starting Battlesnake server on port: {PORT}')
server = HTTPServer(('0.0.0.0', PORT), RequestHandler)
server.serve_forever()
