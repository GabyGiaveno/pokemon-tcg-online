"""Smoke E2E: juega una partida completa contra el backend real (sin mocks).

Uso: levantar el BE (perfil dev) y correr  python scripts/smoke-e2e.py
Registra 2 jugadores, arma mazos validos (16 basicos + 44 energias), crea/une
partida, hace el SETUP de ambos, y juega turnos (energia + ataque) hasta que
alguien gana, resolviendo la seleccion post-KO si aparece. Solo stdlib.
"""
import json
import sys
import time
import urllib.request
import urllib.error

BASE = "http://localhost:8080"
FAILS = []


def call(method, path, token=None, body=None, expect_ok=True):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(req, data=data, timeout=90) as resp:
            raw = resp.read().decode()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        detail = e.read().decode()[:300]
        msg = f"{method} {path} -> HTTP {e.code}: {detail}"
        if expect_ok:
            FAILS.append(msg)
            print("  [FAIL]", msg)
        return {"__http_error__": e.code, "detail": detail}


def action(game_id, token, payload, label):
    resp = call("POST", f"/api/games/{game_id}/actions", token, payload)
    ok = resp.get("success", False)
    err = resp.get("error") or resp.get("errorMessage") or resp.get("detail")
    print(f"  {label}: {'OK' if ok else 'rechazada -> ' + str(err)}")
    return resp


