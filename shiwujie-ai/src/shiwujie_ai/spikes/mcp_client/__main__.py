"""Spike-2 Python 入口：python -m shiwujie_ai.spikes.mcp_client"""

import asyncio
import sys

from .connect import run


def main() -> None:
    # run() 体内捕连接/取工具错；asyncio.run 外层捕 loop 关停时的 teardown 噪声（#466）。
    teardown_error = None
    try:
        report = asyncio.run(run())
    except Exception as e:  # noqa: BLE001 — loop 关停 teardown 噪声（#466），非阻塞
        teardown_error = f"{type(e).__name__}: {e}"
        report = {"tool_count": None, "roundtrip": None, "phase_error": None}
        print(f"[mcp][#466?] teardown raised at loop close: {teardown_error}")

    rt = report.get("roundtrip") or {}
    print(
        f"[mcp] done: tools={report.get('tool_count')} "
        f"roundtrip={'ok:' + rt.get('tool', '') if 'tool' in rt else 'skip'} "
        f"teardown={'#466:' + teardown_error if teardown_error else 'clean'} "
        f"phase_error={report.get('phase_error') or 'none'}"
    )
    # 仅连接/取工具失败计非 0 退出；#466 teardown 噪声不阻塞逐 turn 用法，不计失败。
    if report.get("phase_error"):
        sys.exit(1)


if __name__ == "__main__":
    main()
