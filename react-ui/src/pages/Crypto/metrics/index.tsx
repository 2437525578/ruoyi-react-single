/**
 * @name 虚拟货币行情
 * icon: bitcoin
 */

import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import React, { useRef } from 'react';
import { getMetricsList, triggerCollectMetrics } from '@/services/crypto/api';

const MetricsTable: React.FC = () => {
  const actionRef = useRef<ActionType>();

  const columns: ProColumns<any>[] = [
    { title: 'ID', dataIndex: 'id', width: 80, search: false },
    {
      title: '币种符号',
      dataIndex: 'symbol',
      search: true,
      sorter: true,
      width: 100,
    },
    {
      title: '币种名称',
      dataIndex: 'name',
      search: true,
      sorter: true,
    },
    {
      title: '当前价格(USD)',
      dataIndex: 'priceUsd',
      valueType: 'money',
      sorter: true,
      hideInSearch: true,
    },
    {
      title: '市值(亿美元)',
      dataIndex: 'marketCap',
      valueType: 'money',
      sorter: true,
      hideInSearch: true,
    },
    {
      title: '24h涨跌幅(%)',
      dataIndex: 'change24h',
      sorter: true,
      hideInSearch: true,
      render: (_, record) => {
        if (record.change24h == null) return '-';
        const value = Number(record.change24h);
        return (
          <span style={{ color: value > 0 ? '#52c41a' : value < 0 ? '#ff4d4f' : '#666' }}>
            {value > 0 ? '+' : ''}{value.toFixed(2)}%
          </span>
        );
      },
    },
    {
      title: '哈希率变化(%)',
      dataIndex: 'hashRate',
      hideInSearch: true,
      render: (_, record) => {
        if (record.hashRate == null) return '-';
        const value = Number(record.hashRate);
        return (
          <span style={{ color: value > 0 ? '#52c41a' : value < 0 ? '#ff4d4f' : '#666' }}>
            {value > 0 ? '+' : ''}{value.toFixed(2)}%
          </span>
        );
      },
    },
    {
      title: '流通量/交易量',
      dataIndex: 'transactionCount',
      hideInSearch: true,
    },
    {
      title: '总手续费',
      dataIndex: 'totalFeesBtc',
      valueType: 'money',
      hideInSearch: true,
    },
    {
      title: '区块相关指标',
      dataIndex: 'blockCount',
      hideInSearch: true,
    },
    {
      title: '历史最高价(USD)',
      dataIndex: 'athPrice',
      valueType: 'money',
      hideInSearch: true,
    },
    {
      title: '数据爬取时间',
      dataIndex: 'snapshotTime',
      valueType: 'dateTime',
      search: false,
    },
    {
      title: '创建者',
      dataIndex: 'createBy',
    },
    {
      title: '更新者',
      dataIndex: 'updateBy',
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      valueType: 'dateTime',
      search: false,
    },
  ];

  return (
    <PageContainer>
      <ProTable<any>
        headerTitle="AI采集虚拟货币数据"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 120 }}
        toolBarRender={() => [
          <Button
            type="primary"
            key="collect"
            icon={<ReloadOutlined />}
            onClick={async () => {
              try {
                await triggerCollectMetrics();
                message.success('已成功触发采集虚拟货币数据');
                actionRef.current?.reload();
              } catch (error) {
                message.error('触发采集失败');
                console.error(error);
              }
            }}
          >
            手动收集数据
          </Button>,
        ]}
        request={async (params) => {
          const res = await getMetricsList(params);
          return {
            data: res.rows || [],
            success: true,
            total: res.total || 0,
          };
        }}
        columns={columns}
        pagination={{
          pageSize: 20,
          showSizeChanger: true,
        }}
        options={{
          reload: true,
          density: true,
          fullScreen: true,
        }}
      />
    </PageContainer>
  );
};

export default MetricsTable;