import {
  addHoldings,
  getHoldingsList,
  removeHoldings,
  updateHoldings,
} from '@/services/crypto/api';
import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, message, Popconfirm, Card, Row, Col, Statistic } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { Pie } from '@ant-design/plots';

const HoldingsTable: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [holdingsData, setHoldingsData] = useState<API.BizAssetHoldings[]>([]);
  const [totalValue, setTotalValue] = useState<number>(0);

  // 获取持仓数据
  const fetchHoldingsData = async () => {
    try {
      const response = await getHoldingsList();
      // 处理API响应，根据实际返回结构调整
      const holdings = Array.isArray(response) 
        ? response 
        : Array.isArray((response as any).rows) 
          ? (response as any).rows 
          : [];
      setHoldingsData(holdings);
      // 计算总资产
      const total = holdings.reduce((sum: number, item: API.BizAssetHoldings) => sum + (item.usdtValue || 0), 0);
      setTotalValue(total);
    } catch (error) {
      message.error('获取持仓数据失败');
    }
  };

  // 删除处理
  const handleDelete = async (id: number) => {
    try {
      await removeHoldings(String(id));
      message.success('删除成功');
      actionRef.current?.reload();
      fetchHoldingsData(); // 更新饼图数据
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 转换数据格式用于饼图
  const pieData = holdingsData.map(item => ({
    name: item.coin,  // 使用name字段以便饼图标签正确显示
    type: item.coin,
    value: item.usdtValue || 0,
  }));

  // 饼图配置
  const pieConfig = {
    data: pieData,
    angleField: 'value',
    colorField: 'type',
    radius: 0.8,
    label: {
      type: 'outer',
      content: '{name}: {percentage}',
    },
    interactions: [
      {
        type: 'pie-legend-active',
      },
      {
        type: 'element-active',
      },
    ],
  };

  const columns: ProColumns<API.BizAssetHoldings>[] = [
    { title: 'ID', dataIndex: 'id', width: 48, hideInForm: true, search: false },
    { title: '币种', dataIndex: 'coin', valueType: 'text' },
    { title: '持有数量', dataIndex: 'amount', valueType: 'digit', fieldProps: { precision: 8 } },
    { title: '当前估值(USDT)', dataIndex: 'usdtValue', valueType: 'money' },
    { title: '平均成本', dataIndex: 'costPrice', valueType: 'money', search: false },
    { title: '更新时间', dataIndex: 'updateTime', valueType: 'dateTime', editable: false, search: false },
    {
      title: '操作',
      valueType: 'option',
      render: (text, record, _, action) => [
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

  // 初始加载数据和定期轮询
  useEffect(() => {
    fetchHoldingsData();
    
    // 每隔30秒自动刷新一次数据，确保审核后持仓变化能及时反映
    const interval = setInterval(() => {
      fetchHoldingsData();
    }, 30000);
    
    // 清理定时器
    return () => clearInterval(interval);
  }, []);

  return (
    <PageContainer title="企业持仓资产">
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card title="持仓分布(USDT)">
            <Statistic
              title="总资产"
              value={totalValue}
              precision={2}
              valueStyle={{ color: '#3f8600' }}
              suffix="USDT"
            />
            <div style={{ marginTop: 24, height: 300 }}>
              {pieData.length > 0 ? (
                <Pie {...pieConfig} />
              ) : (
                <div style={{ textAlign: 'center', paddingTop: 100, color: '#999' }}>
                  暂无持仓数据
                </div>
              )}
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={16}>
          <ProTable<API.BizAssetHoldings>
            headerTitle="持仓详情"
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
              // 处理API响应，根据实际返回结构调整
              const data = Array.isArray(msg) 
                ? msg 
                : Array.isArray((msg as any).rows) 
                  ? (msg as any).rows 
                  : [];
              const total = typeof (msg as any).total === 'number' ? (msg as any).total : data.length;
              return {
                data,
                success: true,
                total,
              };
            }}
            columns={columns}
            editable={{
              type: 'multiple',
              onSave: async (rowKey, data, row) => {
                // 使用id是否存在判断是新增还是修改
                if (data.id && typeof data.id === 'number') {
                  await updateHoldings(data);
                } else {
                  await addHoldings(data);
                }
                message.success('保存成功');
                fetchHoldingsData(); // 更新饼图数据
              },
              onDelete: async (key) => {
                await removeHoldings(String(key));
                fetchHoldingsData(); // 更新饼图数据
              },
            }}
          />
        </Col>
      </Row>
    </PageContainer>
  );
};

export default HoldingsTable;
