package ClientDemo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import ClientDemo.HCNetSDK.FMSGCallBack_V31;


public class Realtime {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
	public static PlayCtrl playControl = PlayCtrl.INSTANCE;

	public static HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo;// 设备信息

	public static String m_sDeviceIP;// 已登录设备的IP地址
	public static NativeLong lUserID;// 用户句柄
	public static NativeLong lAlarmHandle;// 报警布防句柄
	
	FMSGCallBack_V31 fMSFCallBack_V31;//报警回调函数实现
    
    
	/*************************************************
	 * 函数: 主函数 函数描述:新建ClientDemo窗体并调用接口初始化SDK
	 *************************************************/
	public static void main(String args[]) {
		new Realtime().doMain();
	}
	
	public void doMain() {
		// 初始化
		boolean initSuc = hCNetSDK.NET_DVR_Init();
		if (initSuc != true) {
			System.out.println("初始化失败");
		}
		hCNetSDK.NET_DVR_SetLogToFile(true, "C:\\SdkLog2018\\", false);
		// 设置连接时间与重连时间
		hCNetSDK.NET_DVR_SetConnectTime(2000, 1);
		hCNetSDK.NET_DVR_SetReconnect(10000, true);

		login();
		lAlarmHandle = new NativeLong(-1);
		long userID = lUserID.longValue();
		if (userID < 0) {
			m_sDeviceIP = "";// 登录未成功,IP置为空
			System.out.println("注册失败");
		} else {
			SetupAlarmChan();
		}
	}
    
	public void login() {
		// 注册
		m_sDeviceIP = "192.168.0.251";// 设备ip地址
		m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
		int iPort = Integer.parseInt("8000");
		lUserID = hCNetSDK.NET_DVR_Login_V30(m_sDeviceIP, (short) iPort, "admin", new String("admin123456"),m_strDeviceInfo);
	}
	
	
	public void SetupAlarmChan() {
		if (lUserID.intValue() == -1)
        {
            System.out.println("请先注册");
            return;
        }
         if (lAlarmHandle.intValue() < 0)//尚未布防,需要布防
         {
                if (fMSFCallBack_V31 == null)
                {
                    fMSFCallBack_V31 = new FMSGCallBack_V31();
                    Pointer pUser = null;
                    if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser))
                    {
                        System.out.println("设置回调函数失败!");
                    }
                }
                HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
                m_strAlarmInfo.dwSize=m_strAlarmInfo.size();
                m_strAlarmInfo.byLevel=1;
                m_strAlarmInfo.byAlarmInfoType=1;
                m_strAlarmInfo.byDeployType =1;
                m_strAlarmInfo.write();
                lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
                if (lAlarmHandle.intValue() == -1)
                {
                    System.out.println("布防失败");
                }
                else
                {
                    System.out.println("布防成功");
                    while(true) {
                    	try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
                    }
                }
          }
	}

	 public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31
	    {
	        //报警信息回调函数
	        public boolean invoke(NativeLong lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser)
	        {
	            AlarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
	            return true;
	        }
	}

	/**
	 * 回调的具体实现方法
	 * @param lCommand
	 * @param pAlarmer
	 * @param pAlarmInfo
	 * @param dwBufLen
	 * @param pUser
	 */
	public void AlarmDataHandle(NativeLong lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo,
			int dwBufLen, Pointer pUser) {
		String sAlarmType = new String();

		sAlarmType = new String("lCommand=") + lCommand.intValue();
		// lCommand是传的报警类型
		switch (lCommand.intValue()) {
			 
			case HCNetSDK.COMM_ALARM_PDC://客流量统计报警上传
				HCNetSDK.NET_DVR_PDC_ALRAM_INFO strPDCResult = new HCNetSDK.NET_DVR_PDC_ALRAM_INFO();
				strPDCResult.write();
				Pointer pPDCInfo = strPDCResult.getPointer();
				pPDCInfo.write(0, pAlarmInfo.getByteArray(0, strPDCResult.size()), 0, strPDCResult.size());
				strPDCResult.read();
	
				if (strPDCResult.byMode == 0) { //0-实时统计结果，自上次清零动作（包括设备重启、手动清零或每天零点自动清零）后开始计算的实时数量
					strPDCResult.uStatModeParam.setType(HCNetSDK.NET_DVR_STATFRAME.class);
					sAlarmType = "byMode实时统计结果:"+sAlarmType + "：客流量统计，进入人数：" + strPDCResult.dwEnterNum + "，离开人数："
							+ strPDCResult.dwLeaveNum + ", byMode:" + strPDCResult.byMode + ", dwRelativeTime:"
							+ strPDCResult.uStatModeParam.struStatFrame.dwRelativeTime + ", dwAbsTime:"
							+ strPDCResult.uStatModeParam.struStatFrame.dwAbsTime;
					System.out.println(sAlarmType);
					break;
				}
				if (strPDCResult.byMode == 1) {//1-周期统计结果，设定的统计周期内增加的数量，更新频率即为设定的统计周期（默认15分钟，最小1分钟，最大60分钟）
					strPDCResult.uStatModeParam.setType(HCNetSDK.NET_DVR_STATTIME.class);
					String strtmStart = ""
							+ String.format("%04d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwYear)
							+"-"+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwMonth)
							+"-"+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwDay)
							+" "+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwHour)
							+":"+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwMinute)
							+":"+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwSecond);
					
					String strtmEnd = "" + String.format("%04d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwYear)
							+"-"+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwMonth)
							+"-"+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwDay)
							+" "+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwHour)
							+":"+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwMinute)
							+":"+ String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwSecond);
					
