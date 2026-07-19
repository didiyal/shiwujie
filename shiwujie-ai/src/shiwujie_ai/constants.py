"""Spike 共享纯常量（不变的值）——不含模型配置（模型配置见 config.py）。

职责分层：常量归此、模型来源归 config、LLM 构造归 llm.py、runner 只用不造。
"""

from pathlib import Path

# FC 非确定性：每条 case 重复次数（取众数；<3 则报中位数+方差无意义）。
REPS = 3

# 「一致通过」所需多数票阈值（3 次里 ≥2）。
# 注：runner 用严格多数 `2*passed > total` 判定（rep=1 单过即过；rep=3 需 ≥2），
# 本常量作文档/阈值说明用。
MAJORITY = 2

# gate：干净集 pass-rate ≥ 此值 → Decision A 成立。
GATE_CLEAN_PASS_RATE = 0.90

# 对抗集降级触发线（< 此值即使 A 成立也建议 B 兜底；仅报告不阻塞）。
ADVERSARIAL_DOWNGRADE_LINE = 0.50

# Python 工程根（src/shiwujie_ai/constants.py -> ../..  = shiwujie-ai/）。
PKG_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = PKG_DIR.parents[1]

CASES_DIR = PROJECT_ROOT / "data" / "fc_cases"
REPORTS_DIR = PROJECT_ROOT / "reports"

# Java MCP server 端点统一在 config.JAVA_MCP_URL（env 覆盖 + 内联默认）——勿在此重复定义。

