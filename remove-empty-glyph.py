#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import fontforge
import argparse


def remove_empty_glyphs(input, output):
    font = fontforge.open(input)
    code_points = []
    for glyph in font.glyphs():
        # 実際には glyph のデータが空になっていることがある
        if glyph.left_side_bearing != 0.0 and glyph.right_side_bearing != 0.0:
            pass
        else:
            font.removeGlyph(glyph)
    font.generate(output)
    font.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Make unicode-range from font.')
    parser.add_argument('input', nargs=1, help='path to input font file')
    parser.add_argument('output', nargs=1, help='path to output font file')
    args = parser.parse_args()
    input_file_path = args.input[0]
    output_file_path = args.output[0]
    remove_empty_glyphs(input_file_path, output_file_path)
