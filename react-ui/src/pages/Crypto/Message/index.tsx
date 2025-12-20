import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Tag, message, Button, Space, Avatar, Typography, Row, Col, Card, Statistic, Spin, Tooltip, Dropdown } from 'antd';
import { EyeOutlined, ReloadOutlined, StockOutlined, RiseOutlined, FallOutlined, InfoCircleOutlined, MessageOutlined, PieChartOutlined, FireOutlined, ColumnHeightOutlined } from '@ant-design/icons';
import React, { useRef, useState, useEffect } from 'react';
import { getMessageList, triggerCollectNews } from '@/services/crypto/api';
import { useNavigate, useLocation } from '@umijs/max';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts';

const { Paragraph, Text } = Typography;

const COLORS = ['#52c41a', '#ff4d4f', '#faad14', '#1890ff', '#722ed1'];

const MessageTable: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const navigate = useNavigate();
  const location = useLocation();
  const [selectedMessageId, setSelectedMessageId] = useState<number | undefined>();
  const [loading, setLoading] = useState(false);
  const [tableSize, setTableSize] = useState<'default' | 'middle' | 'small'>('middle');
  const [stats, setStats] = useState({
    sentimentData: [] as any[],
    coinData: [] as any[],
    totalCount: 0,
    positiveCount: 0
  });

  const updateStats = (data: API.BizCryptoMessage[]) => {
    const total = data.length;
    const sentiments = data.reduce((acc: any, curr) => {
      const s = curr.sentiment || 'NEUTRAL';
      acc[s] = (acc[s] || 0) + 1;
      return acc;
    }, {});

    const coins = data.reduce((acc: any, curr) => {
      if (curr.coin) {
        acc[curr.coin] = (acc[curr.coin] || 0) + 1;
      }
      return acc;
    }, {});

    setStats({
      sentimentData: [
        { name: '利好', value: sentiments['POSITIVE'] || 0, color: '#52c41a' },
        { name: '利空', value: sentiments['NEGATIVE'] || 0, color: '#ff4d4f' },
        { name: '中性', value: sentiments['NEUTRAL'] || 0, color: '#faad14' },
      ],
      coinData: Object.entries(coins)
        .map(([name, value]) => ({ name, value }))
        .sort((a: any, b: any) => b.value - a.value)
        .slice(0, 5),
      totalCount: total,
      positiveCount: sentiments['POSITIVE'] || 0
    });
  };

  // 从location.state中获取selectedMessageId并设置筛选条件
  useEffect(() => {
    if ((location.state as { selectedMessageId?: number })?.selectedMessageId) {
      setSelectedMessageId((location.state as { selectedMessageId?: number }).selectedMessageId);
      // 如果表格已加载，执行筛选
      if (actionRef.current) {
        // 使用 reload 重新加载数据
        actionRef.current?.reload();
      }
    }
  }, [location.state]);

  const columns: ProColumns<API.BizCryptoMessage>[] = [
    { title: 'ID', dataIndex: 'id', width: 48, search: false },
    {
      title: '币种',
      dataIndex: 'coin',
      width: 100,
      valueType: 'select',
      valueEnum: {
        BTC: { text: 'BTC', status: 'Processing' },
        ETH: { text: 'ETH', status: 'Processing' },
        SOL: { text: 'SOL', status: 'Processing' },
      },
      render: (_, record) => (
        <Space>
          <Avatar 
            size="small" 
            src={`https://cryptoicons.org/api/icon/${record.coin?.toLowerCase()}/200`}
            icon={<StockOutlined />}
            style={{ backgroundColor: '#1890ff' }}
          />
          <Text strong>{record.coin}</Text>
        </Space>
      ),
    },
    {
      title: '消息内容',
      dataIndex: 'content',
      width: '40%',
      render: (_, record) => {
        const isPositive = record.sentiment === 'POSITIVE';
        const isNegative = record.sentiment === 'NEGATIVE';
        let bgColor = 'transparent';
        if (isPositive) bgColor = '#f6ffed';
        if (isNegative) bgColor = '#fff1f0';

        return (
          <div style={{ 
            padding: '8px 12px', 
            borderRadius: '4px', 
            backgroundColor: bgColor,
            borderLeft: isPositive ? '4px solid #52c41a' : isNegative ? '4px solid #ff4d4f' : '4px solid #d9d9d9'
          }}>
            <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: '展开' }} style={{ marginBottom: 0 }}>
              {record.content}
            </Paragraph>
          </div>
        );
      }
    },
    {
      title: '情感',
      dataIndex: 'sentiment',
      width: 100,
      valueEnum: {
        POSITIVE: { text: '利好', status: 'Success' },
        NEGATIVE: { text: '利空', status: 'Error' },
        NEUTRAL: { text: '中性', status: 'Default' },
      },
      render: (_, record) => {
        if (record.sentiment === 'POSITIVE') return <Tag color="success" icon={<RiseOutlined />}>利好</Tag>;
        if (record.sentiment === 'NEGATIVE') return <Tag color="error" icon={<FallOutlined />}>利空</Tag>;
        return <Tag icon={<InfoCircleOutlined />}>中性</Tag>;
      }
    },
    {
      title: '影响分数',
      dataIndex: 'impactScore',
      width: 100,
      render: (_, record) => {
        const score = parseFloat(record.impactScore);
        let color = 'default';
        let prefix = '';
        if (score > 0.6) { color = '#52c41a'; prefix = '🔥'; }
        else if (score > 0.3) { color = '#faad14'; prefix = '⚡'; }
        else if (score < -0.6) { color = '#ff4d4f'; prefix = '💎'; }
        
        return (
          <Text style={{ color, fontWeight: 'bold' }}>
            {prefix} {record.impactScore}
          </Text>
        );
      }
    },
    { title: '来源', dataIndex: 'source' },
    { title: '发布时间', dataIndex: 'publishTime', valueType: 'dateTime' },
    { title: '创建时间', dataIndex: 'createTime', valueType: 'dateTime', search: false },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => [
        <Button 
          key="view-report" 
          type="link"
          icon={<EyeOutlined />}
          onClick={() => {
            // 导航到报告页面并筛选当前消息的报告
            navigate('/crypto/report', { state: { messageId: record.id } });
          }}
        >
          查看报告
        </Button>
      ],
    },
  ];

  return (
    <PageContainer 
      title="AI 市场情报分析看板"
      extra={[
        <Button
          type="primary"
          key="refresh"
          icon={<ReloadOutlined />}
          loading={loading}
          onClick={async () => {
            try {
              setLoading(true);
              await triggerCollectNews();
              message.success('已成功触发AI收集新闻，正在同步数据...');
              // 模拟一个较长时间的同步过程，或者多次轮询
              await new Promise(resolve => setTimeout(resolve, 2000));
              actionRef.current?.reload();
            } catch (error) {
              message.error('触发AI收集新闻失败');
            } finally {
              setLoading(false);
            }
          }}
        >
          同步最新情报
        </Button>,
        <Tooltip key="reload" title="刷新表格">
          <Button 
            key="reload_btn" 
            icon={<ReloadOutlined />} 
            onClick={() => actionRef.current?.reload()} 
          />
        </Tooltip>,
        <Dropdown
          key="density"
          menu={{
            items: [
              { key: 'default', label: '默认' },
              { key: 'middle', label: '中等' },
              { key: 'small', label: '紧凑' },
            ],
            onClick: ({ key }) => setTableSize(key as any),
            selectedKeys: [tableSize],
          }}
          trigger={['click']}
        >
          <Tooltip title="密度">
            <Button icon={<ColumnHeightOutlined />} />
          </Tooltip>
        </Dropdown>,
      ]}
    >
      <Spin spinning={loading} tip="正在同步最新市场情报，请稍候..." size="large">
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={24} sm={12} md={6}>
            <Card bordered={false} style={{ height: '180px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Statistic
                title="今日情报总数"
                value={stats.totalCount}
                prefix={<MessageOutlined />}
                valueStyle={{ color: '#1890ff', fontSize: '24px' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card bordered={false} style={{ height: '180px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Statistic
                title="利好情报占比"
                value={stats.totalCount > 0 ? (stats.positiveCount / stats.totalCount) * 100 : 0}
                precision={1}
                suffix="%"
                prefix={<RiseOutlined />}
                valueStyle={{ color: '#52c41a', fontSize: '24px' }}
              />
            </Card>
          </Col>
          <Col xs={24} md={6}>
            <Card title="情感分布" bordered={false} bodyStyle={{ padding: '10px' }} style={{ height: '180px' }}>
              <div style={{ height: 100 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={stats.sentimentData}
                      innerRadius={20}
                      outerRadius={30}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {stats.sentimentData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <RechartsTooltip />
                    <Legend verticalAlign="middle" align="right" layout="vertical" />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </Card>
          </Col>
          <Col xs={24} md={6}>
            <Card title="热门讨论币种" bordered={false} bodyStyle={{ padding: '10px' }} style={{ height: '180px' }}>
              <div style={{ height: 100 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={stats.coinData}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="name" hide />
                    <YAxis hide />
                    <RechartsTooltip />
                    <Bar dataKey="value" fill="#1890ff" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Card>
          </Col>
        </Row>

        <ProTable<API.BizCryptoMessage>
          headerTitle="市场实时情报流"
          actionRef={actionRef}
          rowKey="id"
          search={false}
          options={false}
          size={tableSize}
          pagination={{ pageSize: 10 }}
          request={async (params) => {
            const requestParams = selectedMessageId ? { ...params, id: selectedMessageId } : params;
            const msg = await getMessageList(requestParams);
            const rows = Array.isArray(msg) ? msg : (msg as any).rows || [];
            const total = Array.isArray(msg) ? msg.length : (msg as any).total || 0;
            updateStats(rows);
            return {
              data: rows,
              success: true,
              total,
            };
          }}
          columns={columns}
        />
      </Spin>
    </PageContainer>
  );
};

export default MessageTable;
