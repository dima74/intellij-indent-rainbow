def rgba(r, g, b, a):
    a = int(a * 255)
    return f'0x{a:02X}{r:02X}{g:02X}{b:02X}'


# https://github.com/oderwat/vscode-indent-rainbow/blob/cdcede40f09201f9a63643a64039e4df155316de/extension.ts#L32-L38
default = [
    rgba(255, 255, 64, 0.07),
    rgba(127, 255, 127, 0.07),
    rgba(255, 127, 255, 0.07),
    rgba(79, 236, 236, 0.07),
]

# https://github.com/oderwat/vscode-indent-rainbow/pull/64/files
a = 0.20
pastel = [
    rgba(199, 206, 234, a),
    rgba(181, 234, 215, a),
    rgba(226, 240, 203, a),
    rgba(255, 218, 193, a),
    rgba(255, 183, 178, a),
    rgba(255, 154, 162, a),
]

print(*default, sep=', ')
print(*pastel, sep=', ')
