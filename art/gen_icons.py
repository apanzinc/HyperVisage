#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
# Copyright (C) 2026 apanzinc

"""把 art/icon.svg 转成 Android 自适应图标的三层矢量资源。

设计原则：严格等比映射，不缩放、不位移。
源 SVG 的 650x650 画布整体映射到 108dp 画布，图案本身在画布中占多大比例，
在图标里就占多大比例。这样换图时不用再手调尺寸。

图案 bbox 从路径坐标里自动求，所以新图标只要结构相同（两个 fill="url(#...)" 的 path，
第一个是主体图案、第二个是叠加渐变层）就能直接跑。

用法：cd art && python gen_icons.py
"""

import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'app', 'src', 'main', 'res', 'drawable')

# 源画布边长
SRC_CANVAS = 650.0
# 目标画布边长（自适应图标前景/背景层固定 108dp）
DST_CANVAS = 108.0
# 系统实际只显示 108dp 画布的中心 72dp，比例必须按这个算，不是 108
VISIBLE_CANVAS = 72.0
# 图案宽度占「可视区」的比例。0.66 对应图案宽 47.5dp，视觉上不会顶满
PATTERN_WIDTH_RATIO = 0.66


def parse_paths(svg):
    """取出所有 fill="url(#...)" 的 path，按出现顺序返回 [(d, gradient_id)]。"""
    return re.findall(r'<path d="([^"]+)"[^>]*fill="url\(#([^)]+)\)"', svg)


def bbox_of(path_d):
    """粗略求包围盒：把所有坐标数字成对读出来（足以应付本项目的图标）。

    只取绝对坐标命令后的点，曲线控制点也算进去，得到的框会略大一点，
    对居中定位来说误差可以忽略。
    """
    nums = [float(x) for x in re.findall(r'-?\d+(?:\.\d+)?', path_d)]
    xs = nums[0::2]
    ys = nums[1::2]
    return min(xs), min(ys), max(xs), max(ys)


def path_xml(d, fill_type=None, gradient=None, color=None):
    ft = f'        android:fillType="{fill_type}"\n' if fill_type else ''
    head = f'    <path\n        android:pathData="{d}"\n{ft}'
    if gradient:
        sx, sy, ex, ey, sc, ec = gradient
        return (
            head
            + '        >\n'
            + '        <aapt:attr name="android:fillColor">\n'
            + '            <gradient\n'
            + '                android:type="linear"\n'
            + f'                android:startX="{sx:.2f}"\n'
            + f'                android:startY="{sy:.2f}"\n'
            + f'                android:endX="{ex:.2f}"\n'
            + f'                android:endY="{ey:.2f}"\n'
            + f'                android:startColor="{sc}"\n'
            + f'                android:endColor="{ec}" />\n'
            + '        </aapt:attr>\n'
            + '    </path>\n'
        )
    return head + f'        android:fillColor="{color}" />\n'


def main():
    svg = open(os.path.join(HERE, 'icon.svg'), encoding='utf-8').read()
    paths = parse_paths(svg)
    if len(paths) < 1:
        raise SystemExit('icon.svg 里没找到 fill="url(#...)" 的 path，检查一下结构')

    d_main = paths[0][0]
    d_overlay = paths[1][0] if len(paths) > 1 else None

    minx, miny, maxx, maxy = bbox_of(d_main)
    w, h = maxx - minx, maxy - miny
    cx = (minx + maxx) / 2
    cy = (miny + maxy) / 2

    # 直接把图案宽度缩到目标值，再挪到画布中心，不依赖源画布尺寸
    target_w = VISIBLE_CANVAS * PATTERN_WIDTH_RATIO
    s = target_w / w
    tx = DST_CANVAS / 2 - cx * s
    ty = DST_CANVAS / 2 - cy * s

    print(f'图案 bbox {w:.2f} x {h:.2f}（含曲线控制点，实际可见范围略小）')
    print(f'缩放后 {w * s:.2f} x {h * s:.2f} dp')
    print(f'占可视区 {VISIBLE_CANVAS:.0f}dp 的 {w * s / VISIBLE_CANVAS * 100:.0f}%，'
          f'占整张 108dp 画布的 {w * s / DST_CANVAS * 100:.0f}%')

    header = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!-- 由 art/gen_icons.py 从 art/icon.svg 生成，请勿手改 -->\n'
        '<!-- 图标基于 MingCute (https://www.mingcute.com/, Apache-2.0) 修改而来 -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="108"\n'
        '    android:viewportHeight="108">\n'
    )
    group_open = (
        '    <group\n'
        f'        android:translateX="{tx:.3f}"\n'
        f'        android:translateY="{ty:.3f}"\n'
        f'        android:scaleX="{s:.6f}"\n'
        f'        android:scaleY="{s:.6f}">\n'
    )

    # 前景：主图案（白）+ 叠加渐变层（蓝）
    fg = header + group_open
    fg += path_xml(d_main, gradient=(cx, miny, cx, maxy, '#FFFFFFFF', '#CCFFFFFF'))
    if d_overlay:
        fg += path_xml(d_overlay, fill_type='evenOdd',
                       gradient=(cx, maxy, cx, miny, '#992B96FC', '#002B96FC'))
    fg += '    </group>\n</vector>\n'
    open(os.path.join(RES, 'ic_launcher_foreground.xml'), 'w', encoding='utf-8').write(fg)

    # 单色层（Android 13 主题图标），只留主图案剪影
    mo = header + group_open + path_xml(d_main, color='#FFFFFFFF') + '    </group>\n</vector>\n'
    open(os.path.join(RES, 'ic_launcher_monochrome.xml'), 'w', encoding='utf-8').write(mo)

    print('已生成 ic_launcher_foreground.xml / ic_launcher_monochrome.xml')


if __name__ == '__main__':
    main()
