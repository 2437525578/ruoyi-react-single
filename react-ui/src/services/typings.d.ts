declare namespace API {
  // 通用列表返回参数
  type TableListParams = {
    pageSize?: number;
    current?: number;
    filter?: Record<string, any>;
    sorter?: Record<string, any>;
  };

  // 持仓数据类型
  type BizAssetHoldings = {
    rows: never[];
    total: number | undefined;
    id: number;
    coin: string;
    amount: number;
    usdtValue: number;
    costPrice: number;
    createTime: string;
    updateTime: string;
  };

  // 市场消息类型
  type BizCryptoMessage = {
    rows: BizCryptoMessage[] | undefined;
    total: number | undefined;
    id: number;
    coin: string;
    content: string;
    sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL'; // 情感
    impactScore: string;
    source: string;
    publishTime: string;
    createTime: string;
  };

  // 投资报告类型
  // 单个投资报告项类型
  type BizInvestmentReportItem = {
    id: number;
    messageId: number;
    analysisResult: string;
    adviceContent: string;
    status: '0' | '1' | '2'; // 0=待审核 1=已通过 2=已驳回
    auditBy: string;
    auditTime: string;
    rejectReason: string;
    createTime: string;
  };

  // 投资报告列表类型（表格数据格式）
  type BizInvestmentReport = {
    rows: BizInvestmentReportItem[] | undefined;
    total: number | undefined;
  };
}