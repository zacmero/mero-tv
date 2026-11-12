#!/usr/bin/env python3
"""Reject failed, stalled, or excessively dropping real-device playback samples."""
import argparse
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('metrics', type=Path)
parser.add_argument('--run')
parser.add_argument('--max-drop-fraction', type=float, default=0.05)
args = parser.parse_args()
data = args.metrics.read_text().strip()
rows = json.loads(data) if data.startswith('[') else [json.loads(line) for line in data.splitlines() if line.strip()]
assert rows, 'No playback evidence'
run = args.run or rows[-1]['run']
rows = [row for row in rows if row.get('run') == run]
assert rows and not any(row.get('error') for row in rows), 'Player reported a media error'
assert any(row.get('clicks', 0) for row in rows), 'Play was never activated'
assert rows[-1].get('time', 0) >= 10, 'Video did not advance for ten seconds'
assert rows[-1].get('width', 0) >= 1920, 'Expected a 1080p source'
steady = [row for row in rows if row.get('time', 0) >= 3 and row.get('total', 0)]
assert len(steady) >= 2, 'No steady playback samples'
a, b = steady[0], steady[-1]
assert b['time'] - a['time'] >= 8, 'Steady observation interval too short'
frames, drops = b['total'] - a['total'], b['dropped'] - a['dropped']
assert frames > 0 and drops >= 0, 'No advancing frame counters'
assert drops / frames <= args.max_drop_fraction, 'Too many dropped frames'
print(f'PASS: {frames} advancing frames, {drops} dropped in {b["time"]-a["time"]:.1f}s')
