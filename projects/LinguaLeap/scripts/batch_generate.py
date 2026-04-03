#!/usr/bin/env python3
"""
LinguaLeap 批量预制内容生成脚本
Phase 1: 为全部45个单元生成知识点 (KP)
Phase 2: 为选定单元生成AI题目

使用方法:
  python3 batch_generate.py kps          # 生成全部45单元KP
  python3 batch_generate.py kps 1-5      # 只生成unit_id 1~5
  python3 batch_generate.py questions     # 为L1(1,2) L5(21) L8(36)生成题目
"""

import json
import sys
import time
import subprocess
import urllib.request
import urllib.error

# 绕过系统代理，直连localhost
_no_proxy_handler = urllib.request.ProxyHandler({})
_opener = urllib.request.build_opener(_no_proxy_handler)

AI_SERVICE = "http://localhost:8083"
CONTENT_SERVICE = "http://localhost:8082"
DB_URL = "postgresql://lingualeap:lingualeap123@localhost/ll_content"
RATE_LIMIT_DELAY = 15  # seconds between AI calls

# KP数量: L1-L4=25, L5-L7=40, L8-L9=50
KP_COUNT = {
    "L1": 25, "L2": 25, "L3": 25, "L4": 25,
    "L5": 40, "L6": 40, "L7": 40,
    "L8": 50, "L9": 50,
}

# 题目生成的目标单元及其年级
QUESTION_UNITS = {
    1: "小学",    # L1 Unit 1
    2: "小学",    # L1 Unit 2
    21: "初中",   # L5 Unit 1
    36: "高中",   # L8 Unit 1
}

QUESTION_TYPES = ["en2zh_choice", "zh2en_choice", "fill_blank", "translate"]
QUESTIONS_PER_TYPE = 5  # 每种题型生成5题


