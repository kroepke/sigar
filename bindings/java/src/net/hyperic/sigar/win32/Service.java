package net.hyperic.sigar.win32;

public class Service extends Win32Bindings implements java.io.Serializable
{
    // Service State
    public static final int SERVICE_STOPPED          = 0x00000001;
    public static final int SERVICE_START_PENDING    = 0x00000002;
    public static final int SERVICE_STOP_PENDING     = 0x00000003;
    public static final int SERVICE_RUNNING          = 0x00000004;
    public static final int SERVICE_CONTINUE_PENDING = 0x00000005;
    public static final int SERVICE_PAUSE_PENDING    = 0x00000006;
    public static final int SERVICE_PAUSED           = 0x00000007;

    // Start Type
    public static final int SERVICE_BOOT_START   = 0x00000000;
    public static final int SERVICE_SYSTEM_START = 0x00000001;
    public static final int SERVICE_AUTO_START   = 0x00000002;
    public static final int SERVICE_DEMAND_START = 0x00000003;
    public static final int SERVICE_DISABLED     = 0x00000004;

    // Service Controls
    protected static final int SERVICE_CONTROL_STOP             = 0x00000001;
    protected static final int SERVICE_CONTROL_PAUSE            = 0x00000002;
    protected static final int SERVICE_CONTROL_CONTINUE         = 0x00000003;
    protected static final int SERVICE_CONTROL_INTERROGATE      = 0x00000004;
    protected static final int SERVICE_CONTROL_SHUTDOWN         = 0x00000005;
    protected static final int SERVICE_CONTROL_PARAMCHANGE      = 0x00000006;
    protected static final int SERVICE_CONTROL_NETBINDADD       = 0x00000007;
    protected static final int SERVICE_CONTROL_NETBINDREMOVE    = 0x00000008;
    protected static final int SERVICE_CONTROL_NETBINDENABLE    = 0x00000009;
    protected static final int SERVICE_CONTROL_NETBINDDISABLE   = 0x0000000A;
    protected static final int SERVICE_CONTROL_DEVICEEVENT      = 0x0000000B;
    protected static final int SERVICE_CONTROL_HARDWAREPROFILECHANGE 
        = 0x0000000C;
    protected static final int SERVICE_CONTROL_POWEREVENT       = 0x0000000D;
    protected static final int SERVICE_CONTROL_SESSIONCHANGE    = 0x0000000E;

    // Service Control Manager object specific access types
    protected static final int STANDARD_RIGHTS_REQUIRED = (int)0x000F0000L;

    protected static final int SC_MANAGER_CONNECT            = 0x0001;
    protected static final int SC_MANAGER_CREATE_SERVICE     = 0x0002;
    protected static final int SC_MANAGER_ENUMERATE_SERVICE  = 0x0004;
    protected static final int SC_MANAGER_LOCK               = 0x0008;
    protected static final int SC_MANAGER_QUERY_LOCK_STATUS  = 0x0010;
    protected static final int SC_MANAGER_MODIFY_BOOT_CONFIG = 0x0020;

    protected static final int SC_MANAGER_ALL_ACCESS = 
        (STANDARD_RIGHTS_REQUIRED |
         SC_MANAGER_CONNECT             |
         SC_MANAGER_CREATE_SERVICE      |
         SC_MANAGER_ENUMERATE_SERVICE   |
         SC_MANAGER_LOCK                |
         SC_MANAGER_QUERY_LOCK_STATUS   |
         SC_MANAGER_MODIFY_BOOT_CONFIG);

    // Service object specific access type
    protected static final int SERVICE_QUERY_CONFIG         = 0x0001;
    protected static final int SERVICE_CHANGE_CONFIG        = 0x0002;
    protected static final int SERVICE_QUERY_STATUS         = 0x0004;
    protected static final int SERVICE_ENUMERATE_DEPENDENTS = 0x0008;
    protected static final int SERVICE_START                = 0x0010;
    protected static final int SERVICE_STOP                 = 0x0020;
    protected static final int SERVICE_PAUSE_CONTINUE       = 0x0040;
    protected static final int SERVICE_INTERROGATE          = 0x0080;
    protected static final int SERVICE_USER_DEFINED_CONTROL = 0x0100;

