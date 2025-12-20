import { Button, message, Space, Avatar, Typography, Row, Col, Card, Statistic, Spin, Empty, List, Tooltip as AntdTooltip, Dropdown } from 'antd';
import { PageContainer } from '@ant-design/pro-components';
import { ReloadOutlined, StockOutlined, RiseOutlined, FallOutlined, LineChartOutlined, BarChartOutlined, PieChartOutlined, DashboardOutlined, ColumnHeightOutlined } from '@ant-design/icons';
import React, { useRef, useState, useEffect } from 'react';
import { getMetricsList, triggerCollectMetrics } from '@/services/crypto/api';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, ResponsiveContainer, AreaChart, Area, BarChart, Bar, Cell, PieChart, Pie, Tooltip as RechartsTooltip } from 'recharts';

const { Text, Title } = Typography;

const colors = ['#1890ff', '#52c41a', '#faad14', '#ff4d4f', '#722ed1', '#13c2c2', '#eb2f96'];

const CustomTooltip = ({ active, payload, label, type }: any) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    const formatPrice = (price: any) => {
      const p = Number(price || 0);
      if (p >= 100) return p.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
      if (p >= 1) return p.toLocaleString(undefined, { minimumFractionDigits: 4, maximumFractionDigits: 4 });
      return p.toFixed(8);
    };

    return (
      <div style={{ 
        backgroundColor: '#fff', 
        padding: '12px', 
        border: 'none', 
        borderRadius: '8px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
        borderLeft: `4px solid ${type === 'area' ? '#1890ff' : payload[0].color || '#1890ff'}`
      }}>
        <div style={{ fontWeight: 'bold', marginBottom: '8px', fontSize: '14px', borderBottom: '1px solid #f0f0f0', paddingBottom: '4px' }}>
          {data.name} ({data.symbol})
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: '20px' }}>
            <span style={{ color: '#8c8c8c' }}>当前价格:</span>
            <span style={{ fontWeight: 'bold', color: '#262626' }}>${formatPrice(data.priceUsd)}</span>
          </div>
          {type === 'area' ? (
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '20px' }}>
              <span style={{ color: '#8c8c8c' }}>24h 涨跌幅:</span>
              <span style={{ fontWeight: 'bold', color: Number(data.change24h) >= 0 ? '#52c41a' : '#ff4d4f' }}>
                {Number(data.change24h) >= 0 ? '+' : ''}{data.change24h}%
              </span>
            </div>
          ) : (
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '20px' }}>
              <span style={{ color: '#8c8c8c' }}>总市值:</span>
              <span style={{ fontWeight: 'bold', color: '#1890ff' }}>${Number(data.marketCap).toLocaleString()}B</span>
            </div>
          )}
        </div>
      </div>
    );
  }
  return null;
};