def psql(query):
    """执行psql查询，返回JSON结果"""
    cmd = [
        "psql", DB_URL, "-t", "-A", "-c",
        f"SELECT json_agg(t) FROM ({query}) t"
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  [ERROR] psql: {result.stderr.strip()}")
        return []
    raw = result.stdout.strip()
    if not raw or raw == "" or raw == "null":
        return []
    return json.loads(raw)


def api_post(url, data, headers=None):
    """发送POST请求"""
    body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    if headers:
        for k, v in headers.items():
            req.add_header(k, v)
    try:
        with _opener.open(req, timeout=120) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8") if e.fp else ""
        print(f"  [ERROR] HTTP {e.code}: {error_body[:200]}")
        return None
    except Exception as e:
        print(f"  [ERROR] {e}")
        return None


def get_units(unit_range=None):
    """从数据库获取单元列表"""
    where = ""
    if unit_range:
        start, end = unit_range
        where = f"WHERE u.id BETWEEN {int(start)} AND {int(end)}"

    rows = psql(f"""
        SELECT u.id, u.level_id, l.code, l.name as level_name,
               l.description as level_desc, l.grade_group,
               u.name as unit_name, u.topic, u.kp_count
        FROM knowledge_unit u JOIN knowledge_level l ON u.level_id = l.id
        {where}
        ORDER BY u.id
    """)
    return rows or []


def generate_kps(unit_range=None):
    """Phase 1: 批量生成KP"""
    units = get_units(unit_range)
    if not units:
        print("没有找到单元数据")
        return

    total = len(units)
    success = 0
    skipped = 0
    failed = 0

    print(f"\n{'='*60}")
    print(f"  Phase 1: 生成知识点 (KP) - 共 {total} 个单元")
    print(f"{'='*60}\n")

    for i, unit in enumerate(units):
        uid = unit["id"]
        code = unit["code"]
        count = KP_COUNT.get(code, 25)

        print(f"[{i+1}/{total}] Unit {uid}: {unit['unit_name']} ({code} {unit['level_name']})")

        # 跳过已有KP的单元
        if unit["kp_count"] and unit["kp_count"] > 0:
            print(f"  已有 {unit['kp_count']} 个KP，跳过")
            skipped += 1
            continue

        # Step 1: 调用AI生成KP
        print(f"  生成 {count} 个KP...")
        resp = api_post(f"{AI_SERVICE}/api/ai/generate/unit-content", {
            "levelCode": code,
            "levelName": unit["level_name"],
            "levelDesc": unit["level_desc"] or "",
            "topic": unit["topic"] or unit["unit_name"],
            "unitName": unit["unit_name"],
            "count": count,
        })

        if not resp or resp.get("code") != 200 or not resp.get("data"):
            print(f"  [FAIL] AI生成失败")
            failed += 1
            time.sleep(RATE_LIMIT_DELAY)
            continue

        kp_list = resp["data"]
        print(f"  AI返回 {len(kp_list)} 个KP")

        # Step 2: 导入KP到content服务
        import_resp = api_post(
            f"{CONTENT_SERVICE}/api/content/levels/units/{uid}/import-kps",
            kp_list
        )

        if import_resp and import_resp.get("code") == 200:
            imported = import_resp["data"].get("imported", 0)
            print(f"  ✓ 导入成功: {imported} 个KP")
            success += 1
        else:
            print(f"  [FAIL] 导入失败")
            failed += 1

        # 频率限制
        if i < total - 1:
            print(f"  等待 {RATE_LIMIT_DELAY}s...")
            time.sleep(RATE_LIMIT_DELAY)

    print(f"\n{'='*60}")
    print(f"  KP生成完成: 成功 {success}, 跳过 {skipped}, 失败 {failed}")
    print(f"{'='*60}\n")


def generate_questions():
    """Phase 2: 为选定单元生成AI题目"""
    print(f"\n{'='*60}")
    print(f"  Phase 2: 为选定单元生成AI题目")
    print(f"{'='*60}\n")

    total_success = 0
    total_fail = 0

    for unit_id, grade in QUESTION_UNITS.items():
        # 获取该单元的KP列表
        kps = psql(f"""
            SELECT id, content, type, meaning_zh
            FROM knowledge_point
            WHERE unit_id = {int(unit_id)}
            ORDER BY id
        """)

        if not kps:
            print(f"Unit {unit_id}: 没有KP，跳过")
            continue

        unit_info = psql(f"""
            SELECT u.name, l.code, l.name as level_name
            FROM knowledge_unit u JOIN knowledge_level l ON u.level_id = l.id
            WHERE u.id = {int(unit_id)}
        """)
        unit_name = unit_info[0]["name"] if unit_info else f"Unit {unit_id}"
        level_name = unit_info[0]["level_name"] if unit_info else ""

        print(f"\n--- Unit {unit_id}: {unit_name} ({level_name}) ---")
        print(f"  KP数量: {len(kps)}, 年级: {grade}")

        # 为每种题型选取KP并生成题目
        generated = 0
        failed = 0

        # 查询该单元已有的题目（按kp_id+type），避免重复
        existing = psql(f"""
            SELECT q.kp_id, q.type FROM question q
            JOIN knowledge_point kp ON q.kp_id = kp.id
            WHERE kp.unit_id = {int(unit_id)}
        """)
        existing_set = set()
        if existing:
            existing_set = {(e["kp_id"], e["type"]) for e in existing}

        for type_idx, qtype in enumerate(QUESTION_TYPES):
            # 每种题型选 QUESTIONS_PER_TYPE 个KP
            start_idx = type_idx * QUESTIONS_PER_TYPE
            selected_kps = kps[start_idx:start_idx + QUESTIONS_PER_TYPE]
            if not selected_kps:
                # 循环使用KP
                selected_kps = kps[:QUESTIONS_PER_TYPE]

            for kp in selected_kps:
                kp_id = kp["id"]
                if (kp_id, qtype) in existing_set:
                    print(f"  [{qtype}] kp={kp_id} ({kp['content']})... 已存在，跳过")
                    generated += 1
                    continue
                print(f"  [{qtype}] kp={kp_id} ({kp['content']})...", end=" ", flush=True)

                resp = api_post(
                    f"{AI_SERVICE}/api/ai/generate/question",
                    {"kpId": kp_id, "questionType": qtype, "grade": grade},
                    headers={"X-User-Id": "1"}
                )

                if resp and resp.get("code") == 200:
                    print("✓")
                    generated += 1
                else:
                    print("✗")
                    failed += 1

                time.sleep(RATE_LIMIT_DELAY)

        total_success += generated
        total_fail += failed
        print(f"  本单元: 成功 {generated}, 失败 {failed}")

    print(f"\n{'='*60}")
    print(f"  题目生成完成: 成功 {total_success}, 失败 {total_fail}")
    print(f"{'='*60}\n")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return

    cmd = sys.argv[1]

    if cmd == "kps":
        unit_range = None
        if len(sys.argv) >= 3:
            parts = sys.argv[2].split("-")
            unit_range = (int(parts[0]), int(parts[1]))
        generate_kps(unit_range)

    elif cmd == "questions":
        generate_questions()

    else:
        print(f"未知命令: {cmd}")
        print(__doc__)


if __name__ == "__main__":
    main()