    protected static final int SERVICE_ALL_ACCESS =
        (STANDARD_RIGHTS_REQUIRED |
         SERVICE_QUERY_CONFIG           |
         SERVICE_CHANGE_CONFIG          |
         SERVICE_QUERY_STATUS           |
         SERVICE_ENUMERATE_DEPENDENTS   |
         SERVICE_START                  |
         SERVICE_STOP                   |
         SERVICE_PAUSE_CONTINUE         |
         SERVICE_INTERROGATE            |
         SERVICE_USER_DEFINED_CONTROL);
    
    // Service Types (Bit Mask)
    protected static final int SERVICE_KERNEL_DRIVER       = 0x00000001;
    protected static final int SERVICE_FILE_SYSTEM_DRIVER  = 0x00000002;
    protected static final int SERVICE_ADAPTER             = 0x00000004;
    protected static final int SERVICE_RECOGNIZER_DRIVER   = 0x00000008;
    protected static final int SERVICE_WIN32_OWN_PROCESS   = 0x00000010;
    protected static final int SERVICE_WIN32_SHARE_PROCESS = 0x00000020;
    protected static final int SERVICE_INTERACTIVE_PROCESS = 0x00000100;

    // Error control type
    protected static final int SERVICE_ERROR_IGNORE   = 0x00000000;
    protected static final int SERVICE_ERROR_NORMAL   = 0x00000001;
    protected static final int SERVICE_ERROR_SEVERE   = 0x00000002;
    protected static final int SERVICE_ERROR_CRITICAL = 0x00000003;

    ///////////////////////////////////////////////////////
    // Object Variables
    protected long  m_hMgr;
    protected long  m_hService;

    protected Service() throws Win32Exception
    {
        this.m_hMgr = OpenSCManager("", SC_MANAGER_ALL_ACCESS);
        
        if(this.m_hMgr == 0)
            Service.throwLastErrorException();
    }
    
    public Service(String serviceName) throws Win32Exception
    {
        this();
        
        this.m_hService = OpenService(this.m_hMgr, serviceName, 
                                      SERVICE_ALL_ACCESS);

        if(this.m_hService == 0)
            Service.throwLastErrorException();
    }

    public void finalize()
    {
        this.close();
    }

    private void close()
    {
        if(this.m_hService != 0) {
            CloseServiceHandle(this.m_hService);
            this.m_hService = 0;
        }

        if(this.m_hMgr != 0) {
            CloseServiceHandle(this.m_hMgr);
            this.m_hService = 0;
        }
    }
    
    public static Service create(String serviceName, 
                                 String displayName, 
                                 String description, 
                                 String path) 
        throws Win32Exception
    {
        return Service.create(serviceName, displayName, 
                              description, SERVICE_WIN32_OWN_PROCESS,
                              SERVICE_AUTO_START, SERVICE_ERROR_NORMAL,
                              path, null, null, "");
    }
    
    public static Service create(String serviceName, String displayName,
                                 String description, int serviceType,
                                 int startType, int errorControl, 
                                 String path, String[] dependicies,
                                 String startName, String password)
        throws Win32Exception
    {
        if(serviceName == null)
            throw new IllegalArgumentException("The serviceName argument " +
                                               "cannot be null.");
        if(displayName == null)
            throw new IllegalArgumentException("The displayName argument " +
                                               "cannot be null.");
        if(path == null)
            throw new IllegalArgumentException("The displayName argument " +
                                               "cannot be null.");
                
        Service service = new Service();
        service.m_hService = Service.CreateService(service.m_hMgr,
                                                   serviceName,
                                                   displayName,
                                                   serviceType,
                                                   startType,
                                                   errorControl,
                                                   path,
                                                   dependicies,
                                                   startName,
                                                   password);
        if(service.m_hService == 0)
            Service.throwLastErrorException();

        service.setDescription(description);
        
        return service;
    }

