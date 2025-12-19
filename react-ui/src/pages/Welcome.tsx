import { PageContainer } from '@ant-design/pro-components';
import { Card, theme, Statistic, Row, Col, message } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined, DollarOutlined, FundOutlined } from '@ant-design/icons';
import React, { useState, useEffect } from 'react';
import { getHoldingsList } from '@/services/crypto/api';

// 系统概览数据类型
interface OverviewData {
  totalAssets: number;
  totalHoldings: number;
  totalValueChange: number;
  usdtBalance: number;
}

const Welcome: React.FC = () => {
  const { token } = theme.useToken();
  const [overviewData, setOverviewData] = useState<OverviewData>({
    totalAssets: 0,
    totalHoldings: 0,
    totalValueChange: 0,
    usdtBalance: 0,
  });

  // 从API获取持仓数据计算概览信息
  useEffect(() => {
    const fetchOverviewData = async () => {
      try {
        const response = await getHoldingsList();
        // 处理API响应，根据实际返回结构调整
        const holdings: API.BizAssetHoldings[] = Array.isArray(response) 
          ? response 
          : Array.isArray((response as any).rows) 
            ? (response as any).rows 
            : [];
        
        // 计算总资产
        const totalAssets = holdings.reduce((sum: number, item: API.BizAssetHoldings) => sum + (item.usdtValue || 0), 0);
        
        // 计算持币种类
        const totalHoldings = holdings.length;
        
        // 这里可以从其他API获取USDT余额和价值变动
        // 暂时使用模拟数据，实际项目中应替换为真实API调用
        const usdtBalance = 523456.78;
        const totalValueChange = 2.5;
        
        setOverviewData({
          totalAssets,
          totalHoldings,
          totalValueChange,
          usdtBalance,
        });
      } catch (error) {
        message.error('获取概览数据失败');
        console.error('获取概览数据失败:', error);
      }
    };

    fetchOverviewData();
  }, []);

  return (
    <PageContainer title="系统概览">
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="总资产(USDT)"
              value={overviewData.totalAssets}
              precision={2}
              prefix={<DollarOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="持币种类"
              value={overviewData.totalHoldings}
              prefix={<FundOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="总价值变动(24h)"
              value={overviewData.totalValueChange}
              precision={2}
              suffix="%"
              prefix={overviewData.totalValueChange > 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
              valueStyle={{ color: overviewData.totalValueChange > 0 ? '#cf1322' : '#3f8600' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="USDT余额"
              value={overviewData.usdtBalance}
              precision={2}
              prefix={<DollarOutlined />}
            />
          </Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="系统功能" bordered={false}>
            <Row gutter={[16, 16]}>
              <Col xs={12}>
                <Card type="inner" title="持仓管理" size="small">
                  <p>查看和管理企业持仓资产</p>
                </Card>
              </Col>
              <Col xs={12}>
                <Card type="inner" title="市场消息" size="small">
                  <p>获取最新的数字货币市场消息</p>
                </Card>
              </Col>
              <Col xs={12}>
                <Card type="inner" title="投资建议" size="small">
                  <p>查看AI生成的投资建议报告</p>
                </Card>
              </Col>
              <Col xs={12}>
                <Card type="inner" title="审核操作" size="small">
                  <p>审批和执行交易操作</p>
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="系统提示" bordered={false}>
            <div style={{ lineHeight: '24px', color: token.colorTextSecondary }}>
              <p>1. 请定期查看持仓数据，关注市场变化</p>
              <p>2. 所有交易操作需要经过审核才能执行</p>
              <p>3. 系统会定期生成投资建议报告</p>
              <p>4. 请确保USDT余额充足以执行交易</p>
            </div>
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
};

export default Welcome;
