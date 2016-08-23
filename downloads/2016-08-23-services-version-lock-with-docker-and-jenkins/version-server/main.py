#!/usr/bin/env python

from os import environ
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import SocketServer
import json

data = { 'current': {}, 'versions': [] }

class Handler(BaseHTTPRequestHandler):

    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()

    def do_GET(self):
        global data
        self._set_headers()
        if self.path == '/history':
            self.wfile.write(json.dumps(data['versions']))
        else:
            self.wfile.write(json.dumps(data['current']))


    def do_POST(self):
        global data
        self._set_headers()
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        try:
            json_data = json.loads(post_data)
            data['current'] = json_data
            data['versions'].append(json_data)
        except:
            self.send_response(400)
        self.wfile.write(json.dumps(data['current']))

def run(port):
    server = HTTPServer(('', port), Handler)
    server.serve_forever()

if __name__ == '__main__':
    port = int(environ.get('PORT', 8151))
    print('Running at port %d' % port)
    run(port)

