from PIL import Image

# Create a 64x64 image for TikTok logo
img = Image.new('RGB', (64, 64), color=(0, 0, 0))
pixels = img.load()

# Draw TikTok musical note shape
# Cyan circle (left part)
for x in range(64):
    for y in range(64):
        # Cyan circle
        dx = x - 20
        dy = y - 40
        if dx*dx + dy*dy <= 16*16:
            pixels[x, y] = (0, 255, 255)
        # Pink circle (right part)
        dx = x - 28
        dy = y - 32
        if dx*dx + dy*dy <= 16*16:
            pixels[x, y] = (255, 0, 80)

# White highlight
for x in range(64):
    for y in range(64):
        dx = x - 18
        dy = y - 38
        if dx*dx + dy*dy <= 14*14:
            pixels[x, y] = (255, 255, 255)

# Vertical bar (stem)
for x in range(32, 40):
    for y in range(8, 56):
        pixels[x, y] = (0, 255, 255)

# Pink accent on stem
for x in range(36, 40):
    for y in range(8, 56):
        pixels[x, y] = (255, 0, 80)

img.save('resources/tiktok_logo.png')
print("TikTok logo created: resources/tiktok_logo.png")
