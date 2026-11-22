"""Local classroom demo for the FST Events Compose project. Python 3.8+."""
import contextlib
import json
import secrets
import subprocess
import threading
import time
import webbrowser
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
ADDR = '127.0.0.1:8787'
TOKEN = secrets.token_urlsafe(32)
GW = 'http://127.0.0.1:8090/api'
LOG = 'http://127.0.0.1:8092'
CASES = ['normal', 'balance', 'down', 'slow', 'invalid', 'url', 'db', 'limit', 'cqrs']
lock = threading.Lock()
state = dict(busy=False, rows=[], active='', phase='Prêt')
ids = {}


def stage(msg):
    with lock:
        state['phase'] = msg


def evidence(step, value):
    with lock:
        state['rows'][-1]['steps'].append([step, value])


def check(ok, msg):
    if not ok:
        raise RuntimeError(msg)


def command(args):
    result = subprocess.run(args, cwd=ROOT, capture_output=True, timeout=420)
    check(result.returncode == 0, (result.stderr or result.stdout).decode(errors='replace')[-1400:] or 'Commande en échec')
    return result.stdout.decode(errors='replace').strip()


def compose(*args, overlay=None):
    files = ['-f', 'compose.yml']
    if overlay:
        files += ['-f', overlay]
    return command(['docker', 'compose', *files, *args])


def deploy(service, overlay=None):
    compose('up', '-d', '--no-deps', '--wait', service, overlay=overlay)


def request(url, method='GET', data=None):
    body = json.dumps(data).encode() if data is not None else None
    req = Request(url, data=body, method=method, headers={'Content-Type': 'application/json'})
    try:
        with urlopen(req, timeout=40) as r:
            code, raw = r.status, r.read()
    except HTTPError as err:
        code, raw = err.code, err.read()
    except (OSError, URLError) as err:
        return 0, str(err)
    try:
        return code, json.loads(raw)
    except ValueError:
        return code, raw.decode(errors='replace')[:300]


def wait_for(fetch, predicate, label, seconds=120):
    stage(label)
    deadline = time.monotonic() + seconds
    while True:
        code, body = fetch()
        if predicate(code, body):
            evidence(label, {'HTTP': code, 'réponse': body})
            return body
        if time.monotonic() >= deadline:
            raise RuntimeError(f'{label} • dernier résultat HTTP {code} {str(body)[:500]}')
        time.sleep(2)


def stats(degraded=False):
    def valid(code, obj):
        expected = None if degraded else 50
        return (code == 200 and isinstance(obj, dict) and obj.get('degraded') is degraded
                and obj.get('nbParticipants') == 1 and 'coutLogistiqueReservee' in obj
                and obj['coutLogistiqueReservee'] == expected)
    return wait_for(lambda: request(f"{GW}/event-stats/{ids['eventId']}"), valid,
                    'Statistiques partielles' if degraded else 'Statistiques complètes')


