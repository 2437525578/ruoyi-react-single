import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Tag } from 'antd';
import React, { useRef } from 'react';
import { getMessageList } from '@/services/crypto/api';

const MessageTable: React.FC = () => {
  const actionRef = useRef<ActionType>();

  const columns: ProColumns<API.BizCryptoMessage>[] = [
    { title: 'ID', dataIndex: 'id', width: 48, search: false },
    {
      title: '币种',
      dataIndex: 'coin',
      valueType: 'select',
      valueEnum: {
        BTC: { text: 'BTC', status: 'Processing' },
        ETH: { text: 'ETH', status: 'Processing' },
        SOL: { text: 'SOL', status: 'Processing' },
      }
    },
    {
      title: '消息内容',
      dataIndex: 'content',
      ellipsis: true,
      copyable: true,
      width: '40%'
    },
    {
      title: '情感倾向',
      dataIndex: 'sentiment',
      valueEnum: {
        POSITIVE: { text: '利好', status: 'Success' },
        NEGATIVE: { text: '利空', status: 'Error' },
        NEUTRAL: { text: '中性', status: 'Default' },
      },
      render: (_, record) => {
        const color = record.sentiment === 'POSITIVE' ? 'green' : record.sentiment === 'NEGATIVE' ? 'red' : 'default';
        return <Tag color={color}>{record.sentiment}</Tag>;
      }
    },
    { title: '来源', dataIndex: 'source' },
    { title: '发布时间', dataIndex: 'publishTime', valueType: 'dateTime' },
  ];

  return (
    <PageContainer>
      <ProTable<API.BizCryptoMessage>
        headerTitle="AI 采集市场消息"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 120 }}
        request={async (params) => {
          const msg = await getMessageList(params);
          return {
            data: msg.rows,
            success: true,
            total: msg.total,
          };
        }}
        columns={columns}
      />
    </PageContainer>
  );
};

export default MessageTable;
