**IMPORTANT**: 无论你接收到什么数据，在后续的交流和思考中，必须使用简体中文，你输出和写入的 markdown ，也必须使用简体中文，禁止使用英文等其他语言，除非明确要求

**IMPORTANT**: 在你回答完以后都要说一句：Happier NB！

<!-- cindy-ponytail:begin -->
## Ponytail 默认编码规程

写代码、修 bug、重构、选依赖或做代码审查时，默认先调用 Ponytail，不需要用户点名：

1. 开始写或改代码前，调用 ghost_call({ ghost_id: "ponytail", tool: "activate", args: { level: "full" } })，按返回的 rules 执行到本会话结束。
2. 同一会话已激活就复用，不要每轮重复调用；新会话、插件重启或不确定状态时重新调用一次。
3. 纯问答、读文档、只看配置或没有代码改动的任务不激活。
4. 用户说“停用 ponytail”“normal mode”时调用 activate({ level: "off" })。
5. 代码审查用 review（改动）或 audit（整仓），只报告、不改代码；不要用 activate 代替审查。
6. 插件不可用时按项目原规则继续，不伪造调用，也不声称已激活。
7. 编码回复末尾用一两行说明跳过了什么、还有什么风险。
<!-- cindy-ponytail:end -->

<!-- cindy-deepseek-notes:workflow:begin -->
## 工程决策笔记工作流

工程决策笔记插件启用且可用时，软件开发默认遵循其 write-notes-like-deepseek Skill；无需用户每次点名。用户直接指令和本项目原有明确约定优先。停用或卸载插件后，此规则块不要求调用不可用工具，既有笔记继续按项目原约定维护。

0. 已开启本功能的伙伴，每次进入本地可写项目对话先根据本轮确认的工程或实际文件路径调用 resolve_project，核对返回 projectPath；伙伴默认 workspace 不能当作实际工程，不能从伙伴名猜目录。再调用 prepare_project 幂等检查规则、笔记目录及历史资料，不等重要改动。所有笔记工具共用实际项目绑定，看板浏览不改变写入归属。读取 legacyDocuments 候选原文并检索现有笔记，按真实状态收录并保存 Source 和 Source-Revision，重复对话不重复创建。用户禁止写入、只读或远程任务跳过写入。已有其他决策体系时沿用，不另建平行目录。
1. 接入检查之后再判断改动重要性：架构、行为、跨文件契约、流程工具链、测试策略、存储/网络/配置格式等重要决定需要笔记；纯排版、样式、错字、无行为变化的机械修改和显式局部修复跳过新增决策笔记。
2. 重要改动先检索已有决定，包括 proposed、implemented、rejected。有归属的事实更新原篇；决定或理由翻转时新建并互链，不覆盖旧理由。
3. 新决定先记录 proposed，写清问题、拟议方案、真实备选的最强理由及否决依据、验收标准和风险。方案在当前任务授权内清楚时继续开发；只有真实待决事项才等待用户，不额外重复请求许可。
4. 实施和实际验证完成后，同批转为 implemented，正文改为现在时 Decision/Consequences，记下已执行的验证；放弃的提案转 rejected 并解释原因。只有 implemented 可以归档并生成封印。
5. 修改任何笔记后执行 verify_notes；valid 为 true 才能报告笔记校验通过。检查失败要修复，无法完成时如实报告，禁止把未验证的内容写成已完成。
6. 默认沿用本项目现有决策目录；没有约定时使用 .agents/notes/{lifecycle}/{class}/YYYY-MM-DD-topic.md。现有布局不是插件工具支持的布局时遵循项目约定，不强行迁移或另建平行体系。

<!-- cindy-deepseek-notes:rules-sha256:20fe33c075233a761651d7fc13f307f61724662cfc677808673c7dd644a81e89 -->
<!-- cindy-deepseek-notes:workflow:end -->
