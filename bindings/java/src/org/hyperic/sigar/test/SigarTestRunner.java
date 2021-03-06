/*
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of SIGAR.
 * 
 * SIGAR is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.sigar.test;

import java.util.ArrayList;
import java.util.Collection;

import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarLoader;

import org.hyperic.sigar.cmd.SigarCommandBase;
import org.hyperic.sigar.cmd.Shell;

import org.hyperic.sigar.win32.test.TestEventLog;
import org.hyperic.sigar.win32.test.TestLocaleInfo;
import org.hyperic.sigar.win32.test.TestFileVersion;
import org.hyperic.sigar.win32.test.TestMetaBase;
import org.hyperic.sigar.win32.test.TestPdh;
import org.hyperic.sigar.win32.test.TestRegistryKey;
import org.hyperic.sigar.win32.test.TestService;

public class SigarTestRunner extends SigarCommandBase {

    private Collection completions;

    private static final Class[] TESTS;
    
    private static final Class[] ALL_TESTS = {
        TestLog.class,
        TestInvoker.class,
        TestMx.class,
        TestPTQL.class,
        TestCpu.class,
        TestCpuInfo.class,
        TestFileInfo.class,
        TestFileSystem.class,
        TestFQDN.class,
        TestLoadAverage.class,
        TestMem.class,
        TestNetIf.class,
        TestNetInfo.class,
        TestNetRoute.class,
        TestNetStat.class,
        TestNetStatPort.class,
        TestTcpStat.class,
        TestNfsClientV2.class,
        TestNfsServerV2.class,
        TestNfsClientV3.class,
        TestNfsServerV3.class,
        TestProcArgs.class,
        TestProcEnv.class,
        TestProcExe.class,
        TestProcModules.class,
        TestProcFd.class,
        TestProcList.class,
        TestProcMem.class,
        TestProcState.class,
        TestProcStat.class,
        TestProcTime.class,
        TestResourceLimit.class,
        TestSignal.class,
        TestSwap.class,
        TestThreadCpu.class,
        TestUptime.class,
        TestVMware.class,
        TestWho.class,
    };

    private static final Class[] WIN32_TESTS = {
        TestEventLog.class,
        TestLocaleInfo.class,
        TestPdh.class,
        TestMetaBase.class,
        TestRegistryKey.class,
        TestService.class,
        TestFileVersion.class
    };
    
    static {
        if (SigarLoader.IS_WIN32) {
            TESTS = new Class[ALL_TESTS.length + WIN32_TESTS.length];
            System.arraycopy(ALL_TESTS, 0, TESTS,
                             0, ALL_TESTS.length);
            System.arraycopy(WIN32_TESTS, 0, TESTS,
                             ALL_TESTS.length, WIN32_TESTS.length);
        }
        else {
            TESTS = ALL_TESTS;
        }
    }

    public SigarTestRunner(Shell shell) {
        super(shell);

        this.completions = new ArrayList();
        for (int i=0; i<TESTS.length; i++) {
            String name = TESTS[i].getName();
            int ix = name.lastIndexOf(".Test");
            this.completions.add(name.substring(ix + 5));
        }
    }

    public SigarTestRunner() {
        super();
    }

    protected boolean validateArgs(String[] args) {
        return true;
    }

    public String getSyntaxArgs() {
        return "[testclass]";
    }

    public String getUsageShort() {
        return "Run sigar tests";
    }

    public Collection getCompletions() {
        return this.completions;
    }

    public void output(String[] args) throws SigarException {
        SigarTestPrinter.runTests(TESTS, args);
    }

    public static void main(String[] args) throws Exception {
        new SigarTestRunner().processCommand(args);
    }
}