    public void delete() throws Win32Exception
    {
        DeleteService(this.m_hService);
    }

    public void setDescription(String description)
    {
        Service.ChangeServiceDescription(this.m_hService, description);
    }
            
    public void start() throws Win32Exception
    {
        if(Service.StartService(this.m_hService) == false)
            Service.throwLastErrorException(); 
    }

    public void startAndWait() throws Win32Exception
    {
        // Wait indefinitely
        this.startAndWait(0);
    }

    public void startAndWait(long timeout) throws Win32Exception
    {
        this.start();
        this.waitForStart(timeout);
    }

    public int status()
    {
        return QueryServiceStatus(this.m_hService);
    }

    public int startType()
    {
        return QueryServiceStartType(this.m_hService);
    }

    public void stop() throws Win32Exception
    {
        if(StopService(this.m_hService) == false)
            Service.throwLastErrorException(); 
    }

    public void stopAndWait() throws Win32Exception
    {
        // Wait indefinitely
        this.stopAndWait(0);
    }

    public void stopAndWait(long timeout) throws Win32Exception
    {
        long lStatus;
        
        this.stop();

        long lStart = System.currentTimeMillis();

        while((lStatus = this.status()) != SERVICE_STOPPED) {
            if(lStatus == SERVICE_STOP_PENDING) {
                // The start hasn't completed yet. Keep trying up to 
                // the timeout.
                if((System.currentTimeMillis() - lStart) < 
                   timeout || timeout <= 0) {
                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException e) {
                    }
                }
            }
            else
                break;
        }

        if(lStatus != SERVICE_STOPPED)
            Service.throwLastErrorException();
    }
    
    public void waitForStart(long timeout) throws Win32Exception
    {
        long    lStatus;
        boolean bResult = true;
        long    lStart  = System.currentTimeMillis();
            
        while((lStatus = this.status()) != SERVICE_RUNNING)
        {
            if(lStatus == SERVICE_START_PENDING)
            {
                // The start hasn't completed yet. Keep trying up to 
                // the timeout.
                if((System.currentTimeMillis() - lStart) < 
                   timeout || timeout <= 0) {
                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException e) {
                    }
                }
                else
                    bResult = false;
            } else if(lStatus == SERVICE_STOPPED) {
                // Start failed
                bResult = false;
                break;
            } else {
                // Hrm.
                bResult = false;
                break;
            }
        }

        if(bResult == false)
            Service.throwLastErrorException();
    }

    protected static final void throwLastErrorException() 
        throws Win32Exception
    {
        int iErr = Service.GetLastError();
        throw new Win32Exception(iErr, "Win32 Error Code: " + 
                                 iErr + ": " + Service.GetErrorMessage(iErr));
    }
    
    protected static final native boolean 
        ChangeServiceDescription(long handle,
                                 String description);
    protected static final native boolean CloseServiceHandle(long handle);
    protected static final native long    CreateService(long handle,
                                                        String serviceName,
                                                        String displayName,
                                                        int serviceType,
                                                        int startType,
                                                        int errorControl,
                                                        String path,
                                                        String[] dependicies,
                                                        String startName,
                                                        String password);
    protected static final native boolean ControlService(long handle,
                                                         int control);
    protected static final native boolean DeleteService(long handle);
    protected static final native String  GetErrorMessage(int error);
    protected static final native int     GetLastError();
    protected static final native long    OpenSCManager(String machine,
                                                        int access);
    protected static final native long    OpenService(long handle,
                                                      String service,
                                                      int access);
    protected static final native int     QueryServiceStatus(long handle);
    protected static final native int     QueryServiceStartType(long handle);
    protected static final native boolean StartService(long handle);
    protected static final native boolean StopService(long handle);
}