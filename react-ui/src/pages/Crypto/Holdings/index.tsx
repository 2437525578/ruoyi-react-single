import { PlusOutlined, WalletOutlined, ArrowUpOutlined, ArrowDownOutlined, StockOutlined, RiseOutlined, ReloadOutlined, ColumnHeightOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Card, Row, Col, message, Popconfirm, Statistic, Space, Avatar, Typography, Tooltip, Dropdown } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';
import { getHoldingsList, addHoldings, updateHoldings, removeHoldings } from '@/services/crypto/api';

const { Text } = Typography;

const colors = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d'];

const HoldingsTable: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [holdingsData, setHoldingsData] = useState<API.BizAssetHoldings[]>([]);
  const [totalAssets, setTotalAssets] = useState<number>(0);
  const [totalCost, setTotalCost] = useState<number>(0);
  const [totalDailyChange, setTotalDailyChange] = useState<number>(0);
  const [bestPerformer, setBestPerformer] = useState<{ coin: string, profit: number } | null>(null);
  const [tableSize, setTableSize] = useState<'default' | 'middle' | 'small'>('middle');
  const [assetDistribution, setAssetDistribution] = useState<{ name: string, value: number, color: string }[]>([]);

  const pieChartData = holdingsData
    .filter(item => item.usdtValue && Number(item.usdtValue) > 0)
    .map(item => ({
      name: item.coin,
      value: Number(item.usdtValue),
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 6);

  const updateStats = (data: API.BizAssetHoldings[]) => {
    let totalC = 0;
    let totalV = 0;
    let totalD = 0;
    let bestP = { coin: '-', profit: -Infinity };

    data.forEach(item => {
      const cost = Number(item.costPrice || 0) * Number(item.amount || 0);
      const value = Number(item.usdtValue || 0);
      const change24h = Number(item.change24h || 0);
      
      totalC += cost;
      totalV += value;
      
      // 计算该持仓的今日价值变动
      const r = change24h / 100;
      const dailyChange = value - (value / (1 + r));
      totalD += dailyChange;

      if (cost > 0) {
        const profitPct = (value - cost) / cost;
        if (profitPct > bestP.profit) {
          bestP = { coin: item.coin || '-', profit: profitPct };
        }
      }
    });

    setTotalCost(totalC);
    setTotalAssets(totalV);
    setTotalDailyChange(totalD);
    setBestPerformer(bestP.profit === -Infinity ? null : bestP);
  };

  const handleDelete = async (id: number) => {
    try {
      await removeHoldings(id);
      message.success('删除成功');
      actionRef.current?.reload();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const columns: ProColumns<API.BizAssetHoldings>[] = [
    {
      title: '资产',
      dataIndex: 'coin',
      render: (text) => (
        <Space>
          <Avatar 
            size="small" 
            src={`https://cryptoicons.org/api/icon/${String(text).toLowerCase()}/200`}
            icon={<StockOutlined />}
          />
          <Text strong>{text}</Text>
        </Space>
      ),
    },
    {
      title: '持有数量',
      dataIndex: 'amount',
      valueType: 'digit',
      fieldProps: { precision: 8 },
      render: (val) => <Text style={{ fontFamily: 'monospace' }}>{val}</Text>
    },
    {
      title: '平均成本',
      dataIndex: 'costPrice',
      valueType: 'money',
      search: false,
    },
    {
      title: '当前价格',
      dataIndex: 'currentPrice',
      search: false,
      render: (val, record) => {
        const price = Number(val || 0);
        const change = Number(record.change24h || 0);
        const isRise = change >= 0;
        
        // 格式化价格：大额数字加千分位，小额数字保留更多小数
        const formatPrice = (p: number) => {
          if (p >= 1) return p.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
          return p.toFixed(6);
        };

        return (
          <Tooltip title="24h 市场涨跌幅">
            <div style={{ color: isRise ? '#52c41a' : '#ff4d4f' }}>
              <div style={{ fontWeight: 'bold', color: 'rgba(0,0,0,0.85)' }}>
                ${formatPrice(price)}
              </div>
              <div style={{ fontSize: '11px' }}>
                {isRise ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                {isRise ? '+' : ''}{change.toFixed(2)}%
              </div>
            </div>
          </Tooltip>
        );
      }
    },
    {
      title: '当前估值',
      dataIndex: 'usdtValue',
      render: (val, record) => {
        const current = Number(val || 0);
        const change24h = Number(record.change24h || 0);
        const isRise = change24h >= 0;
        
        // 计算24h价值变动 (跟随市场)
        // Yesterday = Current / (1 + r), Change = Current - Yesterday
        const r = change24h / 100;
        const dailyChange = current - (current / (1 + r));
        
        // 计算累计总盈亏 (持仓表现)
        const cost = Number(record.costPrice || 0) * Number(record.amount || 0);
        const totalProfit = current - cost;
        const totalProfitPct = cost > 0 ? (totalProfit / cost) * 100 : 0;
        const isTotalProfit = totalProfit >= 0;

        return (
          <div>
            <div style={{ fontWeight: 'bold', fontSize: '14px' }}>
              ${current.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', marginTop: '4px' }}>
              {/* 1. 今日变动：反映 24h 市场趋势 */}
              <Tooltip title="今日估值变动 (基于 24h 市场涨跌)">
                <div style={{ fontSize: '12px', color: isRise ? '#52c41a' : '#ff4d4f', display: 'flex', alignItems: 'center' }}>
                  {isRise ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                  <span style={{ marginLeft: '4px' }}>
                    {isRise ? '+' : ''}{dailyChange.toFixed(2)} ({change24h.toFixed(2)}%)
                  </span>
                </div>
              </Tooltip>
              
              {/* 2. 累计盈亏：反映 持仓成本表现 */}
              {cost > 0 && (
                <Tooltip title="累计持仓盈亏 (相对于平均成本)">
                  <div style={{ fontSize: '11px', color: isTotalProfit ? '#52c41a' : '#ff4d4f', opacity: 0.7, borderTop: '1px solid #f0f0f0', paddingTop: '2px' }}>
                    Total: {isTotalProfit ? '+' : ''}{totalProfit.toFixed(2)} ({totalProfitPct.toFixed(2)}%)
                  </div>
                </Tooltip>
              )}
            </div>
          </div>
        );
      }
    },
    {
      title: '操作',
      valueType: 'option',
      width: 100,
      render: (_, record, __, action) => [
        <a key="edit" onClick={() => action?.startEditable?.(record.id)}>编辑</a>,
        <Popconfirm key="delete" title="确定删除吗?" onConfirm={() => handleDelete(record.id)}>
          <a style={{ color: 'red' }}>删除</a>
        </Popconfirm>,
      ],
    },
  ];

  const totalProfit = totalAssets - totalCost;
  const totalProfitPct = totalCost > 0 ? (totalProfit / totalCost) * 100 : 0;

  return (
    <PageContainer 
      title="加密货币资产分析看板"
      extra={[
        <Button 
          type="primary" 
          key="add" 
          icon={<PlusOutlined />} 
          onClick={() => actionRef.current?.addEditRecord?.({ id: Date.now() })}
        >
          新增持仓
        </Button>,
        <Tooltip key="reload" title="刷新">
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
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} md={8}>
          <Card bordered={false} style={{ height: '180px', background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)', borderRadius: '8px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
            <Statistic
              title={<span style={{ color: 'rgba(255,255,255,0.85)' }}>当前持仓总估值 (USDT)</span>}
              value={totalAssets}
              precision={2}
              valueStyle={{ color: '#fff', fontSize: '28px', fontWeight: 'bold' }}
              prefix={<WalletOutlined />}
            />
            <div style={{ marginTop: '8px', display: 'flex', gap: '16px' }}>
              <div>
                <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: '12px' }}>今日盈亏：</Text>
                <Text style={{ color: totalDailyChange >= 0 ? '#52c41a' : '#ff4d4f', fontWeight: 'bold' }}>
                  {totalDailyChange >= 0 ? '+' : ''}{totalDailyChange.toFixed(2)}
                  <span style={{ fontSize: '12px', marginLeft: '4px' }}>
                    ({totalAssets > 0 ? (totalDailyChange / (totalAssets - totalDailyChange) * 100).toFixed(2) : 0}%)
                  </span>
                </Text>
              </div>
              <div style={{ borderLeft: '1px solid rgba(255,255,255,0.2)', paddingLeft: '16px' }}>
                <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: '12px' }}>总盈亏：</Text>
                <Text style={{ color: totalProfit >= 0 ? '#52c41a' : '#ff4d4f', fontWeight: 'bold' }}>
                  {totalProfit >= 0 ? '+' : ''}{totalProfit.toFixed(2)} ({totalProfitPct.toFixed(2)}%)
                </Text>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card bordered={false} style={{ height: '180px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
            <Row gutter={16}>
              <Col span={12}>
                <Statistic
                  title="投资总额"
                  value={totalCost}
                  precision={2}
                  prefix="$"
                  valueStyle={{ fontSize: '20px' }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="最佳表现"
                  value={bestPerformer ? (bestPerformer.profit * 100).toFixed(1) : 0}
                  suffix="%"
                  prefix={bestPerformer ? <Avatar size="small" src={`https://cryptoicons.org/api/icon/${bestPerformer.coin.toLowerCase()}/200`} style={{ marginRight: 8 }} /> : <RiseOutlined />}
                  valueStyle={{ color: '#52c41a', fontSize: '20px' }}
                />
              </Col>
            </Row>
            <div style={{ marginTop: 16 }}>
              <Text type="secondary">持仓币种数量：{holdingsData.length}</Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card title="资产占比分布" bordered={false} bodyStyle={{ padding: '10px' }} style={{ height: '180px' }}>
            <div style={{ height: '100px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieChartData}
                    innerRadius={25}
                    outerRadius={35}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {pieChartData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
                    ))}
                  </Pie>
                  <RechartsTooltip />
                  <Legend verticalAlign="middle" align="right" layout="vertical" />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
        
        <Col span={24}>
          <ProTable<API.BizAssetHoldings>
            headerTitle="资产明细清单"
            actionRef={actionRef}
            rowKey="id"
            search={false}
            options={false}
            size={tableSize}
            request={async (params) => {
              const res = await getHoldingsList(params);
              const rows = res?.rows || (Array.isArray(res) ? res : []);
              setHoldingsData(rows);
              updateStats(rows);
              return { data: rows, success: true };
            }}
            columns={columns}
            editable={{
              type: 'multiple',
              onSave: async (_, data) => {
                try {
                  data.id && data.id > 1700000000000 ? await addHoldings(data) : await updateHoldings(data);
                  message.success('保存成功');
                  actionRef.current?.reload();
                } catch (error) {
                  message.error('保存失败');
                }
              }
            }}
          />
        </Col>
      </Row>
    </PageContainer>
  );
};

export default HoldingsTable;
