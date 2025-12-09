import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Card, Row, Col, message, Popconfirm } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { getHoldingsList, addHoldings, updateHoldings, removeHoldings } from '@/services/crypto/api';

const HoldingsTable: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [holdingsData, setHoldingsData] = useState<API.BizAssetHoldings[]>([]);
  const [totalAssets, setTotalAssets] = useState<number>(0);

  // 生成随机颜色
  const generateColors = (count: number) => {
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E2'];
    return Array.from({ length: count }, (_, i) => colors[i % colors.length]);
  };

  // 获取持仓数据
  const fetchHoldingsData = async () => {
    try {
      const response = await getHoldingsList();
      const data = Array.isArray(response) ? response : (response as any).rows || [];
      setHoldingsData(data as API.BizAssetHoldings[]);
      
      // 计算总资产
      const total = data.reduce((sum: number, item: API.BizAssetHoldings) => sum + item.usdtValue, 0);
      setTotalAssets(total);
    } catch (error) {
      message.error('获取持仓数据失败');
    }
  };

  // 组件挂载时获取数据，并设置定时刷新
  useEffect(() => {
    fetchHoldingsData();
    
    // 设置定时器，每30秒刷新一次数据
    const intervalId = setInterval(() => {
      fetchHoldingsData();
    }, 30000);
    
    // 组件卸载时清除定时器
    return () => clearInterval(intervalId);
  }, []);

  // 删除处理
  const handleDelete = async (id: number) => {
    try {
      await removeHoldings(String(id));
      message.success('删除成功');
      actionRef.current?.reload();
      fetchHoldingsData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const columns: ProColumns<API.BizAssetHoldings>[] = [
    { title: 'ID', dataIndex: 'id', width: 48, hideInForm: true, search: false },
    { title: '币种', dataIndex: 'coin', valueType: 'text' },
    {
      title: '持有数量',
      dataIndex: 'amount',
      valueType: 'digit',
      fieldProps: { precision: 8 }
    },
    {
      title: '当前估值(USDT)',
      dataIndex: 'usdtValue',
      valueType: 'money'
    },
    {
      title: '平均成本',
      dataIndex: 'costPrice',
      valueType: 'money',
      search: false
    },
    { title: '更新时间', dataIndex: 'updateTime', valueType: 'dateTime', editable: false, search: false },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record, __, action) => [
        <a
          key="editable"
          onClick={() => {
            action?.startEditable?.(record.id);
          }}
        >
          编辑
        </a>,
        <Popconfirm key="delete" title="确定删除吗?" onConfirm={() => handleDelete(record.id)}>
          <a style={{ color: 'red' }}>删除</a>
        </Popconfirm>,
      ],
    },
  ];

  // 转换数据用于饼图
  const pieChartData = holdingsData.map(item => ({
    name: item.coin,
    value: item.usdtValue,
    percentage: totalAssets > 0 ? ((item.usdtValue / totalAssets) * 100).toFixed(2) : '0.00',
  }));

  const colors = generateColors(pieChartData.length);

  return (
    <PageContainer title="企业持仓资产">
      <Row gutter={[16, 16]}>
        {/* 饼图展示 */}
        <Col xs={24} lg={8}>
          <Card title="持仓分布">
            <div style={{ textAlign: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0 }}>总资产: {totalAssets.toFixed(2)} USDT</h3>
            </div>
            <div style={{ height: '300px' }}>
              <div style={{ width: '100%', height: '100%' }}></div>
              <ResponsiveContainer>
                <PieChart>
                  <Pie
                    data={pieChartData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    outerRadius={100}
                    fill="#8884d8"
                    dataKey="value"
                    label={({ name, percent = 0 }) => `${name}: ${(percent * 100).toFixed(2)}%`}
                  >
                    {pieChartData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value: number) => [`${value} USDT`, '估值']} />

                </PieChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
        
        {/* 持仓表格 */}
        <Col xs={24} lg={16}>
          <Card>
            <ProTable<API.BizAssetHoldings>
              headerTitle={false}
              actionRef={actionRef}
              rowKey="id"
              search={{ labelWidth: 120 }}
              toolBarRender={() => [
                <Button
                  type="primary"
                  key="primary"
                  onClick={() => {
                    actionRef.current?.addEditRecord?.({
                      id: (Math.random() * 1000000).toFixed(0),
                    });
                  }}
                >
                  <PlusOutlined /> 新增持仓
                </Button>,
              ]}
              request={async (params) => {
                const msg = await getHoldingsList(params);
                // 更新持仓数据用于饼图
                const rows = Array.isArray(msg) ? msg : (msg as any).rows || [];
                const total = Array.isArray(msg) ? msg.length : (msg as any).total || 0;
                setHoldingsData(rows as API.BizAssetHoldings[]);
                const assetsTotal = rows.reduce((sum: number, item: API.BizAssetHoldings) => sum + item.usdtValue, 0);
                setTotalAssets(assetsTotal);
                return {
                  data: rows,
                  success: true,
                  total,
                };
              }}
              columns={columns}
              editable={{
                type: 'multiple',
                onSave: async (_, data, row) => {
                  if (row.id && Number(row.id) > 0) { // 判断是新增还是修改的简单逻辑，使用id判断
                    await updateHoldings(data);
                  } else {
                    await addHoldings(data);
                  }
                  message.success('保存成功');
                  fetchHoldingsData();
                },
                onDelete: async (key) => {
                  await removeHoldings(String(key));
                  fetchHoldingsData();
                }
              }}
            />
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
};

export default HoldingsTable;
