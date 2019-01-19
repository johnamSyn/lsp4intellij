package com.github.lsp4intellij.preloading;

import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Launcher Terminator Implementation for Unix.
 */
public class TerminatorUnix implements Terminator {
    private final String processIdentifier = "org.ballerinalang.langserver.launchers.stdio.Main";
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminatorUnix.class);

    /**
     * Get find process command.
     *
     * @param script absolute path of ballerina file running
     * @return find process command
     */
    private String[] getFindProcessCommand(String script) {

        String[] cmd = {
                "/bin/sh", "-c", "ps ax | grep " + script + " |  grep -v 'grep' | awk '{print $1}'"
        };
        return cmd;
    }

    /**
     * Terminate running ballerina program.
     */
    public void terminate() {
        int processID;
        String[] findProcessCommand = getFindProcessCommand(processIdentifier);
        BufferedReader reader = null;
        try {
            Process findProcess = Runtime.getRuntime().exec(findProcessCommand);
            findProcess.waitFor();
            reader = new BufferedReader(new InputStreamReader(findProcess.getInputStream(), Charset.defaultCharset()));

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    processID = Integer.parseInt(line);
                    killChildProcesses(processID);
                    kill(processID);
                } catch (Throwable e) {
                    LOGGER.error("Launcher was unable to kill process " + line + ".");
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Launcher was unable to find the process ID for " + processIdentifier + ".");
        } finally {
            if (reader != null) {
                IOUtils.closeQuietly(reader);
            }
        }
    }

    /**
     * Terminate running ballerina program.
     *
     * @param pid - process id
     */
    public void kill(int pid) {
        //todo need to put aditional validation
        if (pid < 0) {
            return;
        }
        String killCommand = String.format("kill -9 %d", pid);
        try {
            Process kill = Runtime.getRuntime().exec(killCommand);
            kill.waitFor();
        } catch (Throwable e) {
            LOGGER.error("Launcher was unable to terminate process:" + pid + ".");
        }
    }

    /**
     * Terminate running all child processes for a given pid.
     *
     * @param pid - process id
     */
    void killChildProcesses(int pid) {
        BufferedReader reader = null;
        try {
            Process findChildProcess = Runtime.getRuntime().exec(String.format("pgrep -P %d", pid));
            findChildProcess.waitFor();
            reader = new BufferedReader(
                    new InputStreamReader(findChildProcess.getInputStream(), Charset.defaultCharset()));
            String line;
            int childProcessID;
            while ((line = reader.readLine()) != null) {
                childProcessID = Integer.parseInt(line);
                kill(childProcessID);
            }
        } catch (Throwable e) {
            LOGGER.error("Launcher was unable to find parent for process:" + pid + ".");
        } finally {
            if (reader != null) {
                IOUtils.closeQuietly(reader);
            }
        }
    }
}
