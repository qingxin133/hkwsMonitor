package ClientDemo;


import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
/*****************************************************************************
 * 客流量查询
 ****************************************************************************/
public class SearchDemo {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
	public static PlayCtrl playControl = PlayCtrl.INSTANCE;

	public static HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo;// 设备信息

	public static String m_sDeviceIP;// 已登录设备的IP地址
	public static NativeLong lUserID;// 用户句柄

	/*************************************************
	 * 函数: 主函数 函数描述:新建ClientDemo窗体并调用接口初始化SDK
	 *************************************************/
	public static void main(String args[]) {
		new SearchDemo().doMain();
	}
	/**
	 * 主方法
	 */
	public void doMain() {
		// 初始化
		boolean initSuc = hCNetSDK.NET_DVR_Init();
		if (initSuc != true) {
			System.out.println("初始化失败");
		}
		hCNetSDK.NET_DVR_SetLogToFile(true, "d:\\SdkLog2018\\", false);
		// 设置连接时间与重连时间
		hCNetSDK.NET_DVR_SetConnectTime(2000, 1);
		hCNetSDK.NET_DVR_SetReconnect(10000, true);

		// 注册
		m_sDeviceIP = "192.168.0.251";// 设备ip地址
		m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
		int iPort = Integer.parseInt("8000");
		lUserID = hCNetSDK.NET_DVR_Login_V30(m_sDeviceIP, (short) iPort, "admin", new String("admin123456"),m_strDeviceInfo);

		long userID = lUserID.longValue();
		if (userID < 0) {
			m_sDeviceIP = "";// 登录未成功,IP置为空
			System.out.println("注册失败");
		} else {
//			System.out.println("注册成功");
			//查询用户数
//			searchDevice();
			getDeviceConfig();
			// 注销用户
			if (hCNetSDK.NET_DVR_Logout(lUserID)) {
//				System.out.println("注销用户");
			}
		}

		// 注册成功or失败均释放SDK资源
		if (hCNetSDK.NET_DVR_Cleanup()) {
//			System.out.println("释放SDK资源");
		}
	}
	