def prepare():
    file = ROOT / 'reports/demo-ids.json'
    if not file.exists():
        stage('Création des données via smoke.ps1')
        command(['powershell.exe', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', 'scripts/smoke.ps1'])
    ids.update(json.loads(file.read_text(encoding='utf-8-sig')))
    stats()


def checked(url, method, expected, data=None):
    code, body = request(url, method, data)
    evidence(method + ' ' + url.split('/', 3)[-1], {'HTTP': code, 'réponse': body})
    check(code == expected, f'HTTP {code} reçu, {expected} attendu')
    return body


def fault(mode):
    return checked(LOG + '/demo/fault?mode=' + mode, 'POST', 200)


def failure(case):
    service = 'mysql-logistics' if case == 'db' else 'logistics-service'
    query = LOG + '/logistics/queries/' + str(ids['logisticsId'])
    with contextlib.ExitStack() as cleanup:
        if case in ('down', 'db'):
            cleanup.callback(compose, 'start', service)
            compose('stop', service)
            evidence('Conteneur arrêté', service)
        elif case == 'url':
            cleanup.callback(deploy, 'event-service')
            deploy('event-service', 'compose.bad-url.yml')
            checked(query, 'GET', 200)
        else:
            previous, config = request(LOG + '/demo/fault')
            if previous == 404:
                cleanup.callback(deploy, 'logistics-service')
                deploy('logistics-service', 'compose.demo.yml')
            else:
                check(previous == 200 and config == {'mode': 'none'}, 'Vérifiez le mode initial du filtre logistique.')
            cleanup.callback(fault, 'none')
            fault(case)
            start = time.monotonic()
            payload = checked(query, 'GET', 200)
            elapsed = time.monotonic() - start
            evidence('Durée de la lecture directe', f'{elapsed:.2f} s')
            check(elapsed >= 5 if case == 'slow' else payload == [{'invalid': True}], 'Injection non observée')
        if case == 'db':
            wait_for(lambda: request(query), lambda c, b: c >= 400, 'Erreur de lecture du modèle MySQL')
        stats(True)
        stage('Restauration de la dépendance')
    stats()


def balance():
    running = compose('--profile', 'scale', 'ps', '--status', 'running', '-q', 'participant-service-2')
    with contextlib.ExitStack() as cleanup:
        if not running:
            cleanup.callback(compose, '--profile', 'scale', 'stop', 'participant-service-2')
            compose('--profile', 'scale', 'up', '-d', '--wait', '--no-deps', 'participant-service-2')
        seen = []
        def sample():
            code, body = request(GW + '/demo/participant-instance')
            if code == 200 and isinstance(body, dict) and body.get('instance'):
                seen.append(body['instance'])
            return code, seen.copy()
        wait_for(sample, lambda c, items: len(set(items)) >= 2, 'Deux instances observées via la Gateway')


def cqrs():
    url = GW + '/logistics/commands'
    receipt = checked(url, 'POST', 202, dict(description='Ressource soutenance', reserve=False, prixUnit=3, quantite=1))
    uid = receipt['idLog']
    target = url + '/' + uid
    query = GW + '/logistics/queries/' + uid
    with contextlib.ExitStack() as cleanup:
        def remove():
            checked(target, 'DELETE', 202)
        cleanup.callback(remove)
        wait_for(lambda: request(query), lambda c, b: c == 200 and b.get('idLog') == uid, 'Projection MySQL visible')
        checked(target + '/reservation?reserved=true', 'PUT', 202)
        cleanup.callback(checked, target + '/reservation?reserved=false', 'PUT', 202)
        checked(target + '/reservation?reserved=true', 'PUT', 409)
        checked(target, 'DELETE', 409)
    wait_for(lambda: request(query), lambda c, b: c == 404, 'Suppression projetée')


def rate_limit():
    with ThreadPoolExecutor(max_workers=15) as pool:
        counts = Counter(pool.map(lambda _: request(GW + '/event-stats/summary')[0], range(15)))
    evidence('15 requêtes concurrentes • nombre par statut HTTP', dict(counts))
    check(counts[429] and counts[200] and sum(counts[k] for k in (200, 429)) == 15,
          'Le lot doit contenir des réponses 200 et 429. Vérifiez le limiteur puis relancez.')


def run(cases):
    try:
        for case in cases:
            with lock:
                state.update(active=case, phase='Vérification de l’état initial')
                state['rows'].append(dict(case=case, status='running', steps=[]))
            prepare()
            if case == 'balance':
                balance()
            elif case == 'limit':
                rate_limit()
            elif case == 'cqrs':
                cqrs()
            elif case != 'normal':
                failure(case)
            with lock:
                state['rows'][-1]['status'] = 'pass'
    except Exception as exc:
        evidence('À vérifier', str(exc))
        with lock:
            state['rows'][-1]['status'] = 'fail'
    finally:
        with lock:
            state.update(busy=False, active='', phase='Exécution terminée')


class Handler(BaseHTTPRequestHandler):
    def send(self, code, data, mime):
        self.send_response(code)
        self.send_header('Content-Type', mime)
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Frame-Options', 'DENY')
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.headers.get('Host') != ADDR:
            return self.send_error(403)
        if self.path == '/':
            page = (HERE / 'index.html').read_text('utf-8').replace('__TOKEN__', TOKEN)
            self.send(200, page.encode(), 'text/html; charset=utf-8')
        elif self.path == '/state':
            with lock:
                data = json.dumps(state).encode()
            self.send(200, data, 'application/json')
        else:
            self.send_error(404)

    def do_POST(self):
        case = self.path[5:] if self.path.startswith('/run/') else ''
        if (self.headers.get('Host') != ADDR or self.headers.get('Origin') != 'http://' + ADDR
                or self.headers.get('X-Demo-Key') != TOKEN):
            return self.send_error(403)
        if case not in CASES + ['all']:
            return self.send_error(404)
        with lock:
            if state['busy']:
                return self.send_error(409)
            state.update(busy=True, rows=[], phase='Démarrage')
        threading.Thread(target=run, args=(CASES if case == 'all' else [case],)).start()
        self.send(202, b'{}', 'application/json')


if __name__ == '__main__':
    check(all((ROOT / p).exists() for p in ('compose.yml', 'compose.demo.yml', 'compose.bad-url.yml', 'scripts/smoke.ps1')),
          'Placez le dossier demo-console à côté de compose.yml.')
    server = ThreadingHTTPServer(('127.0.0.1', 8787), Handler)
    print('Console locale : http://' + ADDR + ' • Gardez cette fenêtre ouverte.', flush=True)
    webbrowser.open('http://' + ADDR)
    server.serve_forever()
