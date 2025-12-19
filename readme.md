12.2.0 版本--秦家隆修改文档
配置调整：
在 RuoYiApplication.java 中添加了 @EnableScheduling 注解，开启了定时任务支持。

修改 application.yml，将日志级别从 debug 调整为 info 以减少控制台噪音。

在 application.yml 中新增了 dify.api 配置块（Key 和 URL）。

1.2 业务模块实现 (src/org.example.org.example、src/org/example/system/mapper、src/org/example/system/service)
我们实现了三个核心业务模块：

持仓管理 (Holdings)

Domain: BizAssetHoldings - 记录币种、数量、估值、成本等。

Controller/Service/Mapper: 实现了标准的 CRUD 接口，供前端管理持仓数据，并提供给 AI 作为分析上下文。

市场消息 (Message)

Domain: BizCryptoMessage - 记录消息内容、情感倾向（利好/利空）、来源等。

核心逻辑 (BizCryptoMessageServiceImpl):

实现了 autoCollectNews() 定时任务（每天早8点）。

集成 DifyUtils 调用 AI 搜集新闻。

业务闭环：在新闻入库后，自动调用 reportService.generateReport() 触发分析。

投资建议 (Report)

Domain: BizInvestmentReport - 记录 AI 分析结果、建议内容、审核状态（待审核/已通过/已驳回）。

核心逻辑 (BizInvestmentReportServiceImpl):

实现了 generateReport(messageId)：查询当前持仓 -> 结合新闻构建 Prompt -> 调用 AI 生成建议 -> 存入数据库。

实现了 updateBizInvestmentReport：在审核状态更为“已通过”时，触发模拟的自动调仓逻辑 (executeAutoTrade)。

1.3 工具类封装(src/org/example/system/utils)
DifyUtils: 封装了向 Dify 平台发送 HTTP 请求的通用方法，处理 JSON 响应。

二、 前端开发 (React)
由于代码生成器生成的是 Vue 代码，我们手动重写了对应的 React 页面。

2.1 路由配置 (config/routes.ts)
新增了 /crypto 路由组，包含三个子页面：

/crypto/holdings：持仓管理

/crypto/message：市场消息

/crypto/report：投资建议

2.2 API 服务层 (services/crypto)
typings.d.ts: 定义了 TypeScript 接口类型，确保前后端数据结构一致。

api.ts: 封装了对应后端的 RESTful 请求（GET list, POST add, PUT update, DELETE remove）。

2.3 页面组件 (pages/Crypto)
Holdings/index.tsx (持仓管理)

使用 ProTable 实现持仓列表。

支持“可编辑行”模式，允许直接在表格中修改持仓数量。

Message/index.tsx (市场消息)

展示 AI 采集的新闻。

使用 Tag 组件对情感倾向（利好/利空）进行颜色区分展示。

Report/index.tsx (投资建议审核)

展示待审核的 AI 建议报告。

审核交互：实现了“通过”和“驳回”按钮。点击“驳回”会弹出 Modal 框输入驳回原因。

三、 数据库变更
3.1 业务表结构
在 MySQL 中新建了三张表（包含 create_by, create_time 等标准字段）：

biz_asset_holdings：存储持仓资产。

biz_crypto_message：存储采集到的市场消息。

biz_investment_report：存储分析报告及审核记录。

3.2 菜单数据 (sys_menu)和货币数字系统页面 (biz_asset_holdings、biz_crypto_message、biz_investment_report存放于DOM下)
通过 SQL 脚本直接插入了菜单数据，解决了页面添加可能失败的问题：

一级目录：数字货币系统 (ID: 2000)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2000, '数字货币系统', 0, 1, 'crypto', 'Layout', 1, 0, 'M', '0', '0', '', 'fund', 'admin', NOW(), '', NULL, '数字货币一级目录');
子菜单：持仓管理 (2001)、市场消息 (2002)、投资建议 (2003)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2001, '持仓管理', 2000, 1, 'holdings', 'Crypto/Holdings/index', 1, 0, 'C', '0', '0', 'crypto:holdings:list', 'chart', 'admin', NOW(), '', NULL, '持仓管理页面');

-- 3. 插入子菜单：市场消息
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2002, '市场消息', 2000, 2, 'message', 'Crypto/Message/index', 1, 0, 'C', '0', '0', 'crypto:message:list', 'message', 'admin', NOW(), '', NULL, '市场消息页面');

-- 4. 插入子菜单：投资建议
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2003, '投资建议', 2000, 3, 'report', 'Crypto/Report/index', 1, 0, 'C', '0', '0', 'crypto:report:list', 'read', 'admin', NOW(), '', NULL, '投资建议审核页面');
四、 核心业务流程总结
经过以上修改，系统已具备完整的自动化闭环：

触发：Spring Scheduler 定时触发。

采集：后端调用 Dify AI 联网搜索新闻，解析 JSON 并存入 biz_crypto_message。

分析：新闻入库后，自动查询 biz_asset_holdings，将“新闻+持仓”发送给 AI 进行二次分析。

生成：AI 返回具体的买卖建议，存入 biz_investment_report（状态：待审核）。

审核：管理员在 React 前端看到报告，点击“通过”。

执行：后端检测到审核通过，自动执行持仓更新逻辑（模拟交易）。
2025/12/19李杨阳-覃慧敏
新增了一个数据库用于储存获取到的虚拟货币行情
sql dll如下
CREATE TABLE `biz_crypto_metrics` (
  `id` int NOT NULL AUTO_INCREMENT,
  `symbol` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '币种符号，如 BTC, ETH, USDT（建议填充）',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '币种中文名称，如 比特币',
  `price_usd` decimal(20,8) NOT NULL COMMENT '当前价格（USD），支持高精度小数',
  `market_cap` decimal(20,4) DEFAULT NULL COMMENT '市值（单位：亿美元），如比特币 17420 表示 1.742 万亿美元',
  `hash_rate` decimal(10,2) DEFAULT NULL COMMENT '哈希率变化百分比（%），稳定币可能接近0',
  `24h_change` decimal(10,2) DEFAULT NULL COMMENT '24小时涨跌幅（%）',
  `transaction_count` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流通量或交易量描述，如 "1944万 BTC"、"1863亿 USDT"',
  `total_fees_btc` decimal(20,8) DEFAULT NULL COMMENT '总手续费（可能以BTC或对应币种计）',
  `block_count` decimal(20,4) DEFAULT NULL COMMENT '区块相关指标（不同币种单位不同）',
  `ath_price` decimal(20,8) DEFAULT NULL COMMENT '历史最高价（USD）',
  `snapshot_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '数据爬取时间快照',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `create_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT 'dify_crawler' COMMENT '创建者，默认爬虫名称',
  `update_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT 'dify_crawler' COMMENT '最后更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symbol_snapshot` (`symbol`,`snapshot_time`),
  KEY `idx_name` (`name`),
  KEY `idx_snapshot_time` (`snapshot_time`),
  KEY `idx_symbol` (`symbol`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟货币行情指标表（Dify爬取数据专用）';
在原有sys_menu表中新增了一列
2004	虚拟货币行情	2000	4	metrics	Crypto/Metrics/index		1	0	C	0	0	crypto:metrics:list	metrics	admin	2025-12-14 20:05:21			虚拟货币行情页面
修复了ai采集市场市场消息的部分bug（影响分数不显示）
新增了一个虚拟货币行情功能模块实现后端链接dify获取虚拟货币行情
如果遇到前端一直加载不进入登录页面 可直接在浏览器输入http://localhost:8000/user/login
