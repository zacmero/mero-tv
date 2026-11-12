#!/usr/bin/env python3
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
import json, time, re, argparse
parser = argparse.ArgumentParser(description='Serve controlled browser fixtures on loopback only.')
parser.add_argument('--directory', default='.private/browser-fixtures')
parser.add_argument('--metrics', default='.private/playback-metrics.jsonl')
parser.add_argument('--port', type=int, default=8765)
args = parser.parse_args()
Path(args.metrics).parent.mkdir(parents=True, exist_ok=True)

class Handler(SimpleHTTPRequestHandler):

    def __init__(self, *a, **kw):
        super().__init__(*a, directory=args.directory, **kw)

    def send_head(self):
        self.remaining = None
        path = Path(self.translate_path(self.path))
        header = self.headers.get('Range')
        if not header or not path.is_file():
            return super().send_head()
        m = re.fullmatch('bytes=(\\d*)-(\\d*)', header)
        if not m:
            return super().send_head()
        size = path.stat().st_size
        start = int(m[1]) if m[1] else max(0, size - int(m[2]))
        end = min(size - 1, int(m[2])) if m[1] and m[2] else size - 1
        if start >= size or end < start:
            self.send_error(416)
            return None
        f = path.open('rb')
        f.seek(start)
        self.remaining = end - start + 1
        self.send_response(206)
        self.send_header('Content-Type', self.guess_type(str(path)))
        self.send_header('Accept-Ranges', 'bytes')
        self.send_header('Content-Range', f'bytes {start}-{end}/{size}')
        self.send_header('Content-Length', str(self.remaining))
        self.end_headers()
        return f

    def copyfile(self, source, output):
        if self.remaining is None:
            return super().copyfile(source, output)
        while self.remaining:
            b = source.read(min(65536, self.remaining))
            if not b:
                break
            output.write(b)
            self.remaining -= len(b)

    def do_POST(self):
        if self.path != '/metrics':
            self.send_error(404)
            return
        data = self.rfile.read(min(int(self.headers.get('Content-Length', '0')), 16384))
        try:
            r = json.loads(data)
            r['hostTime'] = time.time()
        except Exception:
            self.send_error(400)
            return
        with Path(args.metrics).open('a') as f:
            f.write(json.dumps(r) + '\n')
        self.send_response(204)
        self.end_headers()

    def log_message(self, *args):
        pass
ThreadingHTTPServer(('127.0.0.1', args.port), Handler).serve_forever()
