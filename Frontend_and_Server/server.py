# Minimal HTTP server for PlateOptimizer Output Viewer
# Usage: python server.py [port]
# Default port: 9000

import http.server
import socketserver
import sys

PORT = 9000
if len(sys.argv) > 1:
    try:
        PORT = int(sys.argv[1])
    except ValueError:
        pass

# Receives HTTP-requests and delivers files from the current directory
Handler = http.server.SimpleHTTPRequestHandler

# Start the TCP-server. Accepts connections and forwards them to the handler.
with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print(f"Serving at http://localhost:{PORT}/output-viewer.html")
    print("Press Ctrl+C to stop.")
    httpd.serve_forever()