//					String strtmStart = strPDCResult.uStatModeParam.struStatTime.tmStart.toStringTime();
//					String strtmEnd =strPDCResult.uStatModeParam.struStatTime.tmEnd.toStringTime();
							
					sAlarmType = "周期统计结果:"+sAlarmType + "：客流量统计，进入人数：" + strPDCResult.dwEnterNum + "，离开人数："
							+ strPDCResult.dwLeaveNum + ", byMode:" + strPDCResult.byMode + ", tmStart:" + strtmStart.toString()
							+ ",tmEnd :" + strtmEnd;
					System.out.println(sAlarmType);
				}
	
			default:
				break;
		}
	}
	
	 /******************************************************************************
     *内部类:   FMSGCallBack
     *报警信息回调函数
     ******************************************************************************/
    public class FMSGCallBack implements HCNetSDK.FMSGCallBack
    {
        //报警信息回调函数

        public void invoke(NativeLong lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, HCNetSDK.RECV_ALARM pAlarmInfo, int dwBufLen, Pointer pUser)
        {
        	System.out.println("11111111111111111111111");
            String sAlarmType = new String();

            String[] newRow = new String[3];
            //报警时间
            Date today = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String[] sIP = new String[2];

            //lCommand是传的报警类型
            switch (lCommand.intValue())
            {
                //9000报警
                case HCNetSDK.COMM_ALARM_V30:
                   HCNetSDK.NET_DVR_ALARMINFO_V30 strAlarmInfoV30 = new HCNetSDK.NET_DVR_ALARMINFO_V30();
                   strAlarmInfoV30.write();
                   Pointer pInfoV30 = strAlarmInfoV30.getPointer();
                   pInfoV30.write(0, pAlarmInfo.RecvBuffer, 0, strAlarmInfoV30.size());
                   strAlarmInfoV30.read();

                    switch (strAlarmInfoV30.dwAlarmType)
                    {
                        case 0:
                            sAlarmType = new String("信号量报警");
                            break;
                        case 1:
                            sAlarmType = new String("硬盘满");
                            break;
                        case 2:
                            sAlarmType = new String("信号丢失");
                            break;
                        case 3:
                            sAlarmType = new String("移动侦测");
                            break;
                        case 4:
                            sAlarmType = new String("硬盘未格式化");
                            break;
                        case 5:
                            sAlarmType = new String("读写硬盘出错");
                            break;
                        case 6:
                            sAlarmType = new String("遮挡报警");
                            break;
                        case 7:
                            sAlarmType = new String("制式不匹配");
                            break;
                        case 8:
                            sAlarmType = new String("非法访问");
                            break;
                    }

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];

                    break;

                //8000报警
                case HCNetSDK.COMM_ALARM:
                    HCNetSDK.NET_DVR_ALARMINFO strAlarmInfo = new HCNetSDK.NET_DVR_ALARMINFO();
                    strAlarmInfo.write();
                    Pointer pInfo = strAlarmInfo.getPointer();
                    pInfo.write(0, pAlarmInfo.RecvBuffer, 0, strAlarmInfo.size());
                    strAlarmInfo.read();


                    switch (strAlarmInfo.dwAlarmType)
                    {
                        case 0:
                            sAlarmType = new String("信号量报警");
                            break;
                        case 1:
                            sAlarmType = new String("硬盘满");
                            break;
                        case 2:
                            sAlarmType = new String("信号丢失");
                            break;
                        case 3:
                            sAlarmType = new String("移动侦测");
                            break;
                        case 4:
                            sAlarmType = new String("硬盘未格式化");
                            break;
                        case 5:
                            sAlarmType = new String("读写硬盘出错");
                            break;
                        case 6:
                            sAlarmType = new String("遮挡报警");
                            break;
                        case 7:
                            sAlarmType = new String("制式不匹配");
                            break;
                        case 8:
                            sAlarmType = new String("非法访问");
                            break;
                    }

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];

                    break;

                //ATM DVR transaction information
                case HCNetSDK.COMM_TRADEINFO:
                    //处理交易信息报警
                    break;

                //IPC接入配置改变报警
                case HCNetSDK.COMM_IPCCFG:
                    // 处理IPC报警
                    break;

                default:
                    System.out.println("未知报警类型");
                    break;
            }
        }
    }
    
	
	
	
	
	
	
}
