import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { message, Modal, Input } from 'antd';
import React, { useRef, useState } from 'react';
import { getReportList, updateReport } from '@/services/crypto/api';

const ReportTable: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [rejectReason, setRejectReason] = useState('');

  // 审核操作
  const handleAudit = async (id: number, status: string, reason: string = '') => {
    try {
      await updateReport({ id, status, rejectReason: reason, auditBy: 'Admin' });
      message.success('操作成功');
      setRejectModalOpen(false);
      actionRef.current?.reload();
    } catch (error) {
      message.error('操作失败');
    }
  };

  const columns: ProColumns<API.BizInvestmentReport>[] = [
    { title: 'ID', dataIndex: 'id', width: 48, search: false },
    {
      title: '关联消息ID',
      dataIndex: 'messageId',
      width: 80,
      render: (dom) => <a onClick={() => message.info('查看关联消息功能待开发')}>{dom}</a>
    },
    { title: 'AI 分析结果', dataIndex: 'analysisResult', ellipsis: true },
    { title: 'AI 建议内容', dataIndex: 'adviceContent', ellipsis: true, width: '30%' },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: {
        '0': { text: '待审核', status: 'Processing' },
        '1': { text: '已通过', status: 'Success' },
        '2': { text: '已驳回', status: 'Error' },
      },
    },
    { title: '审核人', dataIndex: 'auditBy', search: false },
    { title: '审核时间', dataIndex: 'auditTime', valueType: 'dateTime', search: false },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => {
        if (record.status !== '0') return null; // 非待审核状态不显示按钮
        return [
          <a key="pass" onClick={() => handleAudit(record.id, '1')}>
            通过
          </a>,
          <a
            key="reject"
            style={{ color: 'red' }}
            onClick={() => {
              setCurrentId(record.id);
              setRejectModalOpen(true);
            }}
          >
            驳回
          </a>,
        ];
      },
    },
  ];

  return (
    <PageContainer>
      <ProTable<API.BizInvestmentReport>
        headerTitle="AI 投资建议报告"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 120 }}
        request={async (params) => {
          const msg = await getReportList(params);
          return {
            data: msg.rows,
            success: true,
            total: msg.total,
          };
        }}
        columns={columns}
      />

      <Modal
        title="请输入驳回原因"
        open={rejectModalOpen}
        onOk={() => handleAudit(currentId!, '2', rejectReason)}
        onCancel={() => setRejectModalOpen(false)}
      >
        <Input.TextArea
          rows={4}
          placeholder="请输入具体的驳回原因，以便AI重新分析..."
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>
    </PageContainer>
  );
};

export default ReportTable;
