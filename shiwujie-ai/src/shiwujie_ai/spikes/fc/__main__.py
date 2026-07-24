"""Spike-1 入口：python -m shiwujie_ai.spikes.fc [smoke|full]。

composition root：在此组装（构造 build_llm）并注入 runner，使 runner 与 langchain 解耦。
"""

import sys

from ... import constants
from ...llm import build_llm
from .report import aggregate, emit, serialize
from .runner import run


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else "full"
    if mode not in ("smoke", "full"):
        print(f"unknown mode: {mode} (smoke|full)", file=sys.stderr)
        sys.exit(2)
    results = run(mode, build_llm)  # 注入 LLM 工厂 → 执行结果 list[CaseResult]
    raw = serialize(results, mode)  # → 机读 dict
    agg = aggregate(raw)            # → 按 (model,arm,set) 聚合
    emit(raw, agg, constants.REPORTS_DIR)


if __name__ == "__main__":
    main()
