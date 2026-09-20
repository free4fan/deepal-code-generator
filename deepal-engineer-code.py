#!/usr/bin/env python3
"""Deepal S05 Engineer Code Generator v3.1
Формула: last8 -> converted (B2D mapping) -> encrypt (shuffle permutation)

Usage:
  python3 deepal-engineer-code.py VIN [VIN...]   one code per line
  python3 deepal-engineer-code.py                interactive REPL ('q' quits)
  python3 deepal-engineer-code.py --selftest     verify against tests/fixtures.txt
"""

import sys
from pathlib import Path

B2D = {
    'A': 0, 'B': 1, 'C': 2, 'D': 3, 'E': 4, 'F': 5, 'G': 6, 'H': 7,
    'I': 8, 'J': 9, 'K': 9, 'L': 8, 'M': 7, 'N': 6, 'O': 5, 'P': 4,
    'Q': 3, 'R': 2, 'S': 1, 'T': 0, 'U': 0, 'V': 1, 'W': 2, 'X': 3,
    'Y': 4, 'Z': 5
}

SHUFFLE = [2, 5, 1, 4, 7, 0, 3, 6]

VALID_CHARS = set('0123456789')
for _c in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ':
    if _c not in 'IOQ':
        VALID_CHARS.add(_c)


class VinError(ValueError):
    pass


def validate(vin):
    if len(vin) < 8:
        raise VinError(f"VIN too short (need at least 8 characters, got {len(vin)})")
    for i, c in enumerate(vin):
        if c not in VALID_CHARS:
            raise VinError(f"invalid character {c!r} at position {i + 1} "
                           f"(VIN uses 0-9 and A-Z without I, O, Q)")


def convert(last8):
    return [B2D[c] if c.isalpha() else int(c) for c in last8]


def encrypt(c):
    return ''.join(str(c[i]) for i in SHUFFLE)


def gen_code(vin):
    vin = vin.strip().upper()
    validate(vin)
    return f"*#{encrypt(convert(vin[-8:]))}#*"


def selftest(fixtures_path):
    bad = 0
    for line in fixtures_path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#') or ',' not in line:
            continue
        vin, expected = line.split(',', 1)
        actual = gen_code(vin)
        if actual != expected:
            bad += 1
            print(f"FAIL {vin}: expected {expected}, got {actual}")
    total = sum(1 for l in fixtures_path.read_text().splitlines()
                if l.strip() and not l.startswith('#') and ',' in l)
    print(f"selftest: {total - bad}/{total} fixtures OK")
    return 1 if bad else 0


def main(argv):
    if len(argv) > 1 and argv[1] == '--selftest':
        path = Path(argv[2]) if len(argv) > 2 else Path(__file__).parent / 'tests' / 'fixtures.txt'
        return selftest(path)

    if len(argv) > 1:
        for vin in argv[1:]:
            try:
                print(gen_code(vin))
            except VinError as e:
                print(f"error: {e}", file=sys.stderr)
                return 1
        return 0

    while True:
        try:
            vin = input("VIN (или q): ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        except Exception:
            break
        if vin.lower() == 'q':
            break
        if not vin:
            continue
        try:
            print(f"  {gen_code(vin)}\n")
        except VinError as e:
            print(f"  error: {e}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
