#!/usr/bin/env python3
"""Synthesize the game's sound cues into app/src/main/res/raw/*.wav.

Usage: python3 tools/make_sounds.py

Everything is generated from code (no samples), so the sounds carry no license. Mono 16-bit
22.05 kHz WAV, each well under a second except the win/lose jingles. Output is deterministic.
"""

import math
import os
import random
import struct
import wave

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "res", "raw")
RATE = 22050


def envelope(i, n, attack=0.005, release=0.08):
    t = i / RATE
    a = min(1.0, t / attack) if attack > 0 else 1.0
    r = min(1.0, (n - i) / (release * RATE)) if release > 0 else 1.0
    return a * r


def tone(freq, seconds, volume=0.5, shape="square", slide=0.0):
    n = int(seconds * RATE)
    out, phase = [], 0.0
    for i in range(n):
        f = freq + slide * (i / n)
        phase += 2 * math.pi * f / RATE
        s = math.sin(phase)
        if shape == "square":
            s = 1.0 if s >= 0 else -1.0
            s *= 0.6
        elif shape == "triangle":
            s = 2 / math.pi * math.asin(math.sin(phase))
        out.append(s * volume * envelope(i, n))
    return out


def noise_burst(seconds, volume, rng):
    n = int(seconds * RATE)
    return [rng.uniform(-1, 1) * volume * envelope(i, n, 0.001, seconds * 0.8) for i in range(n)]


def silence(seconds):
    return [0.0] * int(seconds * RATE)


def mix(*tracks):
    length = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(length)]


def dice(rng):
    # Five plastic clacks with short gaps: dice rattling in a cup.
    out = []
    for gap in (0.0, 0.05, 0.035, 0.06, 0.045):
        out += silence(gap) + mix(noise_burst(0.03, 0.55, rng), tone(rng.uniform(900, 1400), 0.03, 0.25, "triangle"))
    return out


def tweet(_):
    # Two quick upward chirps.
    return tone(1400, 0.07, 0.35, "triangle", slide=900) + silence(0.02) + tone(1700, 0.09, 0.35, "triangle", slide=1100)


def hit(rng):
    # Low thud with a noisy smack on top.
    return mix(tone(110, 0.28, 0.7, "sine", slide=-60), noise_burst(0.06, 0.4, rng))


def win(_):
    # Rising major arpeggio, then a held chord.
    notes = [523.25, 659.25, 783.99, 1046.5]
    out = []
    for f in notes:
        out += tone(f, 0.11, 0.35)
    return out + mix(*(tone(f, 0.45, 0.18) for f in notes))


def lose(_):
    # Sad trombone: three falling notes and a sagging last one.
    out = []
    for f in (392.0, 369.99, 349.23):
        out += tone(f, 0.22, 0.35, "triangle") + silence(0.03)
    return out + tone(329.63, 0.7, 0.35, "triangle", slide=-40)


def write(name, samples):
    path = os.path.join(OUT, f"sfx_{name}.wav")
    with wave.open(path, "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples))
    return path


def main():
    os.makedirs(OUT, exist_ok=True)
    rng = random.Random(7)
    for name, build in (("dice", dice), ("tweet", tweet), ("hit", hit), ("win", win), ("lose", lose)):
        print(write(name, build(rng)))


if __name__ == "__main__":
    main()
