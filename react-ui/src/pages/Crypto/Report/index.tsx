import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { message, Modal, Input, Tag, Button } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { getReportList, updateReport, generateReport } from '@/services/crypto/api';
import { useLocation, useNavigate } from '@umijs/max';
import { EyeOutlined } from '@ant-design/icons';

const ReportTable: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [rejectReason, setRejectReason] = useState('');
  const location = useLocation();
  const navigate = useNavigate();
  const [messageId, setMessageId] = useState<number | undefined>();
  const [generateModalOpen, setGenerateModalOpen] = useState(false);
  const [inputMessageId, setInputMessageId] = useState('');

  // 从location.state中获取messageId并设置筛选条件
  useEffect(() => {
    if (location.state?.messageId) {
      setMessageId(location.state.messageId);
      // 如果表格已加载，执行筛选
      if (actionRef.current) {
        actionRef.current.setFieldsValue({ messageId: location.state.messageId });
      }
    }
  }, [location.state]);

  // 审核操作
  const handleAudit = async (id: number, status: string, reason: string = '') => {
    try {
      await updateReport({ id, status, rejectReason: reason, auditBy: 'Admin' });
      if (status === '1') {
        message.success('审核通过，自动调仓指令已触发');
      } else {
        message.success('操作成功');
      }
      setRejectModalOpen(false);
      actionRef.current?.reload();
    } catch (error) {
      message.error('操作失败');
      console.error('审核操作失败:', error);
    }
  };

  // 生成报告操作
  const handleGenerateReport = async () => {
    if (!inputMessageId || isNaN(Number(inputMessageId))) {
      message.error('请输入有效的消息ID');
      return;
    }
    
    try {
      await generateReport({ messageId: Number(inputMessageId) });
      message.success('报告生成成功');
      setGenerateModalOpen(false);
      setInputMessageId('');
      actionRef.current?.reload();
    } catch (error) {
      message.error('报告生成失败');
      console.error('报告生成失败:', error);
    }
  };

  const columns: ProColumns<API.BizInvestmentReport>[] = [
    { title: 'ID', dataIndex: 'id', width: 48, search: false },
    { 
      title: '关联消息ID', 
      dataIndex: 'messageId', 
      width: 100, 
      render: (messageId) => (
        <a 
          onClick={() => {
            navigate('/crypto/message', { state: { selectedMessageId: messageId } });
          }}
        >
          {messageId} <EyeOutlined style={{ marginLeft: 4 }} />
        </a>
      ) 
    },
    { 
      title: 'AI 分析结果', 
      dataIndex: 'analysisResult', 
      ellipsis: true,
      width: '30%'
    },
    { 
      title: 'AI 建议内容', 
      dataIndex: 'adviceContent', 
      ellipsis: true, 
      width: '40%'
    },
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
        toolBarRender={() => [
          <Button 
            type="primary" 
            key="generate"
            onClick={() => setGenerateModalOpen(true)}
          >
            手动生成报告
          </Button>,
        ]}
        request={async (params) => {
          // 如果有messageId，添加到请求参数中
          const requestParams = messageId ? { ...params, messageId } : params;
          const msg = await getReportList(requestParams);
          return {
            data: msg.rows,
            success: true,
            total: msg.total,
          };
        }}
        columns={columns}
        expandable={{
          expandedRowRender: (record) => (
            <div style={{ padding: 16 }}>
              <h4 style={{ marginBottom: 8 }}>详细分析结果：</h4>
              <p>{record.analysisResult}</p>
              <h4 style={{ marginTop: 16, marginBottom: 8 }}>详细建议内容：</h4>
              <p>{record.adviceContent}</p>
              {record.rejectReason && (
                <div style={{ marginTop: 16 }}>
                  <h4 style={{ marginBottom: 8 }}>驳回原因：</h4>
                  <p style={{ color: '#ff4d4f' }}>{record.rejectReason}</p>
                </div>
              )}
            </div>
          ),
        }}
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

      <Modal
        title="手动生成报告"
        open={generateModalOpen}
        onOk={handleGenerateReport}
        onCancel={() => {
          setGenerateModalOpen(false);
          setInputMessageId('');
        }}
      >
        <Input
          placeholder="请输入消息ID"
          value={inputMessageId}
          onChange={(e) => setInputMessageId(e.target.value)}
          onPressEnter={handleGenerateReport}
        />
        <p style={{ marginTop: 16, color: '#666' }}>
          系统将根据该消息ID生成投资建议报告，包含当前持仓分析、AI建议和调仓策略
        </p>
      </Modal>
    </PageContainer>
  );
};

export default ReportTable;
