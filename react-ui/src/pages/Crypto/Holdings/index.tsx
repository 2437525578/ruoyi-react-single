import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, message, Popconfirm } from 'antd';
import React, { useRef } from 'react';
import { getHoldingsList, addHoldings, updateHoldings, removeHoldings } from '@/services/crypto/api';

const HoldingsTable: React.FC = () => {
  const actionRef = useRef<ActionType>();

  // 删除处理
  const handleDelete = async (id: number) => {
    try {
      await removeHoldings(String(id));
      message.success('删除成功');
      actionRef.current?.reload();
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

  return (
    <PageContainer>
      <ProTable<API.BizAssetHoldings>
        headerTitle="企业持仓资产"
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
          return {
            data: msg.rows,
            success: true,
            total: msg.total,
          };
        }}
        columns={columns}
        editable={{
          type: 'multiple',
          onSave: async (rowKey, data, row) => {
            if (row.created_at) { // 判断是新增还是修改的简单逻辑，实际可用id是否存在判断
              await updateHoldings(data);
            } else {
              await addHoldings(data);
            }
            message.success('保存成功');
          },
          onDelete: async (key) => {
            await removeHoldings(String(key));
          }
        }}
      />
    </PageContainer>
  );
};

export default HoldingsTable;