const MetricsDashboard: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [chartData, setChartData] = useState<any[]>([]);
  const [marketStats, setMarketStats] = useState({ totalCap: 0, btcDominance: 0, activeCoins: 0 });
  const [topGainers, setTopGainers] = useState<any[]>([]);
  const [topLosers, setTopLosers] = useState<any[]>([]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getMetricsList({ pageSize: 100 });
      const data = res.rows || [];
      
      // 准备图表数据
      const processedChartData = [...data]
        .filter(item => item.change24h !== null)
        .sort((a, b) => (Number(b.marketCap) || 0) - (Number(a.marketCap) || 0));
      setChartData(processedChartData);

      // 计算市场指标
      const totalCap = data.reduce((sum, item) => sum + (Number(item.marketCap) || 0), 0);
      const btcMetric = data.find(item => item.symbol === 'BTC');
      const btcCap = Number(btcMetric?.marketCap) || 0;
      
      setMarketStats({
        totalCap,
        btcDominance: totalCap > 0 ? (btcCap / totalCap) * 100 : 0,
        activeCoins: data.length
      });

      // 涨跌榜
      const sortedByChange = [...data].sort((a, b) => Number(b.change24h) - Number(a.change24h));
      setTopGainers(sortedByChange.slice(0, 5));
      setTopLosers([...sortedByChange].reverse().slice(0, 5));

    } catch (error) {
      message.error('获取数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCollect = async () => {
    try {
      setLoading(true);
      await triggerCollectMetrics();
      message.success('已成功触发采集虚拟货币数据，系统正在从各大交易所同步数据，请稍候...');
      // 模拟或者等待一段时间让数据同步
      await new Promise(resolve => setTimeout(resolve, 3000));
      await fetchData();
    } catch (error) {
      message.error('触发采集失败');
    } finally {
      setLoading(false);
    }
  };

  if (loading && chartData.length === 0) {
    return (
      <PageContainer>
        <div style={{ textAlign: 'center', padding: '100px' }}>
          <Spin size="large" tip="正在加载市场大数据..." />
        </div>
      </PageContainer>
    );
  }

  return (
    <PageContainer 
      title="加密货币市场大数据看板"
      extra={[
        <Button
          type="primary"
          key="collect"
          icon={<ReloadOutlined />}
          loading={loading}
          onClick={handleCollect}
        >
          手动更新全网数据
        </Button>
      ]}
    >
      <Spin spinning={loading} tip="正在获取全网加密货币市场大数据，可能需要 1-2 分钟，请稍候..." size="large">
        <Row gutter={[16, 16]}>
          {/* 第一行：核心指标 */}
          <Col xs={24} sm={8} md={6}>
            <Card bordered={false} bodyStyle={{ padding: '20px' }}>
              <Statistic 
                title="全球加密总市值" 
                value={marketStats.totalCap} 
                precision={2}
                suffix="B"
                prefix="$"
                valueStyle={{ color: '#1890ff', fontWeight: 'bold' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8} md={6}>
            <Card bordered={false} bodyStyle={{ padding: '20px' }}>
              <Statistic 
                title="BTC 市占率" 
                value={marketStats.btcDominance} 
                precision={2}
                suffix="%"
                valueStyle={{ color: '#722ed1', fontWeight: 'bold' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8} md={6}>
            <Card bordered={false} bodyStyle={{ padding: '20px' }}>
              <Statistic 
                title="监控中币种数量" 
                value={marketStats.activeCoins} 
                valueStyle={{ color: '#52c41a', fontWeight: 'bold' }}
                prefix={<DashboardOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8} md={6}>
            <Card bordered={false} bodyStyle={{ padding: '20px' }}>
              <Statistic 
                title="24h 上涨币种" 
                value={chartData.filter(i => Number(i.change24h) > 0).length} 
                valueStyle={{ color: '#52c41a', fontWeight: 'bold' }}
                suffix={`/ ${chartData.length}`}
              />
            </Card>
          </Col>

        {/* 第二行：趋势图 */}
        <Col span={24}>
          <Card 
            title={
              <Space>
                <LineChartOutlined />
                <span>24h 价格涨跌幅分布 (Price Change Distribution)</span>
              </Space>
            }
            bordered={false}
          >
            <div style={{ height: 300, width: '100%' }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData.slice(0, 15)}>
                  <defs>
                    <linearGradient id="colorChange" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#1890ff" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#1890ff" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                  <XAxis 
                    dataKey="symbol" 
                    axisLine={false}
                    tickLine={false}
                    tick={{ fill: '#8c8c8c', fontSize: 12 }}
                  />
                  <YAxis 
                    axisLine={false}
                    tickLine={false}
                    tick={{ fill: '#8c8c8c', fontSize: 12 }}
                    unit="%"
                  />
                  <RechartsTooltip content={<CustomTooltip type="area" />} />
                  <Area 
                    type="monotone" 
                    dataKey="change24h" 
                    stroke="#1890ff" 
                    strokeWidth={3}
                    fillOpacity={1} 
                    fill="url(#colorChange)" 
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>

        {/* 第三行：市值对比 & 涨跌榜 */}
        <Col xs={24} lg={12}>
          <Card 
            title={
              <Space>
                <BarChartOutlined />
                <span>主流币种市值对比 (Market Cap Ranking)</span>
              </Space>
            }
            bordered={false}
          >
            <div style={{ height: 350, width: '100%' }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData.slice(0, 10)} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f0f0f0" />
                  <XAxis type="number" hide />
                  <YAxis 
                    dataKey="symbol" 
                    type="category" 
                    axisLine={false}
                    tickLine={false}
                    width={60}
                  />
                  <RechartsTooltip 
                    cursor={{ fill: 'rgba(0,0,0,0.05)' }}
                    content={<CustomTooltip type="bar" />}
                  />
                  <Bar dataKey="marketCap" radius={[0, 4, 4, 0]} barSize={20}>
                    {chartData.slice(0, 10).map((_, index) => (
                      <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card title="24h 涨幅榜 (Top Gainers)" bordered={false} bodyStyle={{ padding: '12px 24px' }}>
                <List
                  dataSource={topGainers}
                  renderItem={(item) => (
                    <List.Item>
                      <List.Item.Meta
                        avatar={
                          <Avatar src={`https://cryptoicons.org/api/icon/${item.symbol?.toLowerCase()}/200`} />
                        }
                        title={item.symbol}
                        description={item.name}
                      />
                      <div style={{ color: Number(item.change24h) >= 0 ? '#52c41a' : '#ff4d4f', fontWeight: 'bold' }}>
                        {Number(item.change24h) >= 0 ? <RiseOutlined /> : <FallOutlined />} {Number(item.change24h) >= 0 ? '+' : ''}{item.change24h}%
                      </div>
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
            <Col span={24}>
              <Card title="24h 跌幅榜 (Top Losers)" bordered={false} bodyStyle={{ padding: '12px 24px' }}>
                <List
                  dataSource={topLosers}
                  renderItem={(item) => (
                    <List.Item>
                      <List.Item.Meta
                        avatar={
                          <Avatar src={`https://cryptoicons.org/api/icon/${item.symbol?.toLowerCase()}/200`} />
                        }
                        title={item.symbol}
                        description={item.name}
                      />
                      <div style={{ color: Number(item.change24h) >= 0 ? '#52c41a' : '#ff4d4f', fontWeight: 'bold' }}>
                        {Number(item.change24h) >= 0 ? <RiseOutlined /> : <FallOutlined />} {Number(item.change24h) >= 0 ? '+' : ''}{item.change24h}%
                      </div>
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
          </Row>
        </Col>
      </Row>
      </Spin>
    </PageContainer>
  );
};

export default MetricsDashboard;