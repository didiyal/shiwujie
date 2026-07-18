# shiwujie-ai（视无界 AI 服务）

> **Phase 5 chunk-1：spikes-only。** 本目录当前只含两项前置 spike + 容纳它们的 uv 工程 + go/no-go 报告。**不建**生产 loop / FastAPI 服务骨架 / 纵切。生产实现在 chunk-2（待 go/no-go 结论）。

## 为什么

AI 重写（polyglot：Java 业务单体 + Python 自建 ReAct loop 智能体）有两个前置未知必须先实测、再决定 chunk-2 怎么走：

- **Spike-1（Decision A）**：qwen3.6-flash 原生 function-calling 在本工具集+本 prompt 下的可靠性。稳（干净集 ≥90%）→ 意图溶进 agent loop（杀现状 2-call 税）；不稳 → 加独立分类节点。
- **Spike-2（缝 C / Decision C）**：Spring AI 1.1.0 + alibaba 1.1.0.0（留 Boot 3.4.5）能否同 pom 共存 + MCP streamable HTTP server 能起来 + Python `langchain-mcp-adapters` 0.3.0 能连上且关停不崩（#466）。全绿 → 缝 C 首选 MCP；翻车 → Python 8 个 REST wrapper 降级。

> 版本目标根因见 `docs/known-issues.md` AI-1 与根 `docs/architecture/ai-rewrite.md`。

## 运行

```bash
cd shiwujie-ai
uv sync                                   # 建 .venv（uv 自动取 .python-version=3.12）+ 装依赖

# Spike-1（仅需 DashScope 可达，不依赖 Java/远程 infra）
export DASHSCOPE_API_KEY=sk-...           # 沿用 bootstrap application.yml 内联默认值
uv run python -m shiwujie_ai.spikes.fc

# Spike-2 Python 端（需先在 shiwujie-bootstrap 侧 spring-boot:run 起 MCP server /mcp）
uv run python -m shiwujie_ai.spikes.mcp_client
```

## 布局

```
shiwujie-ai/
├── pyproject.toml / .python-version      # uv 工程（spike-only 依赖）
├── src/shiwujie_ai/
│   ├── constants.py                      # 纯常量（路径 / 重复次数 / gate 阈值）
│   ├── config.py                         # 模型源配置（env 可覆盖：base_url / api_key / model）
│   ├── llm.py                            # LLM 唯一构造工厂（build_llm，注入给 runner）
│   ├── tools/{native,read_skill,java_mcp}.py  # 16 工具 @tool 装饰（6 native + 1 read_skill + 9 java_mcp）
│   └── spikes/fc/{__main__,runner,report,cases,scorer,stubs}.py  # Spike-1（runner 执行 / report 出报告）
├── data/fc_cases/{clean,adversarial}.yaml # ≥120 case（盲人口语化）
└── reports/                              # 中间产物（gitignored）：fc_go_nogo.md / spikes_go_nogo.md
```

> 生产 loop / FastAPI / 两层记忆 / KB / Dockerfile 在 chunk-2 加入。本目录 `docs/`（design/known-issues/fallback/deployment/README）已在 Phase 5a 落档，spike 结论反写其中。
