"""Remove the Mojo-specific trademark paragraph added by the shared EPUB builder."""

from pathlib import Path
from zipfile import ZIP_DEFLATED, ZIP_STORED, ZipFile
import os
import tempfile


EPUB = Path(__file__).parent / "dist" / "microproject-user-guide-ja.epub"
MARKER = '<p class="trademark">'


def main() -> None:
	with ZipFile(EPUB, "r") as source:
		entries = [(item, source.read(item.filename)) for item in source.infolist()]

	updated = []
	removed = 0
	for item, data in entries:
		if item.filename == "OEBPS/copyright.xhtml":
			content = data.decode("utf-8")
			start = content.find(MARKER)
			if start >= 0:
				end = content.find("</p>", start)
				if end < 0:
					raise ValueError("Unclosed template trademark paragraph")
				content = content[:start] + content[end + len("</p>"):]
				removed += 1
			data = content.encode("utf-8")
		updated.append((item, data))

	if removed != 1:
		raise ValueError(f"Expected to remove one template trademark paragraph; removed {removed}")

	fd, temp_name = tempfile.mkstemp(suffix=".epub", dir=EPUB.parent)
	os.close(fd)
	try:
		with ZipFile(temp_name, "w") as target:
			for item, data in updated:
				compression = ZIP_STORED if item.filename == "mimetype" else ZIP_DEFLATED
				target.writestr(item, data, compress_type=compression)
		os.replace(temp_name, EPUB)
	finally:
		if os.path.exists(temp_name):
			os.unlink(temp_name)

	with ZipFile(EPUB, "r") as result:
		if any(b"Mojo" in result.read(name) or b"mojo" in result.read(name)
				for name in result.namelist() if name.endswith((".xhtml", ".opf"))):
			raise ValueError("Mojo reference remains in EPUB content or metadata")
	print(f"Removed template trademark paragraph: {EPUB}")


if __name__ == "__main__":
	main()
