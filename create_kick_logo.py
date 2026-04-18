from PIL import Image

# Create a 64x64 image with bright green background
img = Image.new('RGB', (64, 64), color=(0, 255, 0))
pixels = img.load()

# Fill with green
for x in range(64):
    for y in range(64):
        pixels[x, y] = (0, 255, 0)

# Cut out black areas to create K shape
# Left vertical bar
for x in range(8, 20):
    for y in range(64):
        pixels[x, y] = (0, 255, 0)

# Top notch
for x in range(20, 32):
    for y in range(12, 24):
        pixels[x, y] = (0, 0, 0)

# Top right diagonal
for x in range(32, 64):
    for y in range(0, 16):
        pixels[x, y] = (0, 255, 0)

# Right stepped cutouts
for x in range(40, 64):
    for y in range(16, 24):
        pixels[x, y] = (0, 0, 0)

for x in range(44, 64):
    for y in range(24, 32):
        pixels[x, y] = (0, 0, 0)

for x in range(48, 64):
    for y in range(32, 40):
        pixels[x, y] = (0, 0, 0)

# Bottom notch
for x in range(20, 32):
    for y in range(40, 52):
        pixels[x, y] = (0, 0, 0)

# Bottom right diagonal
for x in range(32, 64):
    for y in range(48, 64):
        pixels[x, y] = (0, 255, 0)

# Middle notch in left bar
for x in range(10, 18):
    for y in range(28, 36):
        pixels[x, y] = (0, 0, 0)

img.save('resources/kick_logo.png')
print("Kick logo created: resources/kick_logo.png")