	/**
	 * 获取客流量统计规则配置
	 */
	public void getDeviceConfig() {
		//客流量统计规则配置条件结构体。
	    HCNetSDK.NET_DVR_PDC_RULE_COND struPDCRuleCond = new HCNetSDK.NET_DVR_PDC_RULE_COND ();
        struPDCRuleCond.dwSize = struPDCRuleCond.size();
        struPDCRuleCond.dwChannel = 1;
        struPDCRuleCond.dwID = 1;
        
        //客流量规则配置结构体。
        HCNetSDK.NET_DVR_PDC_RULE_CFG_V42 struPDCRuleCfg = new HCNetSDK.NET_DVR_PDC_RULE_CFG_V42 ();
 
        IntByReference pInt = new IntByReference(0);
        Pointer lpStatusList = pInt.getPointer();

     struPDCRuleCond.write();

     struPDCRuleCfg.write();

     Pointer lpCond = struPDCRuleCond.getPointer();

     Pointer lpInbuferCfg = struPDCRuleCfg.getPointer();
	        
		boolean flag =hCNetSDK.NET_DVR_GetDeviceConfig(lUserID, HCNetSDK.NET_DVR_GET_PDC_RULECFG_V42, 1, lpCond, struPDCRuleCond.size(), lpStatusList, lpInbuferCfg, struPDCRuleCfg.size());
		if (flag)
		{
		    struPDCRuleCfg.read();
			System.out.println("计数触发方式：0- 无，1- 报警输入触发，2- 视频分析触发 :"+struPDCRuleCfg.byCountingType);
			System.out.println("客流量检测数据上传周期（0-15、1-1、2-5、3-10、4-20、5-30、6-60）单位：分钟: "+struPDCRuleCfg.byDataUploadCycle );
			System.out.println("每秒上传机制使能（0-关闭，1-开启）:"+struPDCRuleCfg.bySECUploadEnable);
//			if(mstruTraverseCfg.bySECUploadEnable ==1) {
//				System.out.println("每秒上传机制使能:"+mstruTraverseCfg.bySECUploadEnable);
//				mstruTraverseCfg.bySECUploadEnable = 0;
//			}	
			System.out.println("客流量检测数据上传周期:"+struPDCRuleCfg.byDataUploadCycle);
		    struPDCRuleCfg.byEnable = (byte)1;
			if(struPDCRuleCfg.byDataUploadCycle!=3) {
				struPDCRuleCfg.byDataUploadCycle = (byte)3;
			
				Pointer lpInParamBuffer = struPDCRuleCfg.getPointer();
				boolean flag1  = hCNetSDK.NET_DVR_SetDeviceConfig(lUserID, HCNetSDK.NET_DVR_SET_PDC_RULECFG_V42, 1, lpCond, struPDCRuleCond.size(), lpStatusList, lpInParamBuffer, struPDCRuleCfg.size());
				System.out.println(flag1);
			}		
	     }else {
	    	   int iErr = hCNetSDK.NET_DVR_GetLastError();
	    	   System.out.println("-------------获取客流量统计规则配置失败-----------");
	           return;
	     }
	}

	
	/**
	 * 客流量历史记录查询
	 */
	public void searchDevice() {
		System.out.println("======================================================================");
		HCNetSDK.NET_DVR_PDC_QUERY_COND m_struPdcResultCond = new HCNetSDK.NET_DVR_PDC_QUERY_COND();
		m_struPdcResultCond.dwSize = m_struPdcResultCond.size();
		m_struPdcResultCond.dwChannel = 1;
		m_struPdcResultCond.struStartTime.wYear = 2019;
		m_struPdcResultCond.struStartTime.byMonth = 1;
		m_struPdcResultCond.struStartTime.byDay = 4;
		m_struPdcResultCond.struStartTime.byHour = 0;
		m_struPdcResultCond.struStartTime.byMinute = 0;
		m_struPdcResultCond.struStartTime.bySecond = 0;

		m_struPdcResultCond.struEndTime.wYear = 2019;
		m_struPdcResultCond.struEndTime.byMonth = 1;
		m_struPdcResultCond.struEndTime.byDay = 4;
		m_struPdcResultCond.struEndTime.byHour = 12;
		m_struPdcResultCond.struEndTime.byMinute = 0;
		m_struPdcResultCond.struEndTime.bySecond = 0;
		m_struPdcResultCond.byReportType = 1;
		m_struPdcResultCond.byEnableProgramStatistics = 0;
		m_struPdcResultCond.dwPlayScheduleNo = 0;
		m_struPdcResultCond.byTriggerPeopleCountingData = 0;

		Pointer lpInBuffer = m_struPdcResultCond.getPointer();

		Pointer pUserData = null;
		m_struPdcResultCond.write();
		NativeLong lHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_GET_PDC_RESULT,lpInBuffer, m_struPdcResultCond.size(), null, pUserData);
		if (lHandle.intValue() < 0) {
			System.out.println("建立长连接失败，错误号：" + hCNetSDK.NET_DVR_GetLastError());
			return;
		}
		HCNetSDK.NET_DVR_PDC_RESULT m_struPdcResult = new HCNetSDK.NET_DVR_PDC_RESULT();
		m_struPdcResult.write();
		Pointer pPdcResult = m_struPdcResult.getPointer();

		int iRetSult = 0;
		int allIn =0;
		int allOut = 0;
		int allOver = 0;
		while (true) {
			iRetSult = hCNetSDK.NET_DVR_GetNextRemoteConfig(lHandle, pPdcResult, m_struPdcResult.size());

			if (iRetSult == 1000) {
//				System.out.println("hCNetSDK.NET_DVR_GetLastError():" + hCNetSDK.NET_DVR_GetLastError());
//				System.out.println("NET_DVR_GetNextRemoteConfig 状态  ：" + iRetSult);
				m_struPdcResult.read();
//				if(m_struPdcResult.dwEnterNum>0 || m_struPdcResult.dwLeaveNum>0) {
					allIn += m_struPdcResult.dwEnterNum;
					allOut += m_struPdcResult.dwLeaveNum;
					allOver += m_struPdcResult.dwPeoplePassing;
					System.out.println("开始时间:"+m_struPdcResult.struStartTime+",结束时间:"+m_struPdcResult.struEndTime.toString() + " 进入: " +m_struPdcResult.dwEnterNum + " ,离开: " + m_struPdcResult.dwLeaveNum+",经过:"+m_struPdcResult.dwPeoplePassing); 
//				}
			} else if (iRetSult == 1001) {
//				System.out.println("hCNetSDK.NET_DVR_GetLastError():" + hCNetSDK.NET_DVR_GetLastError());
//				System.out.println("NET_DVR_GetNextRemoteConfig 状态  ：" + iRetSult);
			} else if (iRetSult == 1002 || iRetSult == 1003) {
//				System.out.println("hCNetSDK.NET_DVR_GetLastError():" + hCNetSDK.NET_DVR_GetLastError());
//				System.out.println("NET_DVR_GetNextRemoteConfig 状态  ：" + iRetSult);

				hCNetSDK.NET_DVR_StopRemoteConfig(lHandle);
				break;
			}
		}

		System.out.println("=============================总进入数:"+allIn+",总离开数"+allOut+",总经过数:"+allOver+"=========================================");

	}


}
