import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { message, Modal, Input, Tag, Button, Typography, Space, Badge, Avatar, Card, Row, Col, Statistic, Spin, Tooltip, Dropdown } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import { getReportList, updateReport, generateReport } from '@/services/crypto/api';
import { useLocation, useNavigate } from '@umijs/max';
import { EyeOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined, RobotOutlined, ShoppingCartOutlined, LineChartOutlined, AuditOutlined, BarChartOutlined, ReloadOutlined, ColumnHeightOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';

const { Paragraph, Text } = Typography;

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
  const [loading, setLoading] = useState(false);
  const [tableSize, setTableSize] = useState<'default' | 'middle' | 'small'>('middle');
  const [stats, setStats] = useState({
    auditData: [] as any[],
    tradeData: [] as any[],
    pendingCount: 0,
    totalCount: 0
  });

  const updateStats = (data: API.BizInvestmentReportItem[]) => {
    const total = data.length;
    const auditMap = data.reduce((acc: any, curr: any) => {
      const s = curr.status || '0';
      acc[s] = (acc[s] || 0) + 1;
      return acc;
    }, {});

    let buyCount = 0;
    let sellCount = 0;
    data.forEach(item => {
      if (item.executeJson) {
        try {
          const actions = JSON.parse(item.executeJson);
          actions.forEach((a: any) => {
            if (a.type === 'BUY') buyCount++;
            if (a.type === 'SELL') sellCount++;
          });
        } catch (e) {}
      }
    });

    setStats({
      auditData: [
        { name: '待审核', value: auditMap['0'] || 0, color: '#faad14' },
        { name: '已通过', value: auditMap['1'] || 0, color: '#52c41a' },
        { name: '已驳回', value: auditMap['2'] || 0, color: '#ff4d4f' },
      ],
      tradeData: [
        { name: '买入', value: buyCount, color: '#52c41a' },
        { name: '卖出', value: sellCount, color: '#ff4d4f' },
      ],
      pendingCount: auditMap['0'] || 0,
      totalCount: total
    });
  };

  // 从location.state中获取messageId并设置筛选条件
  useEffect(() => {
    if ((location.state as { messageId?: number })?.messageId) {
      setMessageId((location.state as { messageId?: number }).messageId);
      // 如果表格已加载，执行筛选
      if (actionRef.current) {
        // 通过刷新表格触发搜索条件生效
        actionRef.current?.reload();
      }
    }
  }, [location.state]);

  // 审核操作
  const handleAudit = async (id: number, status: string, reason: string = '') => {
    try {
      setLoading(true);
      // 发送审核请求，使用Status: Approved格式
      await updateReport({ 
        id, 
        status, 
        rejectReason: reason, 
        auditBy: 'Admin',
        // 按照要求添加Status字段
        Status: status === '1' ? 'Approved' : 'Rejected'
      });
      
      if (status === '1') {
        message.success('审核通过，自动调仓指令已触发');
        message.info('持仓数据正在更新，请刷新持仓页面查看最新分布');
      } else {
        message.success('操作成功');
      }
      setRejectModalOpen(false);
      actionRef.current?.reload();
    } catch (error) {
      message.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  // 生成报告操作
  const handleGenerateReport = async () => {
    if (!inputMessageId || isNaN(Number(inputMessageId))) {
      message.error('请输入有效的消息ID');
      return;
    }
    
    try {
      setLoading(true);
      await generateReport({ messageId: Number(inputMessageId) });
      message.success('报告生成成功');
      setGenerateModalOpen(false);
      setInputMessageId('');
      actionRef.current?.reload();
    } catch (error) {
      message.error('报告生成失败');
    } finally {
      setLoading(false);
    }
  };

  const columns: ProColumns<API.BizInvestmentReportItem>[] = [
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
      title: 'AI 分析 & 建议', 
      dataIndex: 'analysisResult', 
      width: '40%',
      render: (_, record) => (
        <Space direction="vertical" size={0} style={{ width: '100%' }}>
          <div style={{ marginBottom: 8 }}>
            <Badge status="processing" text={<Text strong>分析结果</Text>} />
            <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: '展开' }} style={{ fontSize: '13px', color: '#595959', marginTop: 4 }}>
              {record.analysisResult}
            </Paragraph>
          </div>
          <div>
            <Badge status="success" text={<Text strong>建议操作</Text>} />
            <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: '展开' }} style={{ fontSize: '13px', color: '#595959', marginTop: 4 }}>
              {record.adviceContent}
            </Paragraph>
          </div>
        </Space>
      )
    },
    {
      title: '调仓指令',
      dataIndex: 'executeJson',
      width: 180,
      search: false,
      render: (text) => {
        if (!text || text === '[]') return <Text type="secondary">无指令</Text>;
        try {
          const actions = JSON.parse(text as string);
          return (
            <Space direction="vertical" size={4}>
              {actions.map((action: any, index: number) => (
                <Tag 
                  color={action.type === 'BUY' ? 'green' : 'red'} 
                  key={index}
                  icon={action.type === 'BUY' ? <ShoppingCartOutlined /> : <MinusCircleOutlined />}
                  style={{ borderRadius: '12px', padding: '0 10px' }}
                >
                  <Text strong style={{ color: action.type === 'BUY' ? '#3f8600' : '#cf1322' }}>
                    {action.type === 'BUY' ? '买入' : '卖出'} {action.coin} {action.amount}
                  </Text>
                </Tag>
              ))}
            </Space>
          );
        } catch (e) {
          return <Text type="danger">解析错误</Text>;
        }
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      valueEnum: {
        '0': { text: '待审核', status: 'Processing' },
        '1': { text: '已通过', status: 'Success' },
        '2': { text: '已驳回', status: 'Error' },
      },
      render: (_, record) => {
        if (record.status === '0') return <Tag color="processing" icon={<ClockCircleOutlined />}>待审核</Tag>;
        if (record.status === '1') return <Tag color="success" icon={<CheckCircleOutlined />}>已通过</Tag>;
        if (record.status === '2') return <Tag color="error" icon={<CloseCircleOutlined />}>已驳回</Tag>;
        return <Tag>{record.status}</Tag>;
      }
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
      render: (_, record) => {
        if (record.status !== '0') return null;
        return (
          <Space>
            <Button 
              type="primary" 
              size="small" 
              icon={<CheckCircleOutlined />}
              onClick={() => handleAudit(record.id, '1')}
            >
              通过
            </Button>
            <Button 
              danger 
              size="small" 
              icon={<CloseCircleOutlined />}
              onClick={() => {
                setCurrentId(record.id);
                setRejectModalOpen(true);
              }}
            >
              驳回
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <PageContainer 
      title="AI 投资建议生成看板"
      extra={[
        <Button 
          type="primary" 
          key="generate"
          icon={<RobotOutlined />}
          loading={loading}
          onClick={() => setGenerateModalOpen(true)}
        >
          生成AI报告
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
      <Spin spinning={loading} tip="正在生成 AI 投资建议报告，请稍候..." size="large">
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} md={6}>
          <Card bordered={false} style={{ height: '180px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Statistic
              title="待审核建议"
              value={stats.pendingCount}
              prefix={<AuditOutlined />}
              valueStyle={{ color: '#faad14', fontSize: '24px' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card bordered={false} style={{ height: '180px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Statistic
              title="决策总数"
              value={stats.totalCount}
              prefix={<BarChartOutlined />}
              valueStyle={{ color: '#1890ff', fontSize: '24px' }}
            />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card title="审核状态分布" bordered={false} bodyStyle={{ padding: '10px' }} style={{ height: '180px' }}>
            <div style={{ height: 100 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={stats.auditData}
                    innerRadius={20}
                    outerRadius={30}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {stats.auditData.map((entry: any, index: number) => (
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
          <Card title="交易类型占比" bordered={false} bodyStyle={{ padding: '10px' }} style={{ height: '180px' }}>
            <div style={{ height: 100 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={stats.tradeData}
                    innerRadius={20}
                    outerRadius={30}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {stats.tradeData.map((entry: any, index: number) => (
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
      </Row>

      <ProTable<API.BizInvestmentReportItem>
        headerTitle="智能投资建议流"
        actionRef={actionRef}
        rowKey="id"
        search={false}
        options={false}
        size={tableSize}
        pagination={{ pageSize: 10 }}
        request={async (params) => {
          try {
            const requestParams = messageId ? { ...params, messageId } : params;
            const response = await getReportList(requestParams);
            const rows = response.rows || [];
            const total = response.total || 0;
            updateStats(rows);
            
            const processedRows = rows.map(row => {
              if (row.adviceContent && typeof row.adviceContent === 'string') {
                try {
                  const parsed = JSON.parse(row.adviceContent);
                  if (typeof parsed === 'object' && parsed !== null) {
                    let textAdvice = '';
                    for (const [cryptoType, details] of Object.entries(parsed)) {
                      if (Array.isArray(details) && details.length > 0) {
                        textAdvice += `关于${cryptoType}的分析：\n`;
                        details.forEach(item => {
                          if (item.title) textAdvice += `- ${item.title}\n`;
                          if (item.summary) textAdvice += `  ${item.summary}\n`;
                          if (item.influence_score) {
                            const influence = item.influence_score > 0 ? '积极影响' : item.influence_score < 0 ? '消极影响' : '中性影响';
                            textAdvice += `  影响程度：${influence} (${item.influence_score})\n`;
                          }
                          textAdvice += '\n';
                        });
                      }
                    }
                    if (textAdvice) row.adviceContent = textAdvice;
                  }
                } catch (e) {}
              }
              return row;
            });
            
            return { data: processedRows, success: true, total };
          } catch (error) {
            return { data: [], success: false, total: 0 };
          }
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
      </Spin>

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




