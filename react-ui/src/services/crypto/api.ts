import { request } from '@umijs/max';

// ================= 持仓管理 =================

// 查询列表
export async function getHoldingsList(params?: any) {
  return request<API.BizAssetHoldings>('/api/crypto/holdings/list', {
    method: 'GET',
    params,
  });
}

// 新增
export async function addHoldings(data: any) {
  return request('/api/crypto/holdings', {
    method: 'POST',
    data,
  });
}

// 修改
export async function updateHoldings(data: any) {
  return request('/api/crypto/holdings', {
    method: 'PUT',
    data,
  });
}

// 删除
export async function removeHoldings(ids: string) {
  return request(`/api/crypto/holdings/${ids}`, {
    method: 'DELETE',
  });
}

// ================= 消息采集 =================

export async function getMessageList(params?: any) {
  return request<API.BizCryptoMessage>('/api/crypto/message/list', {
    method: 'GET',
    params,
  });
}

export async function addMessage(data: any) {
  return request('/api/crypto/message', {
    method: 'POST',
    data,
  });
}

export async function updateMessage(data: any) {
  return request('/api/crypto/message', {
    method: 'PUT',
    data,
  });
}

export async function removeMessage(ids: string) {
  return request(`/api/crypto/message/${ids}`, {
    method: 'DELETE',
  });
}

// 手动触发AI收集新闻
export async function triggerCollectNews() {
  return request('/api/crypto/message/collect', {
    method: 'POST',
  });
}

// 手动生成投资报告
export async function generateReport(data: any) {
  return request('/api/crypto/report/generate', {
    method: 'POST',
    data,
  });
}

// ================= 投资报告 =================

export async function getReportList(params?: any) {
  return request<API.BizInvestmentReport>('/api/crypto/report/list', {
    method: 'GET',
    params,
  });
}

export async function updateReport(data: any) {
  return request('/api/crypto/report', {
    method: 'PUT',
    data,
  });
}

export async function removeReport(ids: string) {
  return request(`/api/crypto/report/${ids}`, {
    method: 'DELETE',
  });
}

// ================= 仪表盘数据 =================

export async function getDashboardData() {
  return request('/api/crypto/dashboard', {
    method: 'GET',
  });
}


// ================= 比特币链上数据 =================

/** 获取行情列表 */
export async function getMetricsList(params: any) {
  return request('/api/crypto/metrics/list', {
    method: 'GET',
    params,
  });
}

/** 手动触发行情采集 */
export async function triggerCollectMetrics() {
  return request('/api/crypto/metrics/collect', {
    method: 'POST',
  });
}