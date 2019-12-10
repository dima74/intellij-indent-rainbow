w = 400
h = 400
n_rects = 8
m_rects = 4
rect_w = 80
rect_h = 40


def create_rect(x0, y0, color):
    padding = 2
    x = x0 + padding
    y = y0 + padding
    width = rect_w - padding * 2
    height = rect_h - padding * 2
    radius = 10
    return f'<rect x="{x}" y="{y}" width="{width}" height="{height}" rx="{radius}" fill="#{color}" />'


def create_rects(index_j, indexes_i, color):
    return [create_rect(rect_w // 2 + index_j * rect_w, rect_h * (i + 1), color) for i in indexes_i]


rects = [
    *create_rects(0, list(range(8)), '916eff'),
    *create_rects(1, list(range(5)) + [7], 'd602ee'),
    *create_rects(2, [1, 2, 7], 'ff9e22'),
]

lines = []
lines += rects
lines.append('<text x="300" y="290" text-anchor="middle" style="font-size: 160px; font-weight: bold;">IR</text>')

lines_string = '\t' + '\n\t'.join(lines)
svg = f"""
<?xml version="1.0" standalone="no"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" width="400" height="400">
{lines_string}
</svg>
""".strip()

with open('icon.svg', 'w') as f:
    print(svg, file=f)
