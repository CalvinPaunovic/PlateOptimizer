from http.server import SimpleHTTPRequestHandler, HTTPServer
import os, json

# relativer Pfad vom Server-Skript zum Bilderordner
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMG_DIR = os.path.join(BASE_DIR, "IOFiles", "images")

class ImageHandler(SimpleHTTPRequestHandler):
    def do_GET(self):
        # JSON mit Bildnamen zurückgeben
        if self.path == "/images.json":
            exts = (".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp")

            if not os.path.isdir(IMG_DIR):
                self.send_error(404, f"Verzeichnis nicht gefunden: {IMG_DIR}")
                return

            files = [f for f in os.listdir(IMG_DIR) if f.lower().endswith(exts)]
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(files).encode("utf-8"))
            return

        # Bilder unter /IOFiles/images/ ausliefern
        if self.path.startswith("/IOFiles/images/"):
            file_name = os.path.basename(self.path)
            file_path = os.path.join(IMG_DIR, file_name)

            if os.path.isfile(file_path):
                self.send_response(200)
                if file_path.lower().endswith(".bmp"):
                    self.send_header("Content-Type", "image/bmp")
                else:
                    self.send_header("Content-Type", "application/octet-stream")
                self.end_headers()
                with open(file_path, "rb") as f:
                    self.wfile.write(f.read())
                return
            else:
                self.send_error(404, f"Datei nicht gefunden: {file_path}")
                return

        # alle anderen Dateien (z. B. index.html) normal ausliefern
        super().do_GET()

if __name__ == "__main__":
    PORT = 8000
    print(f"Server läuft auf http://localhost:{PORT}")
    print(f"Bilderverzeichnis: {IMG_DIR}")
    HTTPServer(("0.0.0.0", PORT), ImageHandler).serve_forever()