def main():
    stamp = str(int(time.time()))[-7:]
    players = []
    for n in (1, 2):
        body = {"username": f"smoke{n}_{stamp}", "email": f"smoke{n}_{stamp}@test.com",
                "password": "Pass123!"}
        r = call("POST", "/api/auth/register", body=body)
        players.append({"id": r["id"], "token": r["token"], "name": body["username"]})
        print(f"[1] registrado {body['username']} (id {r['id']})")

    # ── Cartas: 4 basicos distintos con ataque de costo 1, + energias del mismo tipo ──
    print("[2] cargando cartas xy1 (primer fetch puede tardar: API externa)...")
    page = None
    for attempt in range(1, 6):
        page = call("GET", "/api/cards?size=250", players[0]["token"], expect_ok=(attempt == 5))
        if "data" in page:
            break
        print(f"    intento {attempt} fallo (API externa); reintento en 20s...")
        time.sleep(20)
    cards = page["data"]
    print(f"    {len(cards)} cartas en cache")

    energies = {}
    for c in cards:
        if c.get("supertype") == "Energy" and "Basic" in (c.get("subtypes") or []):
            for t in (c.get("types") or []):
                energies.setdefault(t, c["id"])
    # fallback: la API trae las energias sin types a veces -> deducir por nombre
    if not energies:
        for c in cards:
            if c.get("supertype") == "Energy" and "Energy" in (c.get("name") or ""):
                color = c["name"].replace(" Energy", "")
                energies.setdefault(color, c["id"])
    print(f"    energias basicas: {sorted(energies)}")

    def first_attack_cost(c):
        # solo ataques que HACEN dano (>0): los de solo-efecto no terminan nunca la partida
        try:
            atks = json.loads(c.get("attacks") or "[]")
            if not atks:
                return None
            dmg = "".join(ch for ch in (atks[0].get("damage") or "") if ch.isdigit())
            if not dmg or int(dmg) <= 0:
                return None
            return atks[0]["cost"]
        except Exception:
            return None

    basics = []
    for c in cards:
        if c.get("supertype") != "Pokémon" or "Basic" not in (c.get("subtypes") or []):
            continue
        cost = first_attack_cost(c)
        if not cost or len(cost) != 1:
            continue
        need = cost[0]
        if need == "Colorless" or need in energies:
            basics.append((c["id"], c["name"], need))
    # 4 nombres distintos que compartan tipo de energia (o Colorless)
    chosen, seen, energy_type = [], set(), None
    for cid, name, need in basics:
        t = None if need == "Colorless" else need
        if name in seen:
            continue
        if t is not None:
            if energy_type is None:
                energy_type = t
            elif t != energy_type:
                continue
        chosen.append((cid, name))
        seen.add(name)
        if len(chosen) == 4:
            break
    if energy_type is None:
        energy_type = sorted(energies)[0]
    energy_id = energies[energy_type]
    print(f"    basicos: {[n for _, n in chosen]} | energia: {energy_type} ({energy_id})")
    assert len(chosen) == 4, "no se encontraron 4 basicos compatibles"

    # ── Linea evolutiva: Stage-1 cuyo evolvesFrom sea uno de nuestros basicos atacantes ──
    by_name = {c["name"]: c for c in cards}
    stage_by_base = {}  # nombre del basico -> (stage_id, stage_name)
    for c in cards:
        ef = c.get("evolvesFrom")
        if ef and any(s.startswith("Stage") for s in (c.get("subtypes") or [])):
            stage_by_base.setdefault(ef, (c["id"], c["name"]))
    evo_base_id = evo_stage_id = evo_stage_name = None
    for cid, name in chosen:
        if name in stage_by_base:
            evo_base_id = cid
            evo_stage_id, evo_stage_name = stage_by_base[name]
            print(f"    evolucion: {name} ({cid}) -> {evo_stage_name} ({evo_stage_id})")
            break

    # ── Entrenadores Item/Supporter (sin objetivo complejo) para ejercitar PLAY_TRAINER ──
    def trainer_subtype(c):
        for s in (c.get("subtypes") or []):
            if s in ("Item", "Supporter", "Stadium"):
                return s
        return None
    trainers = [c for c in cards if c.get("supertype") == "Trainer"
                and trainer_subtype(c) in ("Item", "Supporter")]
    trainer_pick = trainers[:2]
    trainer_ids = {c["id"] for c in trainer_pick}
    trainer_sub = {c["id"]: trainer_subtype(c) for c in trainer_pick}
    print(f"    entrenadores: {[(c['id'], c['name']) for c in trainer_pick]}")

    # ── Mazo de 60: 16 basicos + 4 stage-1 + 2x2 trainers + energias ──
    deck_cards = [{"cardId": cid, "quantity": 4} for cid, _ in chosen]
    if evo_stage_id:
        deck_cards.append({"cardId": evo_stage_id, "quantity": 4})
    for c in trainer_pick:
        deck_cards.append({"cardId": c["id"], "quantity": 4})  # max copias: mejora el robo en el smoke
    used = sum(d["quantity"] for d in deck_cards)
    deck_cards.append({"cardId": energy_id, "quantity": 60 - used})

    decks = []
    for p in players:
        d = call("POST", "/api/decks", p["token"], {"name": "Smoke " + p["name"], "cards": deck_cards})
        valid = d.get("valid", d.get("isValid"))
        print(f"[3] mazo de {p['name']}: id {d.get('id')} valido={valid}")
        if not valid:
            FAILS.append(f"mazo invalido: {d.get('validationErrors')}")
        decks.append(d["id"])

    g = call("POST", "/api/games", players[0]["token"], {"deckId": decks[0]})
    game_id = g["gameId"]
    print(f"[4] partida {game_id} creada (status {g.get('status')})")
    j = call("POST", f"/api/games/{game_id}/join", players[1]["token"], {"deckId": decks[1]})
    print(f"    join p2 -> status {j.get('status')}")

    def state(p):
        return call("GET", f"/api/games/{game_id}/state", p["token"])

    # ── SETUP: cada jugador coloca activo + 1 banca y confirma ──
    basic_ids = {cid for cid, _ in chosen}
    for p in players:
        s = state(p)
        hand = s["myField"]["hand"]
        basics_in_hand = [c for c in hand if c["cardId"] in basic_ids]
        print(f"[5] SETUP {p['name']}: mano {len(hand)}, basicos {len(basics_in_hand)}")
        if not basics_in_hand:
            FAILS.append(f"{p['name']} sin basicos en mano (mulligan fallback)")
            continue
        # El primer colocado queda de ACTIVO: priorizamos el basico evolucionable
        basics_in_hand.sort(key=lambda c: 0 if c["cardId"] == evo_base_id else 1)
        for inst in basics_in_hand[:2]:  # activo + 1 banca (para ejercitar la promocion post-KO)
            action(game_id, p["token"], {"type": "SETUP_PLACE_POKEMON",
                                         "cardInstanceId": inst["instanceId"]},
                   f"colocar {inst['cardId']}")
        action(game_id, p["token"], {"type": "END_TURN"}, "confirmar setup")

    # ── Turnos hasta FINISHED (con cobertura de playBasic-MAIN / trainer / evolve / retreat) ──
    print("[6] jugando turnos...")
    cover = {"playbasic": False, "trainer": False, "evolve": False, "retreat": False}
    # 'attempted' = la accion se despacho con precondiciones validas (para distinguir
    # un rechazo real del backend de la simple falta de oportunidad por el robo).
    attempted = {"playbasic": False, "trainer": False, "evolve": False, "retreat": False}

    def first_free_bench(bench):
        bench = bench or []
        for i in range(5):
            if i >= len(bench) or bench[i] is None:
                return i
        return None

    def trainer_action(sub):
        return {"Item": "PLAY_ITEM", "Stadium": "PLAY_STADIUM"}.get(sub, "PLAY_SUPPORTER")

    for turn in range(1, 101):
        s = state(players[0])
        status = s.get("status")
        if status == "FINISHED":
            winner = s.get("winnerId")
            wname = next((p["name"] for p in players if p["id"] == winner), winner)
            print(f"[7] PARTIDA TERMINADA: gana {wname} por {s.get('finishedReason')}")
            break
        pending = s.get("pendingSelection")
        if pending:
            owner = next(p for p in players if p["id"] == pending["ownerPlayerId"])
            print(f"  [turno {turn}] pendingSelection de {owner['name']}: {pending['validOptions']}")
            action(game_id, owner["token"], {"type": "RESOLVE_SELECTION", "benchIndex": 0},
                   "resolver seleccion")
            continue
        current = next((p for p in players if p["id"] == s.get("currentPlayerId")), None)
        if current is None:
            FAILS.append(f"currentPlayerId desconocido: {s.get('currentPlayerId')}")
            break
        tok = current["token"]
        ms = state(current)["myField"]
        active = ms.get("activePokemon")
        hp = active.get("hp") if active else "-"
        print(f"  [turno {turn}] {current['name']} (hp activo {hp}, "
              f"energias {len(active.get('attachedEnergies') or []) if active else 0}, mano {len(ms['hand'])})")

        # (a) PLAY_BASIC_POKEMON en MAIN — dos sub-pasos independientes:
        #     (a1) cobertura: jugar cualquier basico de la mano a una banca libre.
        if not cover["playbasic"]:
            basic_card = next((c for c in ms["hand"] if c["cardId"] in basic_ids), None)
            free_idx = first_free_bench(ms.get("bench"))
            if basic_card and free_idx is not None:
                attempted["playbasic"] = True
                r = action(game_id, tok, {"type": "PLAY_BASIC_POKEMON",
                                          "cardInstanceId": basic_card["instanceId"],
                                          "targetPosition": f"BENCH_{free_idx}"}, "jugar basico (MAIN)")
                if r.get("success"):
                    cover["playbasic"] = True
                ms = state(current)["myField"]
        #     (a2) sembrar el basico evolucionable en banca para tener objetivo de EVOLVE despues.
        if not cover["evolve"] and evo_base_id:
            in_play_base = (ms.get("activePokemon") or {}).get("cardId") == evo_base_id or \
                any(b and b.get("cardId") == evo_base_id for b in (ms.get("bench") or []))
            base_card = next((c for c in ms["hand"] if c["cardId"] == evo_base_id), None)
            free_idx = first_free_bench(ms.get("bench"))
            if base_card and not in_play_base and free_idx is not None:
                attempted["playbasic"] = True
                r = action(game_id, tok, {"type": "PLAY_BASIC_POKEMON",
                                          "cardInstanceId": base_card["instanceId"],
                                          "targetPosition": f"BENCH_{free_idx}"}, "sembrar base evol (MAIN)")
                if r.get("success"):
                    cover["playbasic"] = True
                ms = state(current)["myField"]

        # (b) PLAY_TRAINER (Item / Supporter)
        if not cover["trainer"]:
            tr = next((c for c in ms["hand"] if c["cardId"] in trainer_ids), None)
            if tr:
                atype = trainer_action(trainer_sub.get(tr["cardId"], "Item"))
                attempted["trainer"] = True
                r = action(game_id, tok, {"type": atype, "cardInstanceId": tr["instanceId"]},
                           f"jugar entrenador {tr['cardId']} ({atype})")
                if r.get("success"):
                    cover["trainer"] = True
                ms = state(current)["myField"]

        # (c) ATTACH_ENERGY al activo (1 por turno)
        active = ms.get("activePokemon")
        energy_in_hand = next((c for c in ms["hand"] if c["cardId"] == energy_id), None)
        if energy_in_hand and active:
            action(game_id, tok, {"type": "ATTACH_ENERGY",
                                  "cardInstanceId": energy_in_hand["instanceId"],
                                  "targetPosition": "ACTIVE"}, "adjuntar energia")
            ms = state(current)["myField"]

        # (d) EVOLVE_POKEMON: stage-1 en mano sobre el basico correspondiente (activo o banca)
        if not cover["evolve"] and evo_stage_id:
            stage = next((c for c in ms["hand"] if c["cardId"] == evo_stage_id), None)
            if stage:
                active = ms.get("activePokemon")
                target_pos = None
                if active and active.get("cardId") == evo_base_id:
                    target_pos = "ACTIVE"
                else:
                    for i, b in enumerate(ms.get("bench") or []):
                        if b and b.get("cardId") == evo_base_id:
                            target_pos = f"BENCH_{i}"
                            break
                if target_pos:
                    attempted["evolve"] = True
                    r = action(game_id, tok, {"type": "EVOLVE_POKEMON",
                                              "cardInstanceId": stage["instanceId"],
                                              "targetPosition": target_pos},
                               f"evolucionar a {evo_stage_name} ({target_pos})")
                    if r.get("success"):
                        cover["evolve"] = True
                    ms = state(current)["myField"]

        # (e) RETREAT: activo con energia + banca disponible (una sola vez)
        if not cover["retreat"]:
            active = ms.get("activePokemon")
            bench = ms.get("bench") or []
            bidx = next((i for i, b in enumerate(bench) if b), None)
            # >=2 energias cubre el costo de retirada habitual (1-2); evita falsos rechazos.
            if active and len(active.get("attachedEnergies") or []) >= 2 and bidx is not None:
                attempted["retreat"] = True
                r = action(game_id, tok, {"type": "RETREAT", "benchIndex": bidx}, "retirar activo")
                if r.get("success"):
                    cover["retreat"] = True
                ms = state(current)["myField"]

        # (f) USE_ATTACK para que la partida progrese y termine
        active = ms.get("activePokemon")
        attached = len(active.get("attachedEnergies") or []) if active else 0
        if active and attached >= 1:
            r = action(game_id, tok, {"type": "USE_ATTACK", "attackIndex": 0}, "atacar")
            if r.get("success"):
                continue  # el ataque cierra el turno solo
        action(game_id, tok, {"type": "END_TURN"}, "terminar turno")
    else:
        FAILS.append("la partida no termino en 100 iteraciones")

    # ── Cobertura de las acciones nuevas ──
    print()
    print("[8] cobertura de acciones nuevas:")
    labels = {"playbasic": "PLAY_BASIC_POKEMON (MAIN)", "trainer": "PLAY_TRAINER",
              "evolve": "EVOLVE_POKEMON", "retreat": "RETREAT"}
    for k, lab in labels.items():
        if cover[k]:
            print(f"    [OK]   {lab}")
        elif attempted[k]:
            # se despacho con precondiciones validas y el backend la rechazo -> bug real
            print(f"    [FAIL] {lab} — intentada y rechazada por el backend")
            FAILS.append(f"accion intentada y rechazada: {lab}")
        else:
            print(f"    [skip] {lab} — sin oportunidad en esta partida (robo aleatorio)")

    print()
    if FAILS:
        print(f"SMOKE E2E: {len(FAILS)} problema(s):")
        for f in FAILS:
            print("  -", f)
        sys.exit(1)
    print("SMOKE E2E: TODO OK — partida completa jugada contra el backend real.")


if __name__ == "__main__":
    main()
