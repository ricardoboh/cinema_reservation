"""Repeatable source dependency check for the documented C08 controller boundary.

This lexical check covers explicit imports, qualified references, direct database
calls and exposed repository getters. It is not a Java AST or whole-program proof.
"""
from pathlib import Path
import re
import sys

FORBIDDEN = re.compile(
    r'com\.reservedbytes\.cinema_reservation\.(?:model|repository)\.'
    r'|(?:jakarta|javax)\.persistence\.|org\.springframework\.data\.'
    r'|org\.springframework\.jdbc\.|(?:java|javax)\.sql\.'
    r'|\b(?:EntityManager|JdbcTemplate|JpaRepository|CrudRepository)\b'
    r'|\.(?:findAll|findById|findLockedById|lockById|save|saveAll|delete|deleteAll|flush)\s*\('
)

def without_literals(source):
    return re.sub(r'//[^\n]*|/\*[\s\S]*?\*/|"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',
                  lambda match: ''.join('\n' if ch == '\n' else ' ' for ch in match.group()), source)

def violations(root):
    java = Path(root)/'backend/src/main/java/com/reservedbytes/cinema_reservation'
    errors = []
    controllers = sorted((java/'controller').glob('*.java'))
    if not controllers:
        return ['No controllers found at the expected project path.']
    for path in controllers:
        source = without_literals(path.read_text(encoding='utf-8-sig'))
        for match in FORBIDDEN.finditer(source):
            line = source[:match.start()].count('\n') + 1
            errors.append(f'{path.relative_to(root)}:{line}: persistence dependency in controller: {match.group()}')
    for path in (java/'service').glob('*.java'):
        source = without_literals(path.read_text(encoding='utf-8-sig'))
        if re.search(r'public\s+[\w.]*Repository\s+\w+\s*\(', source):
            errors.append(f'{path.relative_to(root)}: service exposes a repository.')
    return errors

if __name__ == '__main__':
    root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent
    errors = violations(root)
    if errors:
        print('\n'.join(errors))
        raise SystemExit(1)
    print('PASS: controller DTO/service boundary; no explicit entity, repository, JPA or JDBC dependency.')
