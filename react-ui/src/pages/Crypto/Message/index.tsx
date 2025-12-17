import { getMessageList, triggerCollectNews } from '@/services/crypto/api';
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useLocation, useNavigate } from '@umijs/max';
import { Button, Tag, message } from 'antd';
import React, { useEffect, useRef, useState } from 'react';

const MessageTable: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const navigate = useNavigate();
  const location = useLocation();
  const [selectedMessageId, setSelectedMessageId] = useState<number | undefined>();

  // 从location.state中获取selectedMessageId并设置筛选条件
  useEffect(() => {
    const state = location.state as { selectedMessageId?: number };
    if (state?.selectedMessageId) {
      setSelectedMessageId(state.selectedMessageId);
      // 重新加载数据以应用筛选
      actionRef.current?.reload();
    }
  }, [location.state]);

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
      },
    },
    {
      title: '消息内容',
      dataIndex: 'content',
      ellipsis: true,
      copyable: true,
      width: '30%',
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
        const color =
          record.sentiment === 'POSITIVE'
            ? 'green'
            : record.sentiment === 'NEGATIVE'
            ? 'red'
            : 'default';
        return <Tag color={color}>{record.sentiment}</Tag>;
      },
    },
    {
      title: '影响分数',
      dataIndex: 'impactScore',
      render: (_, record) => {
        const score = parseFloat(record.impactScore);
        let color = 'default';
        if (score > 0.6) color = 'red';
        else if (score > 0.3) color = 'orange';
        else if (score < -0.6) color = 'green';
        else if (score < -0.3) color = 'cyan';
        return <Tag color={color}>{record.impactScore}</Tag>;
      },
    },
    { title: '来源', dataIndex: 'source' },
    { title: '发布时间', dataIndex: 'publishTime', valueType: 'dateTime' },
    { title: '创建时间', dataIndex: 'createTime', valueType: 'dateTime', search: false },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => [
        <a
          key="view-report"
          onClick={() => {
            // 导航到报告页面并筛选当前消息的报告
            navigate('/crypto/report', { state: { messageId: record.id } });
          }}
        >
          <EyeOutlined /> 查看报告
        </a>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<API.BizCryptoMessage>
        headerTitle="AI 采集市场消息"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 120 }}
        toolBarRender={() => [
          <Button
            type="primary"
            key="refresh"
            icon={<ReloadOutlined />}
            onClick={async () => {
              try {
                await triggerCollectNews();
                message.success('已成功触发AI收集新闻');
                actionRef.current?.reload();
              } catch (error) {
                message.error('触发AI收集新闻失败');
                console.error(error);
              }
            }}
          >
            手动收集新闻
          </Button>,
        ]}
        request={async (params) => {
          // 如果有selectedMessageId，添加到请求参数中
          const requestParams = selectedMessageId ? { ...params, id: selectedMessageId } : params;
          const msg = await getMessageList(requestParams);
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
      />
    </PageContainer>
  );
};

export default MessageTable;
